package com.banking.api.repository;

import com.banking.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT t FROM Transaction t WHERE t.compteSource = :numero OR t.compteDestination = :numero ORDER BY t.date DESC")
    List<Transaction> findByCompte(@Param("numero") String numeroCompte);

    @Query("DELETE FROM Transaction t WHERE t.compteSource = :numero OR t.compteDestination = :numero")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByCompte(@Param("numero") String numeroCompte);

    long countByCompteSourceOrCompteDestination(String source, String dest);
}
