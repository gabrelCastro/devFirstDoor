package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.AtualizacaoVagaAdminRequest;
import com.devfirstdoor.controller.dto.VagaAdminResponse;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaAdminFiltro;
import com.devfirstdoor.repository.VagaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
}
