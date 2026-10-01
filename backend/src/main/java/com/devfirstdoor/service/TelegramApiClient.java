package com.devfirstdoor.service;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Cliente HTTP mínimo da Bot API; erros são saneados para nunca expor o token da URL. */
@Component
public class TelegramApiClient implements ClienteTelegram {

    private final RestClient restClient = RestClient.create("https://api.telegram.org");

    @Override
    public void enviar(String token, String chatId, String texto) {
        try {
            restClient.post()
                    .uri("/bot{token}/sendMessage", token)
                    .body(Map.of("chat_id", chatId, "text", texto))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Não encadeia a exceção HTTP: ela pode conter a URL, que inclui o token.
            throw new FalhaEnvioTelegramException();
        }
    }

    public static class FalhaEnvioTelegramException extends RuntimeException {
        public FalhaEnvioTelegramException() {
            super("Não foi possível enviar a mensagem pelo Telegram");
        }
    }
}
