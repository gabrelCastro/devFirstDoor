package com.devfirstdoor.domain;

/** Papel de uma conta. Vira a authority {@code ROLE_<nome>} no Spring Security. */
public enum Papel {
    USUARIO,
    ADMIN
}
