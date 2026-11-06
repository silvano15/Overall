package br.com.overall.exception;

import java.time.LocalDateTime;
import java.util.List;

public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String erro;
    private String mensagem;
    private String path;
    private List<CampoInvalido> camposInvalidos;

    public ApiErrorResponse() {}

    public ApiErrorResponse(LocalDateTime timestamp, int status, String erro, String mensagem,
                             String path, List<CampoInvalido> camposInvalidos) {
        this.timestamp = timestamp; this.status = status; this.erro = erro;
        this.mensagem = mensagem; this.path = path; this.camposInvalidos = camposInvalidos;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getErro() { return erro; }
    public String getMensagem() { return mensagem; }
    public String getPath() { return path; }
    public List<CampoInvalido> getCamposInvalidos() { return camposInvalidos; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private LocalDateTime timestamp; private int status; private String erro;
        private String mensagem; private String path; private List<CampoInvalido> camposInvalidos;
        public Builder timestamp(LocalDateTime t) { this.timestamp = t; return this; }
        public Builder status(int s) { this.status = s; return this; }
        public Builder erro(String e) { this.erro = e; return this; }
        public Builder mensagem(String m) { this.mensagem = m; return this; }
        public Builder path(String p) { this.path = p; return this; }
        public Builder camposInvalidos(List<CampoInvalido> c) { this.camposInvalidos = c; return this; }
        public ApiErrorResponse build() {
            return new ApiErrorResponse(timestamp, status, erro, mensagem, path, camposInvalidos);
        }
    }

    public static class CampoInvalido {
        private String campo;
        private String mensagem;
        public CampoInvalido(String campo, String mensagem) { this.campo = campo; this.mensagem = mensagem; }
        public String getCampo() { return campo; }
        public String getMensagem() { return mensagem; }
    }
}
