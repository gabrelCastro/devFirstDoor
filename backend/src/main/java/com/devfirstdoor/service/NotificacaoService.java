package com.devfirstdoor.service;

import com.devfirstdoor.domain.ExecucaoFonte;
import com.devfirstdoor.domain.SaudeCrawler;
import com.devfirstdoor.domain.StatusFonte;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.ExecucaoFonteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Envia novidades e somente as transições relevantes de saúde dos crawlers. */
@Service
public class NotificacaoService {

    // Margem abaixo dos 4.096 caracteres aceitos pelo Telegram.
    static final int TAMANHO_MAXIMO_MENSAGEM = 4_000;

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private static final String CABECALHO_VAGAS = "Novas vagas no Dev First Door";

    private final ConfiguracaoService configuracaoService;
    private final ExecucaoFonteRepository execucaoFonteRepository;
    private final ClienteTelegram clienteTelegram;

    public NotificacaoService(ConfiguracaoService configuracaoService,
                              ExecucaoFonteRepository execucaoFonteRepository,
                              ClienteTelegram clienteTelegram) {
        this.configuracaoService = configuracaoService;
        this.execucaoFonteRepository = execucaoFonteRepository;
        this.clienteTelegram = clienteTelegram;
    }

    /** Saúde antes de gravar o novo resultado, usada para detectar a transição. */
    @Transactional(readOnly = true)
    public SaudeCrawler consultarSaude(String fonte, boolean ligada) {
        Optional<ExecucaoFonte> ultima =
                execucaoFonteRepository.findFirstByFonteOrderByInicioDescIdDesc(fonte);
        List<ExecucaoFonte> sucessos = execucaoFonteRepository
                .findTop3ByFonteAndStatusOrderByInicioDescIdDesc(fonte, StatusFonte.SUCESSO);
        return SaudeCrawler.calcular(ligada, ultima, sucessos);
    }

    /** Chamado depois que o resultado já está no histórico. */
    @Transactional(readOnly = true)
    public void notificarMudancaSaude(String fonte, boolean ligada, SaudeCrawler anterior) {
        SaudeCrawler atual = consultarSaude(fonte, ligada);
        boolean ficouRuim = atual == SaudeCrawler.FALHA || atual == SaudeCrawler.ALERTA;
        boolean voltouAoNormal = atual == SaudeCrawler.OK
                && (anterior == SaudeCrawler.FALHA || anterior == SaudeCrawler.ALERTA);
        if (atual != anterior && (ficouRuim || voltouAoNormal)) {
            tentarEnviar("Saúde do crawler %s mudou de %s para %s."
                    .formatted(fonte, anterior, atual));
        }
    }

    public void notificarVagasNovas(List<Vaga> vagas) {
        if (vagas.isEmpty()) {
            return;
        }
        List<String> blocos = vagas.stream().map(NotificacaoService::formatarVaga).toList();
        for (String mensagem : agrupar(CABECALHO_VAGAS, blocos)) {
            tentarEnviar(mensagem);
        }
    }

    /** O teste ignora o liga/desliga, mas exige as credenciais já configuradas. */
    public void enviarTeste() {
        ConfiguracaoService.ConfiguracaoTelegram configuracao = configuracaoService.obterTelegram();
        if (!configuracao.tokenPreenchido() || configuracao.chatId().isBlank()) {
            throw new IllegalArgumentException("Informe o token e o chat id do Telegram antes de testar");
        }
        clienteTelegram.enviar(configuracao.token(), configuracao.chatId(),
                "Teste de notificações do Dev First Door.");
    }

    private void tentarEnviar(String texto) {
        ConfiguracaoService.ConfiguracaoTelegram configuracao = configuracaoService.obterTelegram();
        if (!configuracao.ligadas() || !configuracao.tokenPreenchido() || configuracao.chatId().isBlank()) {
            return;
        }
        try {
            clienteTelegram.enviar(configuracao.token(), configuracao.chatId(), texto);
        } catch (RuntimeException e) {
            // A notificação é secundária: indisponibilidade do Telegram não invalida a coleta.
            log.warn("Não foi possível enviar uma notificação pelo Telegram");
        }
    }

    private static String formatarVaga(Vaga vaga) {
        String local = vaga.getLocal() == null || vaga.getLocal().isBlank() ? "não informado" : vaga.getLocal();
        return "%s\nEmpresa: %s\nNível: %s\nLocal: %s\n%s".formatted(
                vaga.getTitulo(), vaga.getEmpresa(), vaga.getNivel(), local, vaga.getLink());
    }

    static List<String> agrupar(String cabecalho, List<String> blocos) {
        List<String> mensagens = new ArrayList<>();
        StringBuilder atual = new StringBuilder(cabecalho);
        for (String bloco : blocos) {
            String separador = "\n\n";
            if (atual.length() + separador.length() + bloco.length() > TAMANHO_MAXIMO_MENSAGEM
                    && atual.length() > cabecalho.length()) {
                mensagens.add(atual.toString());
                atual = new StringBuilder(cabecalho);
            }
            // Campos das vagas têm limites no banco e cabem individualmente nesta margem.
            atual.append(separador).append(bloco);
        }
        mensagens.add(atual.toString());
        return mensagens;
    }
}
