package com.devfirstdoor.curriculo.ia;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Servidor HTTP local no papel da OpenAI: confere o pedido e o tratamento das respostas, sem rede. */
class OpenAiClienteIaTest {

    private HttpServer servidor;
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicReference<String> autorizacao = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String resposta = "";

    @BeforeEach
    void subir() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/chat/completions", troca -> {
            corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            autorizacao.set(troca.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            troca.getResponseBody().write(bytes);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void descer() {
        servidor.stop(0);
    }

    private OpenAiClienteIa cliente(String chave) {
        IaProperties p = new IaProperties();
        p.setChave(chave);
        p.setModelo("modelo-teste");
        p.setUrlBase("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1");
        p.setTimeoutSegundos(5);
        return new OpenAiClienteIa(p);
    }

    private static String respostaComConteudo(String conteudo, String finish, String refusal) {
        return JsonMapper.builder().build().writeValueAsString(Map.of(
                "choices", java.util.List.of(Map.of("finish_reason", finish, "message",
                        refusal == null ? Map.of("role", "assistant", "content", conteudo)
                                : Map.of("role", "assistant", "refusal", refusal))),
                "usage", Map.of("prompt_tokens", 10, "completion_tokens", 5)));
    }

    @Test
    void enviaStructuredOutputsStrictEDevolveOConteudo() {
        resposta = respostaComConteudo("{\"ok\":true}", "stop", null);
        String json = cliente("sk-teste").gerarJson("instruções", "entrada", "meu_schema", Schema.objeto(Schema.props(
                Schema.campo("ok", Map.of("type", "boolean")))));

        assertThat(json).isEqualTo("{\"ok\":true}");
        assertThat(autorizacao.get()).isEqualTo("Bearer sk-teste");
        JsonNode pedido = JsonMapper.builder().build().readTree(corpoRecebido.get());
        assertThat(pedido.path("model").asString()).isEqualTo("modelo-teste");
        assertThat(pedido.path("messages").path(0).path("role").asString()).isEqualTo("system");
        assertThat(pedido.path("messages").path(1).path("content").asString()).isEqualTo("entrada");
        assertThat(pedido.path("response_format").path("type").asString()).isEqualTo("json_schema");
        assertThat(pedido.path("response_format").path("json_schema").path("strict").asBoolean()).isTrue();
        assertThat(pedido.path("response_format").path("json_schema").path("name").asString()).isEqualTo("meu_schema");
    }

    @Test
    void recusaRespostaCortadaEErrosViramMensagensSeguras() {
        resposta = respostaComConteudo(null, "stop", "não posso");
        assertThatThrownBy(() -> cliente("sk-teste").gerarJson("i", "e", "s", Map.of()))
                .isInstanceOf(FalhaIaException.class).hasMessageContaining("recusou");

        resposta = respostaComConteudo("{\"inco", "length", null);
        assertThatThrownBy(() -> cliente("sk-teste").gerarJson("i", "e", "s", Map.of()))
                .hasMessageContaining("longa demais");

        status = 429;
        resposta = "{\"error\":{\"message\":\"Rate limit for key sk-teste\"}}";
        assertThatThrownBy(() -> cliente("sk-teste").gerarJson("i", "e", "s", Map.of()))
                .isInstanceOf(FalhaIaException.class)
                .hasMessageContaining("sobrecarregada")
                .hasMessageNotContaining("sk-teste");

        status = 401;
        assertThatThrownBy(() -> cliente("sk-errada").gerarJson("i", "e", "s", Map.of()))
                .hasMessageContaining("indisponível")
                .hasMessageNotContaining("sk-errada");
    }

    @Test
    void semChave_naoChamaAApi() {
        assertThat(cliente("").configurado()).isFalse();
        assertThatThrownBy(() -> cliente("").gerarJson("i", "e", "s", Map.of()))
                .hasMessageContaining("não está configurada");
        assertThat(corpoRecebido.get()).isNull();
    }
}
