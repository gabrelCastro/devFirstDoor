package com.devfirstdoor.curriculo;

import com.devfirstdoor.controller.dto.NovaVersaoCurriculoRequest;
import com.devfirstdoor.controller.dto.StatusCurriculoResponse;
import com.devfirstdoor.controller.dto.VersaoCurriculoResponse;
import com.devfirstdoor.controller.dto.VersaoCurriculoResumoResponse;
import com.devfirstdoor.curriculo.Casamento.Evidencia;
import com.devfirstdoor.curriculo.ia.ClienteIa;
import com.devfirstdoor.curriculo.ia.FalhaIaException;
import com.devfirstdoor.curriculo.ia.IaProperties;
import com.devfirstdoor.domain.Candidatura;
import com.devfirstdoor.domain.Usuario;
import com.devfirstdoor.domain.UsoIa;
import com.devfirstdoor.domain.VersaoCurriculo;
import com.devfirstdoor.repository.CandidaturaRepository;
import com.devfirstdoor.repository.UsoIaRepository;
import com.devfirstdoor.repository.UsuarioRepository;
import com.devfirstdoor.repository.VersaoCurriculoRepository;
import com.devfirstdoor.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Orquestra a adaptação: confere cota e perfil, analisa a vaga, cruza com o perfil, chama a IA,
 * valida e guarda a versão. As chamadas à IA ficam fora de transação (levam segundos); só as
 * leituras do começo e a gravação do fim usam o banco.
 */
@Service
public class AdaptacaoService {

    static final int DESCRICAO_MIN = 150;
    static final int DESCRICAO_MAX = 20_000;
    static final int MAX_VERSOES = 100;

    private final PerfilCurriculoService perfilService;
    private final AnaliseVagaService analiseVagaService;
    private final ClienteIa clienteIa;
    private final IaProperties properties;
    private final UsuarioRepository usuarioRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final VersaoCurriculoRepository versaoRepository;
    private final UsoIaRepository usoIaRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transacao;
    private final TransactionTemplate leitura;

    public AdaptacaoService(PerfilCurriculoService perfilService, AnaliseVagaService analiseVagaService,
                            ClienteIa clienteIa, IaProperties properties, UsuarioRepository usuarioRepository,
                            CandidaturaRepository candidaturaRepository, VersaoCurriculoRepository versaoRepository,
                            UsoIaRepository usoIaRepository, ObjectMapper objectMapper,
                            PlatformTransactionManager transactionManager) {
        this.perfilService = perfilService;
        this.analiseVagaService = analiseVagaService;
        this.clienteIa = clienteIa;
        this.properties = properties;
        this.usuarioRepository = usuarioRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.versaoRepository = versaoRepository;
        this.usoIaRepository = usoIaRepository;
        this.objectMapper = objectMapper;
        this.transacao = new TransactionTemplate(transactionManager);
        this.leitura = new TransactionTemplate(transactionManager);
        this.leitura.setReadOnly(true);
    }

    public StatusCurriculoResponse status(String login, LocalDateTime agora) {
        long usadas = leitura.execute(s -> usoIaRepository.contarDesde(usuario(login).getId(), agora.minusHours(24)));
        return new StatusCurriculoResponse(clienteIa.configurado(), properties.getLimiteDiario(), (int) usadas);
    }

    public VersaoCurriculoResponse criar(String login, NovaVersaoCurriculoRequest request, LocalDateTime agora) {
        String descricao = request.descricaoVaga() == null ? "" : request.descricaoVaga().strip();
        if (descricao.length() < DESCRICAO_MIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cole a descrição completa da vaga (pelo menos " + DESCRICAO_MIN + " caracteres).");
        }
        if (descricao.length() > DESCRICAO_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A descrição passa de " + DESCRICAO_MAX + " caracteres. Deixe só a parte da vaga.");
        }
        if (!clienteIa.configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "A geração com IA não está configurada no servidor.");
        }

