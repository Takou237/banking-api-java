package com.banking.api.service;

import com.banking.api.dto.Requests.*;
import com.banking.api.exception.BankingExceptions.*;
import com.banking.api.model.Banque;
import com.banking.api.model.Compte;
import com.banking.api.model.Transaction;
import com.banking.api.model.Transaction.TypeTransaction;
import com.banking.api.repository.BanqueRepository;
import com.banking.api.repository.CompteRepository;
import com.banking.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankingService {

    private final CompteRepository compteRepository;
    private final TransactionRepository transactionRepository;
    private final BanqueRepository banqueRepository;

    // ─── Comptes ──────────────────────────────────────────────────────────────

    @Transactional
    public Compte creerCompte(CreerCompteRequest request) {
        if (compteRepository.existsByEmail(request.getEmail())) {
            throw new EmailDejaUtiliseException();
        }
        Banque banque = banqueRepository.findById(request.getBanqueId())
                .orElseThrow(BanqueIntrouvableException::new);
        Compte compte = Compte.builder()
                .nomTitulaire(request.getNomTitulaire())
                .email(request.getEmail())
                .banque(banque)
                .build();
        return compteRepository.save(compte);
    }

    public List<Compte> listerComptes() {
        return compteRepository.findAll();
    }

    public List<Compte> listerComptesParBanque(UUID banqueId) {
        if (!banqueRepository.existsById(banqueId)) {
            throw new BanqueIntrouvableException();
        }
        return compteRepository.findByBanqueId(banqueId);
    }

    // ─── Banques ──────────────────────────────────────────────────────────────

    @Transactional
    public Banque creerBanque(CreerBanqueRequest request) {
        Banque banque = Banque.builder()
                .nom(request.getNom())
                .adresse(request.getAdresse())
                .build();
        return banqueRepository.save(banque);
    }

    public List<Banque> listerBanques() {
        return banqueRepository.findAll();
    }

    public Banque consulterBanque(UUID banqueId) {
        return banqueRepository.findById(banqueId)
                .orElseThrow(BanqueIntrouvableException::new);
    }

    @Transactional
    public Map<String, Object> supprimerBanque(UUID banqueId) {
        Banque banque = consulterBanque(banqueId);
        if (compteRepository.countByBanqueId(banqueId) > 0) {
            throw new BanqueNonVideException();
        }
        banqueRepository.delete(banque);
        return Map.of("succes", true, "banque_supprimee", banque.getNom());
    }

    public Compte consulterCompte(String numeroCompte) {
        return compteRepository.findByNumeroCompte(numeroCompte)
                .orElseThrow(CompteIntrouvableException::new);
    }

    @Transactional
    public Map<String, Object> supprimerCompte(String numeroCompte) {
        Compte compte = consulterCompte(numeroCompte);
        long nbTxns = transactionRepository
                .countByCompteSourceOrCompteDestination(numeroCompte, numeroCompte);
        transactionRepository.deleteByCompte(numeroCompte);
        compteRepository.delete(compte);
        return Map.of(
                "succes", true,
                "compte_supprime", numeroCompte,
                "transactions_supprimees", nbTxns
        );
    }

    // ─── Transactions ─────────────────────────────────────────────────────────

    @Transactional
    public Transaction depot(String numeroCompte, MontantRequest request) {
        Compte compte = consulterCompte(numeroCompte);
        compte.setSolde(compte.getSolde() + request.getMontant());
        compteRepository.save(compte);

        return transactionRepository.save(Transaction.builder()
                .type(TypeTransaction.depot)
                .montant(request.getMontant())
                .compteSource(numeroCompte)
                .build());
    }

    @Transactional
    public Transaction retrait(String numeroCompte, MontantRequest request) {
        Compte compte = consulterCompte(numeroCompte);
        if (compte.getSolde() < request.getMontant()) {
            throw new SoldeInsuffisantException();
        }
        compte.setSolde(compte.getSolde() - request.getMontant());
        compteRepository.save(compte);

        return transactionRepository.save(Transaction.builder()
                .type(TypeTransaction.retrait)
                .montant(request.getMontant())
                .compteSource(numeroCompte)
                .build());
    }

    @Transactional
    public Transaction virement(String numeroCompte, VirementRequest request) {
        if (numeroCompte.equals(request.getNumeroCompteDestination())) {
            throw new VirementInvalideException("Impossible de virer vers le même compte");
        }
        Compte source = consulterCompte(numeroCompte);
        Compte destination = compteRepository
                .findByNumeroCompte(request.getNumeroCompteDestination())
                .orElseThrow(() -> new CompteIntrouvableException("Compte destination introuvable"));

        if (source.getSolde() < request.getMontant()) {
            throw new SoldeInsuffisantException();
        }
        source.setSolde(source.getSolde() - request.getMontant());
        destination.setSolde(destination.getSolde() + request.getMontant());
        compteRepository.save(source);
        compteRepository.save(destination);

        return transactionRepository.save(Transaction.builder()
                .type(TypeTransaction.virement)
                .montant(request.getMontant())
                .compteSource(numeroCompte)
                .compteDestination(request.getNumeroCompteDestination())
                .build());
    }

    public List<Transaction> historiqueTransactions(String numeroCompte) {
        consulterCompte(numeroCompte); // vérifie l'existence
        return transactionRepository.findByCompte(numeroCompte);
    }
}
