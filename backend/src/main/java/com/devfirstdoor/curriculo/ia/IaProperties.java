package com.devfirstdoor.curriculo.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da IA do currículo. Sem chave, a adaptação fica desligada (503) e o resto do
 * currículo (perfil, PDF, DOCX) continua funcionando.
 */
@ConfigurationProperties(prefix = "app.ia")
public class IaProperties {

    /** Chave da OpenAI (OPENAI_API_KEY). Só o backend a conhece. */
    private String chave = "";

    /** Modelo usado nas duas chamadas (OPENAI_MODEL). Precisa suportar Structured Outputs. */
    private String modelo = "gpt-4o-mini";

    private String urlBase = "https://api.openai.com/v1";

    private int timeoutSegundos = 90;

    /** Adaptações por conta a cada 24 horas: o cadastro é aberto e cada chamada custa dinheiro. */
    private int limiteDiario = 10;

    public boolean isConfigurada() {
        return chave != null && !chave.isBlank();
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public int getTimeoutSegundos() {
        return timeoutSegundos;
    }

    public void setTimeoutSegundos(int timeoutSegundos) {
        this.timeoutSegundos = timeoutSegundos;
    }

    public int getLimiteDiario() {
        return limiteDiario;
    }

    public void setLimiteDiario(int limiteDiario) {
        this.limiteDiario = limiteDiario;
    }
}
