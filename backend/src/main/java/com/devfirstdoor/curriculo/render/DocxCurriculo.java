package com.devfirstdoor.curriculo.render;

import com.devfirstdoor.curriculo.CurriculoFinal;
import com.devfirstdoor.curriculo.CurriculoFinal.Secao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Currículo em DOCX escrito à mão (um .docx é um zip de XML), sem Apache POI. Usa os estilos
 * nativos "Title" e "Heading1" e lista com marcador de verdade, que é como os ATS reconhecem
 * nome, seções e tópicos.
 */
public final class DocxCurriculo {

    private DocxCurriculo() {
    }

    public static byte[] gerar(CurriculoFinal c) {
        StringBuilder corpo = new StringBuilder();
        String nome = c.contato() == null ? null : c.contato().nome();
        if (nome != null) corpo.append(paragrafo("Title", nome, false));
        String contato = SecoesCurriculo.linhaContato(c.contato());
        if (!contato.isEmpty()) corpo.append(paragrafo(null, contato, false));
        for (SecoesCurriculo.Bloco bloco : SecoesCurriculo.blocos(c)) {
            corpo.append(paragrafo("Heading1", bloco.titulo(), false));
            for (Secao s : bloco.itens()) {
                corpo.append(paragrafo(null, s.titulo(), true));
                String apoio = SecoesCurriculo.linhaApoio(s);
                if (!apoio.isEmpty()) corpo.append(paragrafo(null, apoio, false));
                for (String bullet : s.bullets()) corpo.append(topico(bullet));
                if (!s.tecnologias().isEmpty()) {
                    corpo.append(paragrafo(null, "Tecnologias: " + String.join(", ", s.tecnologias()), false));
                }
            }
            for (String linha : bloco.linhas()) corpo.append(paragrafo(null, linha, false));
        }
        String documento = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>%s\
                <w:sectPr><w:pgSz w:w="11906" w:h="16838"/>\
                <w:pgMar w:top="1000" w:right="1000" w:bottom="1000" w:left="1000" w:header="0" w:footer="0" w:gutter="0"/>\
                </w:sectPr></w:body></w:document>""".formatted(corpo);

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes)) {
            entrada(zip, "[Content_Types].xml", TIPOS);
            entrada(zip, "_rels/.rels", RELACOES);
            entrada(zip, "word/_rels/document.xml.rels", RELACOES_DOCUMENTO);
            entrada(zip, "word/styles.xml", ESTILOS);
            entrada(zip, "word/numbering.xml", NUMERACAO);
            entrada(zip, "word/document.xml", documento);
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String paragrafo(String estilo, String texto, boolean negrito) {
        String propriedades = estilo == null ? "" : "<w:pPr><w:pStyle w:val=\"" + estilo + "\"/></w:pPr>";
        String run = negrito ? "<w:rPr><w:b/></w:rPr>" : "";
        return "<w:p>" + propriedades + "<w:r>" + run + "<w:t xml:space=\"preserve\">" + xml(texto) + "</w:t></w:r></w:p>";
    }

    private static String topico(String texto) {
        return "<w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"1\"/></w:numPr></w:pPr>"
                + "<w:r><w:t xml:space=\"preserve\">" + xml(texto) + "</w:t></w:r></w:p>";
    }

    /** Escapa XML e remove caracteres de controle, que deixariam o .docx inválido. */
    static String xml(String texto) {
        StringBuilder saida = new StringBuilder();
        texto.codePoints().forEach(cp -> {
            switch (cp) {
                case '&' -> saida.append("&amp;");
                case '<' -> saida.append("&lt;");
                case '>' -> saida.append("&gt;");
                case '"' -> saida.append("&quot;");
                default -> {
                    if (cp == 0x9 || cp == 0xA || cp == 0xD || (cp >= 0x20 && cp != 0xFFFE && cp != 0xFFFF)) {
                        saida.appendCodePoint(cp == 0xA || cp == 0xD || cp == 0x9 ? ' ' : cp);
                    }
                }
            }
        });
        return saida.toString();
    }

    private static void entrada(ZipOutputStream zip, String nome, String conteudo) throws IOException {
        zip.putNextEntry(new ZipEntry(nome));
        zip.write(conteudo.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static final String TIPOS = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">\
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>\
            <Default Extension="xml" ContentType="application/xml"/>\
            <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>\
            <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>\
            <Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>\
            </Types>""";

    private static final String RELACOES = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>\
            </Relationships>""";

    private static final String RELACOES_DOCUMENTO = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>\
            <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering" Target="numbering.xml"/>\
            </Relationships>""";

    private static final String ESTILOS = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">\
            <w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>\
            <w:sz w:val="21"/><w:lang w:val="pt-BR"/></w:rPr></w:rPrDefault>\
            <w:pPrDefault><w:pPr><w:spacing w:after="60" w:line="264" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>\
            <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>\
            <w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/>\
            <w:pPr><w:spacing w:after="80"/></w:pPr><w:rPr><w:b/><w:sz w:val="36"/></w:rPr></w:style>\
            <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/>\
            <w:pPr><w:keepNext/><w:spacing w:before="200" w:after="80"/>\
            <w:pBdr><w:bottom w:val="single" w:sz="4" w:space="1" w:color="808080"/></w:pBdr><w:outlineLvl w:val="0"/></w:pPr>\
            <w:rPr><w:b/><w:caps/><w:sz w:val="22"/></w:rPr></w:style>\
            </w:styles>""";

    private static final String NUMERACAO = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:numbering xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">\
            <w:abstractNum w:abstractNumId="0"><w:multiLevelType w:val="singleLevel"/>\
            <w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="bullet"/><w:lvlText w:val="•"/><w:lvlJc w:val="left"/>\
            <w:pPr><w:ind w:left="360" w:hanging="240"/></w:pPr></w:lvl></w:abstractNum>\
            <w:num w:numId="1"><w:abstractNumId w:val="0"/></w:num>\
            </w:numbering>""";
}
