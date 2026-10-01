package com.devfirstdoor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "vaga")
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String empresa;

    private String local;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelVaga nivel;

    // Anuláveis para o ddl-auto conseguir adicioná-las em bancos já populados.
    // Os getters tratam nulo como os padrões anteriores à área de moderação.
    @Enumerated(EnumType.STRING)
    private StatusVaga status;

    private Boolean nivelManual;

    private Boolean remotoManual;

    @Column(nullable = false, unique = true, length = 1024)
    private String link;

    @Column(nullable = false)
    private String fonte;

    private LocalDate dataPublicacao;

    @Column(nullable = false)
    private LocalDateTime dataColeta;

    @Column(nullable = false, unique = true, length = 64)
    private String hashDeduplicacao;

    // Anulável pelo mesmo motivo das colunas derivadas; vagas antigas sem ela contam
    // a partir da dataColeta (ver VagaRepository#marcarExpiradas).
    private LocalDateTime dataUltimaVisita;

    // Colunas derivadas (ver ClassificacaoVaga). Anuláveis para o "ddl-auto: update"
    // conseguir adicioná-las em bancos com vagas antigas, preenchidas depois no startup.
    private Boolean remoto;

    private Boolean internacional;

    @Column(length = 2048)
    private String textoBusca;

    // Permitem comparar duplicatas entre fontes diretamente no banco, sem carregar
    // todas as vagas para normalizar em memória.
    @Column(length = 512)
    private String tituloNormalizado;

    @Column(length = 512)
    private String empresaNormalizada;

    protected Vaga() {
    }

    public Vaga(String titulo, String empresa, String local, NivelVaga nivel, String link,
                String fonte, LocalDate dataPublicacao, LocalDateTime dataColeta) {
        this.titulo = titulo;
        this.empresa = empresa;
        this.local = local;
        this.nivel = nivel;
        this.link = link;
        this.fonte = fonte;
        this.dataPublicacao = dataPublicacao;
        this.dataColeta = dataColeta;
        this.dataUltimaVisita = dataColeta;
        this.status = StatusVaga.ATIVA;
        this.nivelManual = false;
        this.remotoManual = false;
        atualizarCamposDerivados();
    }

    public void atualizarCamposDerivados() {
        if (!isRemotoManual()) {
            this.remoto = ClassificacaoVaga.isRemoto(local);
        }
        this.internacional = ClassificacaoVaga.isInternacional(fonte, local);
        this.textoBusca = ClassificacaoVaga.textoDeBusca(titulo, empresa, local);
        this.tituloNormalizado = ClassificacaoVaga.normalizarParaDuplicata(titulo);
        this.empresaNormalizada = ClassificacaoVaga.normalizarParaDuplicata(empresa);
    }

    public void alterarStatus(StatusVaga status) {
        this.status = Objects.requireNonNull(status);
    }

    public void corrigirNivel(NivelVaga nivel) {
        this.nivel = Objects.requireNonNull(nivel);
        this.nivelManual = true;
    }

    public void corrigirRemoto(boolean remoto) {
        this.remoto = remoto;
        this.remotoManual = true;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getEmpresa() {
        return empresa;
    }

    public String getLocal() {
        return local;
    }

    public NivelVaga getNivel() {
        return nivel;
    }

    public StatusVaga getStatus() {
        return status != null ? status : StatusVaga.ATIVA;
    }

    public boolean isNivelManual() {
        return Boolean.TRUE.equals(nivelManual);
    }

    public boolean isRemotoManual() {
        return Boolean.TRUE.equals(remotoManual);
    }

    public String getLink() {
        return link;
    }

    public String getFonte() {
        return fonte;
    }

    public LocalDate getDataPublicacao() {
        return dataPublicacao;
    }

    public LocalDateTime getDataColeta() {
        return dataColeta;
    }

    public LocalDateTime getDataUltimaVisita() {
        return dataUltimaVisita;
    }

    public String getHashDeduplicacao() {
        return hashDeduplicacao;
    }

    public boolean isRemoto() {
        return Boolean.TRUE.equals(remoto);
    }

    public boolean isInternacional() {
        return Boolean.TRUE.equals(internacional);
    }

    public String getTextoBusca() {
        return textoBusca;
    }

    public String getTituloNormalizado() {
        return tituloNormalizado;
    }

    public String getEmpresaNormalizada() {
        return empresaNormalizada;
    }

    public void setHashDeduplicacao(String hashDeduplicacao) {
        this.hashDeduplicacao = hashDeduplicacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vaga vaga)) return false;
        return Objects.equals(link, vaga.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(link);
    }
}
