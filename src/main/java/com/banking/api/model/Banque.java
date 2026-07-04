package com.banking.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "banques")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Le nom de la banque est obligatoire")
    @Column(nullable = false)
    private String nom;

    @Column(unique = true, nullable = false)
    private String code;

    private String adresse;

    @Column(name = "date_creation", nullable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    // Une banque peut avoir plusieurs comptes.
    // @JsonIgnore pour éviter la boucle infinie de sérialisation (Compte -> Banque -> comptes -> ...).
    // Pas de cascade : la suppression d'une banque est bloquée tant qu'elle a des comptes (voir BanqueService).
    @OneToMany(mappedBy = "banque", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Compte> comptes = new java.util.ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.code == null) {
            this.code = "BQ-" + UUID.randomUUID().toString()
                    .substring(0, 6).toUpperCase();
        }
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
    }
}
