package com.banking.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comptes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_compte", unique = true, nullable = false)
    private String numeroCompte;

    @NotBlank(message = "Le nom du titulaire est obligatoire")
    @Column(name = "nom_titulaire", nullable = false)
    private String nomTitulaire;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Double solde = 0.0;

    @Column(name = "date_creation", nullable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.numeroCompte == null) {
            this.numeroCompte = "BK-" + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase();
        }
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        if (this.solde == null) {
            this.solde = 0.0;
        }
    }
}
