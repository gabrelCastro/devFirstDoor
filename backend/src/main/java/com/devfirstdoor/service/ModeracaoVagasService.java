package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.AtualizacaoVagaAdminRequest;
import com.devfirstdoor.controller.dto.GrupoDuplicatasResponse;
import com.devfirstdoor.controller.dto.ResolucaoDuplicatasResponse;
import com.devfirstdoor.controller.dto.VagaAdminResponse;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaAdminFiltro;
import com.devfirstdoor.repository.VagaRepository;
import com.devfirstdoor.util.TextNormalizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModeracaoVagasService {

    private static final Sort ORDEM = Sort.by(Sort.Order.desc("dataColeta"), Sort.Order.desc("id"));

    private final VagaRepository vagaRepository;

    public ModeracaoVagasService(VagaRepository vagaRepository) {
        this.vagaRepository = vagaRepository;
    }

    @Transactional(readOnly = true)
    public Page<VagaAdminResponse> listar(StatusVaga status, String fonte, String busca, Pageable pageable) {
        PageRequest pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEM);
        return vagaRepository.findAll(new VagaAdminFiltro(status, fonte, busca).toSpecification(), pagina)
                .map(VagaAdminResponse::from);
    }

    @Transactional
    public VagaAdminResponse atualizar(long id, AtualizacaoVagaAdminRequest request) {
        Vaga vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga não encontrada"));
        if (request.status() != null) {
            vaga.alterarStatus(request.status());
        }
        if (request.nivel() != null) {
            vaga.corrigirNivel(request.nivel());
        }
        if (request.remoto() != null) {
            vaga.corrigirRemoto(request.remoto());
        }
        return VagaAdminResponse.from(vagaRepository.save(vaga));
    }

    /** Recalcula classificações reproduzíveis a partir dos dados salvos, sem apagar correções manuais. */
    @Transactional
    public int reclassificarTodas() {
        var vagas = vagaRepository.findAll();
        vagas.forEach(Vaga::atualizarCamposDerivados);
        vagaRepository.saveAll(vagas);
        return vagas.size();
    }

    @Transactional(readOnly = true)
    public List<GrupoDuplicatasResponse> listarDuplicatas() {
        return agruparAtivas().entrySet().stream()
                .filter(entry -> temFontesDiferentes(entry.getValue()))
                .map(entry -> new GrupoDuplicatasResponse(
                        entry.getKey().titulo(),
                        entry.getKey().empresa(),
                        entry.getValue().stream().map(VagaAdminResponse::from).toList()))
                .toList();
    }

    /** Oculta o grupo calculado no momento da ação, evitando resolver dados obsoletos do painel. */
    @Transactional
    public ResolucaoDuplicatasResponse resolverDuplicatas(long vagaMantidaId) {
        Vaga mantida = vagaRepository.findById(vagaMantidaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga não encontrada"));
        List<Vaga> grupo = agruparAtivas().getOrDefault(ChaveDuplicata.from(mantida), List.of());
        boolean mantidaEstaAtiva = grupo.stream().anyMatch(vaga -> vaga.getId().equals(vagaMantidaId));
        if (!mantidaEstaAtiva || !temFontesDiferentes(grupo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A vaga mantida não pertence a um grupo de duplicatas ativo");
        }

        List<Vaga> ocultadas = grupo.stream()
                .filter(vaga -> !vaga.getId().equals(vagaMantidaId))
                .toList();
        ocultadas.forEach(vaga -> vaga.alterarStatus(StatusVaga.OCULTA));
        vagaRepository.saveAll(ocultadas);
        return new ResolucaoDuplicatasResponse(vagaMantidaId, ocultadas.size());
    }

    private Map<ChaveDuplicata, List<Vaga>> agruparAtivas() {
        List<Vaga> ativas = vagaRepository.findAll(
                new VagaAdminFiltro(StatusVaga.ATIVA, null, null).toSpecification(), ORDEM);
        Map<ChaveDuplicata, List<Vaga>> grupos = new LinkedHashMap<>();
        for (Vaga vaga : ativas) {
            grupos.computeIfAbsent(ChaveDuplicata.from(vaga), ignored -> new ArrayList<>()).add(vaga);
        }
        return grupos;
    }

    private static boolean temFontesDiferentes(List<Vaga> vagas) {
        return vagas.stream().map(Vaga::getFonte).distinct().limit(2).count() == 2;
    }

    private record ChaveDuplicata(String titulo, String empresa) {

        private static ChaveDuplicata from(Vaga vaga) {
            return new ChaveDuplicata(
                    TextNormalizer.normalizar(vaga.getTitulo()),
                    TextNormalizer.normalizar(vaga.getEmpresa()));
        }
    }
}
