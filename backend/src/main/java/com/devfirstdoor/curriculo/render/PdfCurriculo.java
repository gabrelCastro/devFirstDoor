package com.devfirstdoor.curriculo.render;

import com.devfirstdoor.curriculo.CurriculoFinal;
import com.devfirstdoor.curriculo.CurriculoFinal.Secao;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Currículo em PDF com texto real (não imagem), uma coluna e fonte padrão do PDF: é o formato que
 * os ATS extraem sem embaralhar. Quebra de linha e de página feitas à mão, medindo o texto.
 */
public final class PdfCurriculo {

    private static final float MARGEM = 50;
    private static final float LARGURA = PDRectangle.A4.getWidth() - 2 * MARGEM;
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font NEGRITO = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final PDDocument documento = new PDDocument();
    private PDPageContentStream conteudo;
    private float y;

    private PdfCurriculo() {
    }

    public static byte[] gerar(CurriculoFinal curriculo) {
        try {
            return new PdfCurriculo().escrever(curriculo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] escrever(CurriculoFinal c) throws IOException {
        try (documento) {
            novaPagina();
            String nome = c.contato() == null ? null : c.contato().nome();
            if (nome != null) paragrafo(nome, NEGRITO, 18, 0, 4);
            String contato = SecoesCurriculo.linhaContato(c.contato());
            if (!contato.isEmpty()) paragrafo(contato, REGULAR, 9.5f, 0, 6);

            for (SecoesCurriculo.Bloco bloco : SecoesCurriculo.blocos(c)) {
                titulo(bloco.titulo());
                for (Secao s : bloco.itens()) {
                    garantirEspaco(40);
                    paragrafo(s.titulo(), NEGRITO, 10.5f, 0, 1);
                    String apoio = SecoesCurriculo.linhaApoio(s);
                    if (!apoio.isEmpty()) paragrafo(apoio, REGULAR, 9.5f, 0, 2);
                    for (String bullet : s.bullets()) topico(bullet);
                    if (!s.tecnologias().isEmpty()) {
                        paragrafo("Tecnologias: " + String.join(", ", s.tecnologias()), REGULAR, 9.5f, 0, 2);
                    }
                    y -= 4;
                }
                for (String linha : bloco.linhas()) paragrafo(linha, REGULAR, 10, 0, 2);
            }

            PDDocumentInformation info = documento.getDocumentInformation();
            if (nome != null) {
                info.setTitle("Currículo - " + nome);
                info.setAuthor(nome);
            }
            conteudo.close();
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    private void titulo(String texto) throws IOException {
        garantirEspaco(50);
        y -= 8;
        paragrafo(texto.toUpperCase(java.util.Locale.forLanguageTag("pt-BR")), NEGRITO, 11, 0, 2);
        conteudo.setLineWidth(0.6f);
        conteudo.moveTo(MARGEM, y + 2);
        conteudo.lineTo(MARGEM + LARGURA, y + 2);
        conteudo.stroke();
        y -= 6;
    }

    private void topico(String texto) throws IOException {
        float recuo = 12;
        List<String> linhas = quebrar(seguro(texto, REGULAR), REGULAR, 10, LARGURA - recuo);
        for (int i = 0; i < linhas.size(); i++) {
            garantirEspaco(14);
            if (i == 0) escreverEm("•", REGULAR, 10, MARGEM + 2);
            escreverEm(linhas.get(i), REGULAR, 10, MARGEM + recuo);
            y -= 13;
        }
        y -= 1;
    }

    private void paragrafo(String texto, PDType1Font fonte, float tamanho, float recuo, float depois) throws IOException {
        for (String linha : quebrar(seguro(texto, fonte), fonte, tamanho, LARGURA - recuo)) {
            garantirEspaco(tamanho + 4);
            escreverEm(linha, fonte, tamanho, MARGEM + recuo);
            y -= tamanho * 1.3f;
        }
        y -= depois;
    }

    private void escreverEm(String linha, PDType1Font fonte, float tamanho, float x) throws IOException {
        conteudo.beginText();
        conteudo.setFont(fonte, tamanho);
        conteudo.newLineAtOffset(x, y - tamanho);
        conteudo.showText(linha);
        conteudo.endText();
    }

    private void garantirEspaco(float altura) throws IOException {
        if (y - altura < MARGEM) novaPagina();
    }

    private void novaPagina() throws IOException {
        if (conteudo != null) conteudo.close();
        PDPage pagina = new PDPage(PDRectangle.A4);
        documento.addPage(pagina);
        conteudo = new PDPageContentStream(documento, pagina);
        y = PDRectangle.A4.getHeight() - MARGEM;
    }

    /** Quebra por palavras pela largura medida; palavra maior que a linha é cortada. */
    static List<String> quebrar(String texto, PDType1Font fonte, float tamanho, float largura) throws IOException {
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.replace('\n', ' ').split(" +")) {
            if (palavra.isEmpty()) continue;
            String tentativa = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (medir(tentativa, fonte, tamanho) <= largura) {
                atual = new StringBuilder(tentativa);
                continue;
            }
            if (!atual.isEmpty()) linhas.add(atual.toString());
            atual = new StringBuilder();
            String resto = palavra;
            while (medir(resto, fonte, tamanho) > largura) {
                int corte = resto.length() - 1;
                while (corte > 1 && medir(resto.substring(0, corte), fonte, tamanho) > largura) corte--;
                linhas.add(resto.substring(0, corte));
                resto = resto.substring(corte);
            }
            atual.append(resto);
        }
        if (!atual.isEmpty()) linhas.add(atual.toString());
        return linhas;
    }

    private static float medir(String texto, PDType1Font fonte, float tamanho) throws IOException {
        return fonte.getStringWidth(texto) / 1000 * tamanho;
    }

    /** As fontes padrão do PDF só cobrem o WinAnsi: o que ficar de fora (emoji etc.) sai do texto. */
    static String seguro(String texto, PDType1Font fonte) {
        StringBuilder saida = new StringBuilder();
        texto.codePoints().forEach(cp -> {
            String caractere = new String(Character.toChars(cp));
            if (Character.isWhitespace(cp)) {
                saida.append(' ');
                return;
            }
            try {
                fonte.encode(caractere);
                saida.append(caractere);
            } catch (IllegalArgumentException | IOException e) {
                // sem glifo nesta fonte: omite
            }
        });
        return saida.toString().strip();
    }
}
