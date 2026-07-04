package com.banking.api.controller;

import com.banking.api.dto.Requests.*;
import com.banking.api.model.Compte;
import com.banking.api.model.Transaction;
import com.banking.api.service.BankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comptes")
@RequiredArgsConstructor
@Tag(name = "Comptes & Transactions", description = "API Bancaire")
public class BankingController {

    private final BankingService bankingService;

    // ─── Comptes ──────────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte")
    public Compte creerCompte(@Valid @RequestBody CreerCompteRequest request) {
        return bankingService.creerCompte(request);
    }

    @GetMapping
    @Operation(summary = "Lister tous les comptes")
    public List<Compte> listerComptes() {
        return bankingService.listerComptes();
    }

    @GetMapping("/{numeroCompte}")
    @Operation(summary = "Consulter un compte")
    public Compte consulterCompte(@PathVariable String numeroCompte) {
        return bankingService.consulterCompte(numeroCompte);
    }

    @DeleteMapping("/{numeroCompte}")
    @Operation(summary = "Supprimer un compte")
    public Map<String, Object> supprimerCompte(@PathVariable String numeroCompte) {
        return bankingService.supprimerCompte(numeroCompte);
    }

    // ─── Transactions ─────────────────────────────────────────────────────────

    @PostMapping("/{numeroCompte}/depot")
    @Operation(summary = "Effectuer un dépôt")
    public Transaction depot(
            @PathVariable String numeroCompte,
            @Valid @RequestBody MontantRequest request) {
        return bankingService.depot(numeroCompte, request);
    }

    @PostMapping("/{numeroCompte}/retrait")
    @Operation(summary = "Effectuer un retrait")
    public Transaction retrait(
            @PathVariable String numeroCompte,
            @Valid @RequestBody MontantRequest request) {
        return bankingService.retrait(numeroCompte, request);
    }

    @PostMapping("/{numeroCompte}/virement")
    @Operation(summary = "Effectuer un virement")
    public Transaction virement(
            @PathVariable String numeroCompte,
            @Valid @RequestBody VirementRequest request) {
        return bankingService.virement(numeroCompte, request);
    }

    @GetMapping("/{numeroCompte}/transactions")
    @Operation(summary = "Historique des transactions")
    public List<Transaction> historiqueTransactions(@PathVariable String numeroCompte) {
        return bankingService.historiqueTransactions(numeroCompte);
    }

    // ─── Santé ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Vérification de santé")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
