package com.devfirstdoor.curriculo.ia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Chat Completions da OpenAI com Structured Outputs ({@code response_format: json_schema, strict}).
 * Chamado por HTTP direto, como o cliente do Telegram: é uma chamada só e dispensa o SDK.
 * Erros viram {@link FalhaIaException} com mensagem segura: nem a chave nem o corpo do erro vazam.
 */
@Component
public class OpenAiClienteIa implements ClienteIa {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClienteIa.class);

    private final IaProperties properties;
    private final RestClient restClient;

    public OpenAiClienteIa(IaProperties properties) {
        this.properties = properties;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSegundos()));
        // Bufferizado para mandar Content-Length em vez de corpo em blocos (chunked), que alguns
        // proxies e servidores compatíveis com a API da OpenAI recusam. O corpo é pequeno.
        this.restClient = RestClient.builder().baseUrl(properties.getUrlBase())
                .requestFactory(new BufferingClientHttpRequestFactory(fabrica)).build();
    }

    @Override
    public boolean configurado() {
        return properties.isConfigurada();
    }

    @Override
    public String gerarJson(String instrucoes, String entrada, String nomeSchema, Map<String, Object> schema) {
        if (!configurado()) {
            throw new FalhaIaException(HttpStatus.SERVICE_UNAVAILABLE, "A geração com IA não está configurada no servidor.");
        }
        Map<String, Object> corpo = Map.of(
                "model", properties.getModelo(),
                "messages", List.of(
                        Map.of("role", "system", "content", instrucoes),
                        Map.of("role", "user", "content", entrada)),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of("name", nomeSchema, "strict", true, "schema", schema)));
        JsonNode resposta;
        try {
            resposta = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.getChave())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            log.warn("OpenAI respondeu {} para o schema {}", status, nomeSchema);
            if (status == 429) {
                throw new FalhaIaException(HttpStatus.SERVICE_UNAVAILABLE,
                        "A IA está sobrecarregada ou sem créditos no momento. Tente de novo mais tarde.");
            }
            if (status == 401 || status == 403) {
                log.error("Chave da OpenAI recusada: confira OPENAI_API_KEY");
            }
            throw FalhaIaException.indisponivel();
        } catch (ResourceAccessException e) {
            log.warn("Sem resposta da OpenAI ({}): {}", nomeSchema, e.getClass().getSimpleName());
            throw FalhaIaException.indisponivel();
        }
        return extrairConteudo(resposta, nomeSchema);
    }

    static String extrairConteudo(JsonNode resposta, String nomeSchema) {
        if (resposta == null) {
            throw FalhaIaException.indisponivel();
        }
        JsonNode uso = resposta.path("usage");
        log.info("OpenAI {}: {} tokens de entrada ({} em cache), {} de saída", nomeSchema,
                uso.path("prompt_tokens").asInt(), uso.path("prompt_tokens_details").path("cached_tokens").asInt(),
                uso.path("completion_tokens").asInt());
        JsonNode escolha = resposta.path("choices").path(0);
        JsonNode mensagem = escolha.path("message");
        if (!mensagem.path("refusal").isMissingNode() && !mensagem.path("refusal").isNull()) {
            throw new FalhaIaException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "A IA se recusou a processar este conteúdo. Revise a descrição da vaga e tente de novo.");
        }
        if ("length".equals(escolha.path("finish_reason").asString(""))) {
            throw new FalhaIaException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "A resposta da IA ficou longa demais. Tente uma descrição de vaga mais curta.");
        }
        String conteudo = mensagem.path("content").asString("");
        if (conteudo.isBlank()) {
            throw FalhaIaException.indisponivel();
        }
        return conteudo;
    }
}
