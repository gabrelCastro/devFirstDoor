package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.ContagensResponse;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaFiltro;
import com.devfirstdoor.repository.VagaFiltro.Escopo;
import com.devfirstdoor.repository.VagaFiltro.Secao;
import com.devfirstdoor.repository.VagaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ConsultaVagasService {

    /** Ordem fixa (mais recentes primeiro, id para desempatar) para o "carregar mais" não repetir nem pular vagas. */
    private static final Sort ORDEM = Sort.by(Sort.Order.desc("dataColeta"), Sort.Order.desc("id"));

    private final VagaRepository vagaRepository;

    public ConsultaVagasService(VagaRepository vagaRepository) {
        this.vagaRepository = vagaRepository;
    }

    public Page<Vaga> listar(VagaFiltro filtro, Pageable pageable) {
        PageRequest pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEM);
        return vagaRepository.findAll(filtro.toSpecification(), pagina);
    }

    public ContagensResponse contar(VagaFiltro filtro) {
        Map<Secao, Long> porSecao = new EnumMap<>(Secao.class);
        for (Secao secao : Secao.values()) {
            porSecao.put(secao, vagaRepository.count(filtro.comSecao(secao).toSpecification()));
        }
        Map<Escopo, Long> porEscopo = new EnumMap<>(Escopo.class);
        for (Escopo escopo : Escopo.values()) {
            porEscopo.put(escopo, vagaRepository.count(filtro.comEscopo(escopo).toSpecification()));
        }
        return new ContagensResponse(vagaRepository.count(), vagaRepository.findFontes(), porSecao, porEscopo);
    }

    /**
     * Preenche as colunas derivadas (remoto, internacional, texto de busca) das vagas
     * salvas antes de elas existirem, para que essas vagas apareçam nos filtros.
     */
    @Transactional
    public int preencherCamposDerivadosPendentes() {
        var pendentes = vagaRepository.findByRemotoIsNullOrInternacionalIsNullOrTextoBuscaIsNull();
        pendentes.forEach(Vaga::atualizarCamposDerivados);
        vagaRepository.saveAll(pendentes);
        return pendentes.size();
    }
}
