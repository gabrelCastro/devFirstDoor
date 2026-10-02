package com.devfirstdoor.config;

/**
 * Vai nos {@code details} do {@code Authentication} quando o login veio de um token de sessão,
 * para o logout e a troca de senha saberem qual sessão é a atual. Login por HTTP Basic não tem.
 */
public record SessaoAutenticada(long sessaoId) {
}
