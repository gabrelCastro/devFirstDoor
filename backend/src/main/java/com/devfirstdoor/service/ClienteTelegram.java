package com.devfirstdoor.service;

/** Porta isolada para os testes nunca precisarem acessar a API real do Telegram. */
public interface ClienteTelegram {

    void enviar(String token, String chatId, String texto);
}
