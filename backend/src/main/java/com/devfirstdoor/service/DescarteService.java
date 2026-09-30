package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.DescartesResponse;
import com.devfirstdoor.controller.dto.VagaDescartadaResponse;
import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.VagaDescartada;
import com.devfirstdoor.repository.VagaDescartadaFiltro;
import com.devfirstdoor.repository.VagaDescartadaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

@Service
public class DescarteService implements RegistroDescarte {

    public static final int DIAS_RETENCAO = 30;
    private static final Sort ORDEM = Sort.by(Sort.Order.desc("dataDescarte"), Sort.Order.desc("id"));

    private final VagaDescartadaRepository repository;

    public DescarteService(VagaDescartadaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void registrar(String fonte, String titulo, String empresa, String local, String link,
                          MotivoDescarte motivo) {
        if (link == null || link.isBlank()) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        VagaDescartada descarte = repository.findByLinkAndMotivo(link, motivo)
                .orElseGet(() -> new VagaDescartada(fonte, titulo, empresa, local, link, motivo, agora));
        descarte.atualizar(fonte, titulo, empresa, local, agora);
        repository.save(descarte);
    }

    @Transactional
    public long removerAntigos() {
        return repository.deleteByDataDescarteBefore(LocalDateTime.now().minusDays(DIAS_RETENCAO));
    }

    @Transactional(readOnly = true)
    public DescartesResponse listar(String fonte, MotivoDescarte motivo, String busca, Pageable pageable) {
        PageRequest pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEM);
        Page<VagaDescartadaResponse> descartes = repository
                .findAll(new VagaDescartadaFiltro(fonte, motivo, busca).toSpecification(), pagina)
                .map(VagaDescartadaResponse::from);

        Map<MotivoDescarte, Long> contagens = new EnumMap<>(MotivoDescarte.class);
        for (MotivoDescarte valor : MotivoDescarte.values()) {
            contagens.put(valor, 0L);
        }
        repository.contarPorMotivo().forEach(contagem ->
                contagens.put(contagem.getMotivo(), contagem.getTotal()));
        return new DescartesResponse(descartes, contagens);
    }
}
