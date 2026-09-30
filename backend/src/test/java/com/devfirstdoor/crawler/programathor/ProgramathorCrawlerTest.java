package com.devfirstdoor.crawler.programathor;

import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.robots.RobotsTxtChecker;
import com.devfirstdoor.service.RegistroDescarte;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgramathorCrawlerTest {

    private final ProgramathorHtmlClient client = mock(ProgramathorHtmlClient.class);
    private final RobotsTxtChecker robotsTxtChecker = mock(RobotsTxtChecker.class);
    private final RegistroDescarte registroDescarte = mock(RegistroDescarte.class);
    private final ProgramathorCrawlerProperties properties = new ProgramathorCrawlerProperties();

    @Test
    void coletar_vagaSemJava_deveRegistrarODescarte() {
        ProgramathorJobDto semJava = new ProgramathorJobDto(
                "Desenvolvedor Python Júnior", "Empresa", "Remoto",
                "https://programathor.com.br/jobs/1", List.of("Python", "Django"));
        when(robotsTxtChecker.isPermitido(anyString(), anyString())).thenReturn(true);
        when(client.buscarTodasAsPaginas(anyString())).thenReturn(List.of(semJava), List.of());
        ProgramathorCrawler crawler = new ProgramathorCrawler(
                client, properties, robotsTxtChecker, registroDescarte);

        assertThat(crawler.coletar()).isEmpty();

        verify(registroDescarte).registrar(
                ProgramathorJobMapper.FONTE, semJava.titulo(), semJava.empresa(), semJava.local(), semJava.link(),
                MotivoDescarte.NAO_JAVA);
    }
}
