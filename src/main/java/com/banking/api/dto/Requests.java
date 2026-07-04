package com.banking.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

// ─── Requêtes ─────────────────────────────────────────────────────────────────

public class Requests {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CreerCompteRequest {
        @NotBlank(message = "Le nom du titulaire est obligatoire")
        private String nomTitulaire;

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        private String email;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MontantRequest {
        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif")
        private Double montant;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class VirementRequest {
        @NotBlank(message = "Le numéro de compte destination est obligatoire")
        private String numeroCompteDestination;

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif")
        private Double montant;
    }
}
