package com.devfirstdoor.service;

import com.devfirstdoor.domain.MotivoDescarte;

@FunctionalInterface
public interface RegistroDescarte {

    RegistroDescarte NENHUM = (fonte, titulo, empresa, local, link, motivo) -> { };

    void registrar(String fonte, String titulo, String empresa, String local, String link,
                   MotivoDescarte motivo);
}
