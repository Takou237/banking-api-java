package com.banking.api.repository;

import com.banking.api.model.Banque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BanqueRepository extends JpaRepository<Banque, UUID> {
}
