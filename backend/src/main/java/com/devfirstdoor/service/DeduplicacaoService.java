package com.devfirstdoor.service;

import com.devfirstdoor.domain.Vaga;
import com.devfirstdoor.repository.VagaRepository;
import com.devfirstdoor.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DeduplicacaoService {

    private final VagaRepository vagaRepository;

    public DeduplicacaoService(VagaRepository vagaRepository) {
        this.vagaRepository = vagaRepository;
    }

    /**
     * Hash estável a partir de título normalizado + empresa + fonte. Não usa o link
     * porque a mesma vaga pode ser republicada com uma URL diferente; título+empresa+fonte
     * é o que de fato identifica "a mesma vaga" para fins de deduplicação.
     */
    public String calcularHash(String titulo, String empresa, String fonte) {
        String chave = TextNormalizer.normalizar(titulo) + "|"
                + TextNormalizer.normalizar(empresa) + "|"
                + TextNormalizer.normalizar(fonte);
        return sha256(chave);
    }

    public boolean isDuplicada(String hash) {
        return vagaRepository.existsByHashDeduplicacao(hash);
    }

    /**
     * Calcula o hash de cada vaga coletada e descarta tanto as que já existem no banco
     * quanto as duplicadas dentro do próprio lote (mesma vaga vinda de buscas diferentes).
     */
    public List<Vaga> filtrarNovas(List<Vaga> coletadas) {
        List<Vaga> novas = new ArrayList<>();
        Set<String> hashesDoLote = new HashSet<>();

        for (Vaga vaga : coletadas) {
            String hash = calcularHash(vaga.getTitulo(), vaga.getEmpresa(), vaga.getFonte());
            if (!hashesDoLote.add(hash)) {
                continue;
            }
            if (isDuplicada(hash)) {
                continue;
            }
            vaga.setHashDeduplicacao(hash);
            novas.add(vaga);
        }
        return novas;
    }

    /**
     * Marca como vistas as vagas já salvas que reapareceram numa coleta. A deduplicação
     * as descarta, mas sem isso seriam tomadas por encerradas (ver ExpiracaoVagasService).
     */
    @Transactional
    public int registrarVisita(Collection<String> hashes, LocalDateTime quando) {
        if (hashes.isEmpty()) {
            return 0;
        }
        return vagaRepository.atualizarDataUltimaVisita(hashes, quando);
    }

    private static String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }
}
