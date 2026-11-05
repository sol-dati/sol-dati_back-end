package com.soldati.exception;

public class SoldatiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SoldatiException(String message) {
        super(message);
    }

    // conserva l'eccezione originale (IOException, MavenInvocationException...)
    public SoldatiException(String message, Throwable cause) {
        super(message, cause);
    }
}