package com.banking.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

// ─── Exception métier ─────────────────────────────────────────────────────────
class BankingException extends RuntimeException {
    private final HttpStatus status;

    public BankingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}

public class BankingExceptions {

    public static class CompteIntrouvableException extends BankingException {
        public CompteIntrouvableException() {
            super(HttpStatus.NOT_FOUND, "Compte introuvable");
        }
        public CompteIntrouvableException(String msg) {
            super(HttpStatus.NOT_FOUND, msg);
        }
    }

    public static class SoldeInsuffisantException extends BankingException {
        public SoldeInsuffisantException() {
            super(HttpStatus.BAD_REQUEST, "Solde insuffisant");
        }
    }

    public static class EmailDejaUtiliseException extends BankingException {
        public EmailDejaUtiliseException() {
            super(HttpStatus.BAD_REQUEST, "Email déjà utilisé");
        }
    }

    public static class VirementInvalideException extends BankingException {
        public VirementInvalideException(String msg) {
            super(HttpStatus.BAD_REQUEST, msg);
        }
    }

    public static class BanqueIntrouvableException extends BankingException {
        public BanqueIntrouvableException() {
            super(HttpStatus.NOT_FOUND, "Banque introuvable");
        }
    }

    public static class BanqueNonVideException extends BankingException {
        public BanqueNonVideException() {
            super(HttpStatus.BAD_REQUEST, "Impossible de supprimer une banque qui possède des comptes");
        }
    }

    // ─── Handler global ───────────────────────────────────────────────────────
    @RestControllerAdvice
    public static class GlobalExceptionHandler {

        @ExceptionHandler(BankingException.class)
        public ResponseEntity<Map<String, String>> handleBankingException(BankingException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(Map.of("erreur", e.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
            String msg = e.getBindingResult().getFieldErrors().stream()
                    .map(f -> f.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(Map.of("erreur", msg));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erreur", "Erreur interne du serveur"));
        }
    }
}
