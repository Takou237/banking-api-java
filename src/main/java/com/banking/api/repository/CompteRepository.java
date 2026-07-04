package com.banking.api.repository;

import com.banking.api.model.Compte;
import com.banking.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompteRepository extends JpaRepository<Compte, UUID> {
    Optional<Compte> findByNumeroCompte(String numeroCompte);
    boolean existsByEmail(String email);
    List<Compte> findAll();
    List<Compte> findByBanqueId(UUID banqueId);
    long countByBanqueId(UUID banqueId);
}
