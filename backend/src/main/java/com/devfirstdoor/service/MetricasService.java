package com.devfirstdoor.service;

import com.devfirstdoor.controller.dto.MetricasResponse;
import com.devfirstdoor.controller.dto.MetricasResponse.Modalidade;
import com.devfirstdoor.controller.dto.MetricasResponse.VagasNovasDia;
import com.devfirstdoor.domain.MotivoDescarte;
import com.devfirstdoor.domain.NivelVaga;
import com.devfirstdoor.domain.StatusVaga;
import com.devfirstdoor.repository.VagaDescartadaRepository;
import com.devfirstdoor.repository.VagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class MetricasService {

    private static final double SEGUNDOS_POR_HORA = 3_600.0;

    private final VagaRepository vagaRepository;
    private final VagaDescartadaRepository vagaDescartadaRepository;

    public MetricasService(VagaRepository vagaRepository,
                           VagaDescartadaRepository vagaDescartadaRepository) {
        this.vagaRepository = vagaRepository;
        this.vagaDescartadaRepository = vagaDescartadaRepository;
    }

    public MetricasResponse consultar() {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio30Dias = hoje.minusDays(29).atStartOfDay();
        LocalDateTime inicio7Dias = hoje.minusDays(6).atStartOfDay();

        Map<String, Long> ativasPorFonte = new LinkedHashMap<>();
        vagaRepository.contarAtivasPorFonte(StatusVaga.ATIVA)
                .forEach(contagem -> ativasPorFonte.put(contagem.getChave(), contagem.getTotal()));

        Map<NivelVaga, Long> ativasPorNivel = mapaComZeros(NivelVaga.class);
        vagaRepository.contarAtivasPorNivel(StatusVaga.ATIVA)
                .forEach(contagem -> ativasPorNivel.put(contagem.getNivel(), contagem.getTotal()));

        Map<Modalidade, Long> ativasPorModalidade = mapaComZeros(Modalidade.class);
        vagaRepository.contarAtivasPorModalidade(StatusVaga.ATIVA).forEach(contagem ->
                ativasPorModalidade.put(contagem.getRemoto() ? Modalidade.REMOTA : Modalidade.NAO_REMOTA,
                        contagem.getTotal()));

        var novasPorDia = vagaRepository.contarNovasPorDiaEFonteDesde(inicio30Dias).stream()
                .map(contagem -> new VagasNovasDia(
                        contagem.getData(), contagem.getFonte(), contagem.getTotal()))
                .toList();

        Double mediaSegundos = vagaRepository.calcularTempoMedioNoArEmSegundos(StatusVaga.EXPIRADA);

        Map<String, Long> exclusivasPorFonte = new LinkedHashMap<>();
        vagaRepository.contarAtivasExclusivasPorFonte(StatusVaga.ATIVA)
                .forEach(contagem -> exclusivasPorFonte.put(contagem.getChave(), contagem.getTotal()));

        Map<MotivoDescarte, Long> descartesPorMotivo = mapaComZeros(MotivoDescarte.class);
        vagaDescartadaRepository.contarPorMotivoDesde(inicio7Dias).forEach(contagem ->
                descartesPorMotivo.put(contagem.getMotivo(), contagem.getTotal()));

        return new MetricasResponse(ativasPorFonte, ativasPorNivel, ativasPorModalidade,
                novasPorDia, mediaSegundos == null ? 0 : mediaSegundos / SEGUNDOS_POR_HORA,
                exclusivasPorFonte, descartesPorMotivo);
    }

    private static <E extends Enum<E>> Map<E, Long> mapaComZeros(Class<E> tipo) {
        Map<E, Long> mapa = new EnumMap<>(tipo);
        for (E valor : tipo.getEnumConstants()) {
            mapa.put(valor, 0L);
        }
        return mapa;
    }
}
