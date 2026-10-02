package com.devfirstdoor.curriculo.render;

import com.devfirstdoor.curriculo.CurriculoFinal;
import com.devfirstdoor.curriculo.CurriculoFinal.Secao;
import com.devfirstdoor.curriculo.PerfilCurriculo.Contato;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class RenderCurriculoTest {

    private static CurriculoFinal curriculo(List<String> bullets) {
        return new CurriculoFinal(
                new Contato("Maria Conceição", "maria@exemplo.com", "(81) 99999-0000", "Recife, PE", null,
                        "https://github.com/maria", null),
                "Estudante de Análise e Desenvolvimento de Sistemas com foco em backend Java.",
                List.of(new Secao("Estagiária de Desenvolvimento", "Banco X · Recife", "02/2025 – atual", bullets,
                        List.of("Java", "Spring Boot"))),
                List.of(new Secao("Agenda", "https://github.com/maria/agenda", "", List.of("App em React <com> & API"), List.of())),
                List.of("Análise e Desenvolvimento de Sistemas — UFPE (02/2023 – cursando)"),
                List.of("Docker para iniciantes — Alura (2024)"),
                List.of("Java", "Spring Boot", "Git"),
                List.of("Inglês — Intermediário"));
    }

    private static String textoDoPdf(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void pdf_temTextoRealNaOrdemCertaComAcentos() throws Exception {
        byte[] pdf = PdfCurriculo.gerar(curriculo(List.of("Desenvolvi APIs de cadastro em Java e Spring Boot 🚀",
                "Corrigi 15 bugs reportados pelo QA")));
        String texto = textoDoPdf(pdf);

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(texto).contains("Maria Conceição", "maria@exemplo.com | (81) 99999-0000 | Recife, PE",
                "RESUMO", "EXPERIÊNCIA", "Estagiária de Desenvolvimento", "Banco X · Recife | 02/2025 – atual",
                "Desenvolvi APIs de cadastro em Java e Spring Boot", "Tecnologias: Java, Spring Boot",
                "PROJETOS", "FORMAÇÃO", "Análise e Desenvolvimento de Sistemas — UFPE", "HABILIDADES", "IDIOMAS");
        // Ordem das seções como o ATS espera.
        assertThat(texto.indexOf("RESUMO")).isLessThan(texto.indexOf("EXPERIÊNCIA"));
        assertThat(texto.indexOf("EXPERIÊNCIA")).isLessThan(texto.indexOf("PROJETOS"));
        assertThat(texto.indexOf("FORMAÇÃO")).isLessThan(texto.indexOf("HABILIDADES"));
        // Emoji fora do WinAnsi é omitido em vez de quebrar a geração.
        assertThat(texto).doesNotContain("🚀");
    }

    @Test
    void pdf_quebraLinhasEPaginas() throws Exception {
        String longo = "Implementei uma funcionalidade bastante detalhada que precisa ocupar mais de uma linha "
                + "para garantir que a quebra por palavras funcione corretamente no PDF gerado";
        byte[] pdf = PdfCurriculo.gerar(curriculo(Collections.nCopies(60, longo)));
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
        assertThat(textoDoPdf(pdf)).contains("Implementei uma funcionalidade");
    }

    @Test
    void docx_eUmZipComXmlValidoEstilosELista() throws Exception {
        byte[] docx = DocxCurriculo.gerar(curriculo(List.of("Desenvolvi APIs & <testes>")));
        Map<String, String> partes = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                partes.put(entrada.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertThat(partes).containsKeys("[Content_Types].xml", "_rels/.rels", "word/document.xml",
                "word/styles.xml", "word/numbering.xml", "word/_rels/document.xml.rels");

        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        for (String parte : partes.values()) {
            fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(parte.getBytes(StandardCharsets.UTF_8)));
        }
        Document documento = fabrica.newDocumentBuilder()
                .parse(new ByteArrayInputStream(partes.get("word/document.xml").getBytes(StandardCharsets.UTF_8)));
        String texto = documento.getDocumentElement().getTextContent();
        assertThat(texto).contains("Maria Conceição", "Experiência", "Desenvolvi APIs & <testes>",
                "App em React <com> & API", "Tecnologias: Java, Spring Boot");
        assertThat(partes.get("word/document.xml")).contains("w:val=\"Title\"", "w:val=\"Heading1\"", "<w:numId w:val=\"1\"/>");
    }

    @Test
    void xml_removeCaracteresDeControle() {
        assertThat(DocxCurriculo.xml("a\u0001b<c>&\"d\"")).isEqualTo("ab&lt;c&gt;&amp;&quot;d&quot;");
    }
}
