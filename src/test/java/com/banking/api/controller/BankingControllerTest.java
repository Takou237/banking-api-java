package com.banking.api.controller;

import com.banking.api.model.Compte;
import com.banking.api.model.Transaction;
import com.banking.api.service.BankingService;
import com.banking.api.dto.Requests.*;
import com.banking.api.exception.BankingExceptions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankingController.class)
@DisplayName("BankingController — Tests d'intégration HTTP")
class BankingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private BankingService bankingService;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Compte compteTest() {
        return Compte.builder()
                .id(UUID.randomUUID())
                .numeroCompte("BK-ALICE001")
                .nomTitulaire("Alice Dupont")
                .email("alice@example.com")
                .solde(0.0)
                .build();
    }

    private Transaction txnTest(Transaction.TypeTransaction type) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .type(type)
                .montant(500.0)
                .compteSource("BK-ALICE001")
                .build();
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /comptes
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /comptes")
    class PostComptes {

        @Test
        @DisplayName("retourne 201 et le compte créé")
        void creerCompte_201() throws Exception {
            var compte = compteTest();
            when(bankingService.creerCompte(any())).thenReturn(compte);

            mockMvc.perform(post("/comptes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("nomTitulaire", "Alice Dupont", "email", "alice@example.com"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.numeroCompte").value("BK-ALICE001"))
                    .andExpect(jsonPath("$.solde").value(0.0));
        }

        @Test
        @DisplayName("retourne 400 si email déjà utilisé")
        void creerCompte_emailDuplique_400() throws Exception {
            when(bankingService.creerCompte(any())).thenThrow(new EmailDejaUtiliseException());

            mockMvc.perform(post("/comptes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("nomTitulaire", "Bob", "email", "alice@example.com"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erreur").value("Email déjà utilisé"));
        }

        @Test
        @DisplayName("retourne 400 si corps incomplet")
        void creerCompte_corpsIncomplet_400() throws Exception {
            mockMvc.perform(post("/comptes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("nomTitulaire", "Bob"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET /comptes
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /comptes")
    class GetComptes {

        @Test
        @DisplayName("retourne 200 et la liste des comptes")
        void listerComptes_200() throws Exception {
            when(bankingService.listerComptes()).thenReturn(List.of(compteTest()));

            mockMvc.perform(get("/comptes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].numeroCompte").value("BK-ALICE001"));
        }

        @Test
        @DisplayName("retourne 200 et liste vide")
        void listerComptes_vide_200() throws Exception {
            when(bankingService.listerComptes()).thenReturn(List.of());

            mockMvc.perform(get("/comptes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET /comptes/:numeroCompte
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /comptes/{numeroCompte}")
    class GetCompte {

        @Test
        @DisplayName("retourne 200 et le compte")
        void consulterCompte_200() throws Exception {
            when(bankingService.consulterCompte("BK-ALICE001")).thenReturn(compteTest());

            mockMvc.perform(get("/comptes/BK-ALICE001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("alice@example.com"));
        }

        @Test
        @DisplayName("retourne 404 si compte introuvable")
        void consulterCompte_404() throws Exception {
            when(bankingService.consulterCompte("BK-FAKE"))
                    .thenThrow(new CompteIntrouvableException());

            mockMvc.perform(get("/comptes/BK-FAKE"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.erreur").value("Compte introuvable"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE /comptes/:numeroCompte
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /comptes/{numeroCompte}")
    class DeleteCompte {

        @Test
        @DisplayName("retourne 200 et le rapport de suppression")
        void supprimerCompte_200() throws Exception {
            when(bankingService.supprimerCompte("BK-ALICE001"))
                    .thenReturn(Map.of("succes", true, "compte_supprime", "BK-ALICE001", "transactions_supprimees", 2L));

            mockMvc.perform(delete("/comptes/BK-ALICE001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.succes").value(true));
        }

        @Test
        @DisplayName("retourne 404 si compte inexistant")
        void supprimerCompte_404() throws Exception {
            when(bankingService.supprimerCompte("BK-FAKE"))
                    .thenThrow(new CompteIntrouvableException());

            mockMvc.perform(delete("/comptes/BK-FAKE"))
                    .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /comptes/:numeroCompte/depot
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /comptes/{numeroCompte}/depot")
    class Depot {

        @Test
        @DisplayName("retourne 200 et la transaction de dépôt")
        void depot_200() throws Exception {
            when(bankingService.depot(eq("BK-ALICE001"), any()))
                    .thenReturn(txnTest(Transaction.TypeTransaction.depot));

            mockMvc.perform(post("/comptes/BK-ALICE001/depot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("montant", 500))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("depot"));
        }

        @Test
        @DisplayName("retourne 400 pour montant négatif")
        void depot_montantNegatif_400() throws Exception {
            mockMvc.perform(post("/comptes/BK-ALICE001/depot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("montant", -100))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("retourne 404 pour compte inexistant")
        void depot_compteInexistant_404() throws Exception {
            when(bankingService.depot(eq("BK-FAKE"), any()))
                    .thenThrow(new CompteIntrouvableException());

            mockMvc.perform(post("/comptes/BK-FAKE/depot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("montant", 100))))
                    .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /comptes/:numeroCompte/retrait
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /comptes/{numeroCompte}/retrait")
    class Retrait {

        @Test
        @DisplayName("retourne 200 et la transaction de retrait")
        void retrait_200() throws Exception {
            when(bankingService.retrait(eq("BK-ALICE001"), any()))
                    .thenReturn(txnTest(Transaction.TypeTransaction.retrait));

            mockMvc.perform(post("/comptes/BK-ALICE001/retrait")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("montant", 200))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("retrait"));
        }

        @Test
        @DisplayName("retourne 400 pour solde insuffisant")
        void retrait_soldeInsuffisant_400() throws Exception {
            when(bankingService.retrait(eq("BK-ALICE001"), any()))
                    .thenThrow(new SoldeInsuffisantException());

            mockMvc.perform(post("/comptes/BK-ALICE001/retrait")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("montant", 9999))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erreur").value("Solde insuffisant"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /comptes/:numeroCompte/virement
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /comptes/{numeroCompte}/virement")
    class Virement {

        @Test
        @DisplayName("retourne 200 et la transaction de virement")
        void virement_200() throws Exception {
            var txn = txnTest(Transaction.TypeTransaction.virement);
            txn.setCompteDestination("BK-BOB002");
            when(bankingService.virement(eq("BK-ALICE001"), any())).thenReturn(txn);

            mockMvc.perform(post("/comptes/BK-ALICE001/virement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("numeroCompteDestination", "BK-BOB002", "montant", 300))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("virement"))
                    .andExpect(jsonPath("$.compteDestination").value("BK-BOB002"));
        }

        @Test
        @DisplayName("retourne 400 pour virement vers soi-même")
        void virement_memeCompte_400() throws Exception {
            when(bankingService.virement(eq("BK-ALICE001"), any()))
                    .thenThrow(new VirementInvalideException("Impossible de virer vers le même compte"));

            mockMvc.perform(post("/comptes/BK-ALICE001/virement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("numeroCompteDestination", "BK-ALICE001", "montant", 100))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erreur").value("Impossible de virer vers le même compte"));
        }

        @Test
        @DisplayName("retourne 404 si destination introuvable")
        void virement_destinationIntrouvable_404() throws Exception {
            when(bankingService.virement(eq("BK-ALICE001"), any()))
                    .thenThrow(new CompteIntrouvableException("Compte destination introuvable"));

            mockMvc.perform(post("/comptes/BK-ALICE001/virement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("numeroCompteDestination", "BK-FAKE", "montant", 100))))
                    .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET /comptes/:numeroCompte/transactions
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /comptes/{numeroCompte}/transactions")
    class Historique {

        @Test
        @DisplayName("retourne 200 et la liste des transactions")
        void historique_200() throws Exception {
            when(bankingService.historiqueTransactions("BK-ALICE001"))
                    .thenReturn(List.of(
                            txnTest(Transaction.TypeTransaction.depot),
                            txnTest(Transaction.TypeTransaction.retrait)
                    ));

            mockMvc.perform(get("/comptes/BK-ALICE001/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("retourne 404 pour compte inconnu")
        void historique_404() throws Exception {
            when(bankingService.historiqueTransactions("BK-FAKE"))
                    .thenThrow(new CompteIntrouvableException());

            mockMvc.perform(get("/comptes/BK-FAKE/transactions"))
                    .andExpect(status().isNotFound());
        }
    }
}
