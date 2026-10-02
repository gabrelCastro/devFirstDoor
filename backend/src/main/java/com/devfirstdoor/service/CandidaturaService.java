package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.AtualizacaoCandidaturaRequest;
import com.devfirstdoor.controller.dto.CandidaturaDetalheResponse;
import com.devfirstdoor.controller.dto.CandidaturaResponse;
import com.devfirstdoor.controller.dto.EventoCandidaturaResponse;
import com.devfirstdoor.controller.dto.NovaCandidaturaRequest;
import com.devfirstdoor.controller.dto.ResumoCandidaturasResponse;
import com.devfirstdoor.domain.Candidatura;
import com.devfirstdoor.domain.EtapaCandidatura;
import com.devfirstdoor.domain.EventoCandidatura;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.TipoEventoCandidatura;
import com.devfirstdoor.domain.Usuario;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.CandidaturaRepository;
import com.devfirstdoor.repository.EventoCandidaturaRepository;
import com.devfirstdoor.repository.UsuarioRepository;
import com.devfirstdoor.repository.VagaRepository;
import com.devfirstdoor.util.LinkSeguro;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Candidaturas de um usuário. Toda operação recebe o login autenticado e só enxerga as
 * candidaturas dele: a de outra conta responde 404, como se não existisse.
 */
@Service
public class CandidaturaService {

    static final int LIMITE_POR_USUARIO = 500;
    static final int LIMITE_EVENTOS = 300;
    /** Candidatado há tanto tempo sem mudança de etapa: hora de mandar um follow-up. */
    static final int DIAS_PARA_FOLLOW_UP = 14;
    static final int SEMANAS_NO_RESUMO = 12;

    private final CandidaturaRepository candidaturaRepository;
    private final EventoCandidaturaRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VagaRepository vagaRepository;

