package br.com.overall.exception;

public class RecursoNaoEncontradoException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public RecursoNaoEncontradoException(String mensagem) { super(mensagem); }
}
