package com.devfirstdoor.domain;

/** O que disparou uma coleta: o startup, o agendamento ou o painel admin. */
public enum OrigemColeta {
    INICIAL,
    AGENDADA,
    MANUAL
}