    public CandidaturaService(CandidaturaRepository candidaturaRepository, EventoCandidaturaRepository eventoRepository,
                              UsuarioRepository usuarioRepository, VagaRepository vagaRepository) {
        this.candidaturaRepository = candidaturaRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.vagaRepository = vagaRepository;
    }

    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listar(String login) {
        return candidaturaRepository.listarDoUsuario(usuario(login).getId()).stream()
                .map(CandidaturaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CandidaturaDetalheResponse detalhar(String login, long id) {
        Candidatura candidatura = buscar(login, id);
        return detalhe(candidatura);
    }

    @Transactional
    public CandidaturaDetalheResponse criar(String login, NovaCandidaturaRequest request, LocalDateTime agora) {
        Usuario usuario = usuario(login);
        if (candidaturaRepository.countByUsuarioId(usuario.getId()) >= LIMITE_POR_USUARIO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Limite de " + LIMITE_POR_USUARIO + " candidaturas atingido. Exclua as antigas para continuar.");
        }
        Candidatura candidatura = request.vagaId() != null
                ? daVaga(usuario, request.vagaId(), agora)
                : externa(usuario, request, agora);
        try {
            candidatura = candidaturaRepository.saveAndFlush(candidatura);
        } catch (DataIntegrityViolationException e) {
            // Dois cliques simultâneos em "acompanhar": o índice único decide.
            if (request.vagaId() == null) {
                throw e;
            }
            throw new CandidaturaDuplicadaException(candidaturaRepository
                    .findByUsuarioIdAndVagaId(usuario.getId(), request.vagaId()).orElseThrow(() -> e).getId());
        }
        eventoRepository.save(EventoCandidatura.criada(candidatura, agora));
        return detalhe(candidatura);
    }

    @Transactional
    public CandidaturaDetalheResponse atualizar(String login, long id, AtualizacaoCandidaturaRequest request,
                                                LocalDateTime agora) {
        Candidatura candidatura = buscar(login, id);
        boolean corrigeDados = request.titulo() != null || request.empresa() != null
                || request.local() != null || request.link() != null;
        if (corrigeDados) {
            if (!candidatura.isExterna()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Os dados de uma vaga coletada não podem ser editados");
            }
            candidatura.corrigirDados(
                    obrigatorio(valorOu(request.titulo(), candidatura.getTitulo()), "Informe o título da vaga"),
                    obrigatorio(valorOu(request.empresa(), candidatura.getEmpresa()), "Informe a empresa"),
                    opcional(valorOu(request.local(), candidatura.getLocal())),
                    link(valorOu(request.link(), candidatura.getLink())));
            candidatura.tocar(agora);
        }
        if (Boolean.TRUE.equals(request.limparDataCandidatura())) {
            if (request.dataCandidatura() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Informe a data da candidatura ou peça para limpá-la, não os dois");
            }
            // Ex.: moveu para "candidatei" por engano; sem limpar, ela contaria como enviada no resumo.
            candidatura.definirDataCandidatura(null);
            candidatura.tocar(agora);
        } else if (request.dataCandidatura() != null) {
            validarData(request.dataCandidatura(), agora);
            candidatura.definirDataCandidatura(request.dataCandidatura());
            candidatura.tocar(agora);
        }
        if (request.etapa() != null && request.etapa() != candidatura.getEtapa()) {
            registrarEvento(candidatura, EventoCandidatura.etapa(candidatura, candidatura.moverPara(request.etapa(), agora), agora));
        }
        return detalhe(candidatura);
    }

    @Transactional
    public CandidaturaDetalheResponse definirProximoPasso(String login, long id, String texto, LocalDate data,
                                                          LocalDateTime agora) {
        Candidatura candidatura = buscar(login, id);
        String limpo = opcional(texto);
        if (limpo == null && data != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descreva o próximo passo");
        }
        candidatura.definirProximoPasso(limpo, limpo == null ? null : data);
        candidatura.tocar(agora);
        return detalhe(candidatura);
    }

    @Transactional
    public EventoCandidaturaResponse adicionarNota(String login, long id, String texto, LocalDateTime agora) {
        Candidatura candidatura = buscar(login, id);
        EventoCandidatura nota = registrarEvento(candidatura, EventoCandidatura.nota(candidatura, texto.strip(), agora));
        candidatura.tocar(agora);
        return EventoCandidaturaResponse.from(nota);
    }

    @Transactional
    public void apagarNota(String login, long id, long notaId) {
        Candidatura candidatura = buscar(login, id);
        EventoCandidatura nota = eventoRepository
                .findByIdAndCandidaturaIdAndTipo(notaId, candidatura.getId(), TipoEventoCandidatura.NOTA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota não encontrada"));
        eventoRepository.delete(nota);
    }

    @Transactional
    public void excluir(String login, long id) {
        Candidatura candidatura = buscar(login, id);
        // Os eventos saem junto pelo ON DELETE CASCADE do banco.
        candidaturaRepository.delete(candidatura);
    }

    @Transactional(readOnly = true)
    public ResumoCandidaturasResponse resumir(String login, LocalDateTime agora) {
        long usuarioId = usuario(login).getId();
        List<Candidatura> candidaturas = candidaturaRepository.listarDoUsuario(usuarioId);
        // Primeira vez em que cada candidatura chegou a uma etapa de resposta da empresa.
        Map<Long, LocalDateTime> primeiraResposta = new HashMap<>();
        for (EventoCandidatura evento : eventoRepository.etapasDoUsuario(usuarioId)) {
            if (evento.getEtapaNova().isResposta()) {
                primeiraResposta.putIfAbsent(evento.getCandidatura().getId(), evento.getData());
            }
        }
        return calcularResumo(candidaturas, primeiraResposta, agora);
    }

    /** Separado da consulta para os números serem testáveis com dados montados à mão. */
    static ResumoCandidaturasResponse calcularResumo(List<Candidatura> candidaturas,
                                                     Map<Long, LocalDateTime> primeiraResposta,
                                                     LocalDateTime agora) {
        LocalDate hoje = agora.toLocalDate();
        Map<EtapaCandidatura, Integer> porEtapa = new EnumMap<>(EtapaCandidatura.class);
        for (EtapaCandidatura etapa : EtapaCandidatura.values()) {
            porEtapa.put(etapa, 0);
        }
        LocalDate inicioSemanaAtual = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate inicioPrimeiraSemana = inicioSemanaAtual.minusWeeks(SEMANAS_NO_RESUMO - 1);
        int[] porSemana = new int[SEMANAS_NO_RESUMO];

        int ativas = 0;
        int enviadas = 0;
        int enviadasNoMes = 0;
        int responderam = 0;
        int followUps = 0;
        long somaDias = 0;
        int comTempoDeResposta = 0;

        for (Candidatura c : candidaturas) {
            porEtapa.merge(c.getEtapa(), 1, Integer::sum);
            if (!c.getEtapa().isEncerrada()) {
                ativas++;
                if (precisaFollowUp(c, agora)) {
                    followUps++;
                }
            }
            LocalDate envio = c.getDataCandidatura();
            if (envio == null) {
                continue;
            }
            enviadas++;
            if (envio.getYear() == hoje.getYear() && envio.getMonth() == hoje.getMonth()) {
                enviadasNoMes++;
            }
            if (!envio.isBefore(inicioPrimeiraSemana) && !envio.isAfter(hoje)) {
                porSemana[(int) ChronoUnit.WEEKS.between(inicioPrimeiraSemana, envio)]++;
            }
            LocalDateTime resposta = primeiraResposta.get(c.getId());
            if (resposta != null || c.getEtapa().isResposta()) {
                responderam++;
            }
            if (resposta != null && !resposta.toLocalDate().isBefore(envio)) {
                somaDias += ChronoUnit.DAYS.between(envio, resposta.toLocalDate());
                comTempoDeResposta++;
            }
        }

        List<ResumoCandidaturasResponse.Semana> semanas = new ArrayList<>();
        for (int i = 0; i < SEMANAS_NO_RESUMO; i++) {
            semanas.add(new ResumoCandidaturasResponse.Semana(inicioPrimeiraSemana.plusWeeks(i), porSemana[i]));
        }
        Double taxa = enviadas == 0 ? null : arredondar((double) responderam / enviadas, 100);
        Double diasMedio = comTempoDeResposta == 0 ? null : arredondar((double) somaDias / comTempoDeResposta, 10);
        return new ResumoCandidaturasResponse(candidaturas.size(), ativas, candidaturas.size() - ativas, porEtapa,
                enviadas, enviadasNoMes, responderam, taxa, diasMedio, followUps, semanas);
    }

    /** Parado em "candidatei" há duas semanas ou mais, ou com o próximo passo vencido. */
    static boolean precisaFollowUp(Candidatura c, LocalDateTime agora) {
        boolean parado = c.getEtapa() == EtapaCandidatura.CANDIDATADO
                && !c.getEtapaDesde().plusDays(DIAS_PARA_FOLLOW_UP).isAfter(agora);
        boolean passoVencido = c.getDataProximoPasso() != null && c.getDataProximoPasso().isBefore(agora.toLocalDate());
        return parado || passoVencido;
    }

    @Transactional(readOnly = true)
    public List<Candidatura> listarParaExportar(String login) {
        return candidaturaRepository.listarDoUsuario(usuario(login).getId());
    }

    private Candidatura daVaga(Usuario usuario, long vagaId, LocalDateTime agora) {
        Vaga vaga = vagaRepository.findById(vagaId)
                .filter(v -> v.getStatus() != StatusVaga.OCULTA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga não encontrada"));
        candidaturaRepository.findByUsuarioIdAndVagaId(usuario.getId(), vagaId).ifPresent(existente -> {
            throw new CandidaturaDuplicadaException(existente.getId());
        });
        return Candidatura.daVaga(usuario, vaga, agora);
    }

    private Candidatura externa(Usuario usuario, NovaCandidaturaRequest request, LocalDateTime agora) {
        EtapaCandidatura etapa = request.etapa() == null ? EtapaCandidatura.INTERESSE : request.etapa();
        Candidatura candidatura = Candidatura.externa(usuario,
                obrigatorio(request.titulo(), "Informe o título da vaga"),
                obrigatorio(request.empresa(), "Informe a empresa"),
                opcional(request.local()),
                link(request.link()),
                etapa, agora);
        if (request.dataCandidatura() != null) {
            validarData(request.dataCandidatura(), agora);
            candidatura.definirDataCandidatura(request.dataCandidatura());
        } else if (etapa.impliesEnvio()) {
            candidatura.definirDataCandidatura(agora.toLocalDate());
        }
        return candidatura;
    }

    private EventoCandidatura registrarEvento(Candidatura candidatura, EventoCandidatura evento) {
        if (eventoRepository.countByCandidaturaId(candidatura.getId()) >= LIMITE_EVENTOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limite de registros desta candidatura atingido");
        }
        return eventoRepository.save(evento);
    }

    private CandidaturaDetalheResponse detalhe(Candidatura candidatura) {
        List<EventoCandidaturaResponse> eventos = eventoRepository
                .findByCandidaturaIdOrderByDataAscIdAsc(candidatura.getId()).stream()
                .map(EventoCandidaturaResponse::from)
                .toList();
        return new CandidaturaDetalheResponse(CandidaturaResponse.from(candidatura), eventos);
    }

    private Candidatura buscar(String login, long id) {
        return candidaturaRepository.buscarDoUsuario(id, usuario(login).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidatura não encontrada"));
    }

    private Usuario usuario(String login) {
        return usuarioRepository.findByLogin(UsuarioService.normalizarLogin(login))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida"));
    }

    private static void validarData(LocalDate data, LocalDateTime agora) {
        if (data.isAfter(agora.toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data da candidatura não pode estar no futuro");
        }
    }

    private static String valorOu(String novo, String atual) {
        return novo != null ? novo : atual;
    }

    private static String obrigatorio(String valor, String mensagem) {
        String limpo = opcional(valor);
        if (limpo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
        }
        return limpo;
    }

    private static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    /** Só http(s) com host: o link vira um href na página, então {@code javascript:} não pode passar. */
    static String link(String valor) {
        return LinkSeguro.validar(valor, "O link");
    }

    private static double arredondar(double valor, int escala) {
        return Math.round(valor * escala) / (double) escala;
    }
}