        // 1. Leituras e conferências, numa transação curta.
        PerfilCurriculo perfil = perfilService.obter(login);
        String tituloCandidatura = leitura.execute(s -> {
            Usuario usuario = usuario(login);
            if (usoIaRepository.contarDesde(usuario.getId(), agora.minusHours(24)) >= properties.getLimiteDiario()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Você usou as "
                        + properties.getLimiteDiario() + " adaptações das últimas 24 horas. Tente de novo mais tarde.");
            }
            if (versaoRepository.countByUsuarioId(usuario.getId()) >= MAX_VERSOES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Limite de " + MAX_VERSOES + " versões atingido. Exclua as antigas para continuar.");
            }
            return request.candidaturaId() == null ? null : candidatura(request.candidaturaId(), usuario).getEmpresa();
        });
        if (perfil.experiencias().isEmpty() && perfil.projetos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Preencha seu perfil com pelo menos uma experiência ou projeto antes de adaptar.");
        }

        // 2. IA, fora de transação.
        VagaAnalisada vaga = analiseVagaService.analisar(descricao);
        Map<String, Evidencia> casamento = Casamento.calcular(perfil, vaga);
        String json = clienteIa.gerarJson(PromptAdaptacao.INSTRUCOES,
                PromptAdaptacao.entrada(perfil, vaga, casamento, objectMapper),
                "curriculo_adaptado", PromptAdaptacao.schema(perfil, vaga));
        AdaptacaoIa resposta;
        try {
            resposta = objectMapper.readValue(json, AdaptacaoIa.class);
        } catch (JacksonException e) {
            throw FalhaIaException.indisponivel();
        }
        Escolhas escolhas = Escolhas.aceitarTudo();
        Proposta proposta = comCobertura(perfil, vaga, ValidadorAdaptacao.validar(perfil, vaga, resposta), escolhas);
        String empresa = vaga.empresa() != null ? vaga.empresa() : tituloCandidatura;
        String titulo = vaga.cargo() + (empresa == null ? "" : " · " + empresa);

        // 3. Grava a versão e conta o uso, numa transação.
        return transacao.execute(s -> {
            Usuario usuario = usuario(login);
            Candidatura candidatura = request.candidaturaId() == null ? null : candidatura(request.candidaturaId(), usuario);
            usoIaRepository.save(new UsoIa(usuario, agora));
            VersaoCurriculo versao = versaoRepository.save(new VersaoCurriculo(usuario, candidatura,
                    titulo.length() > 300 ? titulo.substring(0, 300) : titulo, descricao,
                    objectMapper.writeValueAsString(perfil), objectMapper.writeValueAsString(vaga),
                    objectMapper.writeValueAsString(proposta), objectMapper.writeValueAsString(escolhas), agora));
            return resposta(versao);
        });
    }

    public List<VersaoCurriculoResumoResponse> listar(String login) {
        return leitura.execute(s -> versaoRepository.listarDoUsuario(usuario(login).getId()).stream().map(v -> {
            Proposta.Cobertura c = objectMapper.readValue(v.getPropostaJson(), Proposta.class).cobertura();
            return new VersaoCurriculoResumoResponse(v.getId(), v.getTitulo(),
                    v.getCandidatura() == null ? null : v.getCandidatura().getId(), v.getCriadaEm(),
                    c.palavrasChave().size(), c.antes().size(), c.depois().size());
        }).toList());
    }

    public VersaoCurriculoResponse detalhar(String login, long id) {
        return leitura.execute(s -> resposta(versao(login, id)));
    }

    /** Guarda o que a pessoa aceitou e recalcula a cobertura "depois" com essas escolhas. */
    public VersaoCurriculoResponse atualizarEscolhas(String login, long id, Escolhas escolhas, LocalDateTime agora) {
        return transacao.execute(s -> {
            VersaoCurriculo versao = versao(login, id);
            PerfilCurriculo perfil = objectMapper.readValue(versao.getPerfilJson(), PerfilCurriculo.class);
            VagaAnalisada vaga = objectMapper.readValue(versao.getVagaJson(), VagaAnalisada.class);
            Proposta proposta = objectMapper.readValue(versao.getPropostaJson(), Proposta.class);
            Escolhas limpas = new Escolhas(escolhas.usarResumo(), escolhas.recusados().stream().distinct().limit(200).toList());
            Proposta atualizada = comCobertura(perfil, vaga, proposta, limpas);
            versao.atualizar(objectMapper.writeValueAsString(atualizada), objectMapper.writeValueAsString(limpas), agora);
            return resposta(versao);
        });
    }

    public void excluir(String login, long id) {
        transacao.executeWithoutResult(s -> versaoRepository.delete(versao(login, id)));
    }

    /** Currículo final de uma versão: contato atual da pessoa + fatos da cópia do perfil + escolhas. */
    public CurriculoFinal curriculoFinal(String login, long id) {
        PerfilCurriculo atual = perfilService.obter(login);
        return leitura.execute(s -> {
            VersaoCurriculo versao = versao(login, id);
            PerfilCurriculo copia = objectMapper.readValue(versao.getPerfilJson(), PerfilCurriculo.class);
            return CurriculoFinal.montar(copia, atual.contato(),
                    objectMapper.readValue(versao.getPropostaJson(), Proposta.class),
                    objectMapper.readValue(versao.getEscolhasJson(), Escolhas.class));
        });
    }

    public String tituloVersao(String login, long id) {
        return leitura.execute(s -> versao(login, id).getTitulo());
    }

    private Proposta comCobertura(PerfilCurriculo perfil, VagaAnalisada vaga, Proposta proposta, Escolhas escolhas) {
        String antes = CurriculoFinal.doPerfil(perfil).comoTexto();
        String depois = CurriculoFinal.montar(perfil, perfil.contato(), proposta, escolhas).comoTexto();
        return proposta.comCobertura(new Proposta.Cobertura(vaga.palavrasChaveAts(),
                Casamento.palavrasPresentes(antes, vaga.palavrasChaveAts()),
                Casamento.palavrasPresentes(depois, vaga.palavrasChaveAts())));
    }

    private VersaoCurriculoResponse resposta(VersaoCurriculo v) {
        return new VersaoCurriculoResponse(v.getId(), v.getTitulo(),
                v.getCandidatura() == null ? null : v.getCandidatura().getId(), v.getCriadaEm(), v.getAtualizadaEm(),
                objectMapper.readValue(v.getVagaJson(), VagaAnalisada.class),
                objectMapper.readValue(v.getPropostaJson(), Proposta.class),
                objectMapper.readValue(v.getEscolhasJson(), Escolhas.class));
    }

    private VersaoCurriculo versao(String login, long id) {
        return versaoRepository.buscarDoUsuario(id, usuario(login).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão não encontrada"));
    }

    private Candidatura candidatura(long id, Usuario usuario) {
        return candidaturaRepository.buscarDoUsuario(id, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidatura não encontrada"));
    }

    private Usuario usuario(String login) {
        return usuarioRepository.findByLogin(UsuarioService.normalizarLogin(login))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida"));
    }
}
