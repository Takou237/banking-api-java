package com.banking.api.controller;

import com.banking.api.dto.Requests.*;
import com.banking.api.model.Banque;
import com.banking.api.model.Compte;
import com.banking.api.service.BankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/banques")
@RequiredArgsConstructor
@Tag(name = "Banques", description = "Gestion des banques")
public class BanqueController {

    private final BankingService bankingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une banque")
    public Banque creerBanque(@Valid @RequestBody CreerBanqueRequest request) {
        return bankingService.creerBanque(request);
    }

    @GetMapping
    @Operation(summary = "Lister toutes les banques")
    public List<Banque> listerBanques() {
        return bankingService.listerBanques();
    }

    @GetMapping("/{banqueId}")
    @Operation(summary = "Consulter une banque")
    public Banque consulterBanque(@PathVariable UUID banqueId) {
        return bankingService.consulterBanque(banqueId);
    }

    @GetMapping("/{banqueId}/comptes")
    @Operation(summary = "Lister les comptes d'une banque")
    public List<Compte> listerComptesDeLaBanque(@PathVariable UUID banqueId) {
        return bankingService.listerComptesParBanque(banqueId);
    }

    @DeleteMapping("/{banqueId}")
    @Operation(summary = "Supprimer une banque (si elle n'a aucun compte)")
    public Map<String, Object> supprimerBanque(@PathVariable UUID banqueId) {
        return bankingService.supprimerBanque(banqueId);
    }
}
