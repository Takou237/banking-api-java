package com.banking.api.service;

import com.banking.api.dto.Requests.*;
import com.banking.api.exception.BankingExceptions.*;
import com.banking.api.model.Banque;
import com.banking.api.model.Compte;
import com.banking.api.model.Transaction;
import com.banking.api.repository.BanqueRepository;
import com.banking.api.repository.CompteRepository;
import com.banking.api.repository.TransactionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankingService — Tests unitaires")
class BankingServiceTest {

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BanqueRepository banqueRepository;

    @InjectMocks
    private BankingService service;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Banque banqueTest() {
        return Banque.builder()
                .id(UUID.randomUUID())
                .nom("Banque Test")
                .code("BQ-TEST01")
                .build();
    }

    private Compte compteTest(String numero, double solde) {
        return Compte.builder()
                .id(UUID.randomUUID())
                .numeroCompte(numero)
                .nomTitulaire("Alice Dupont")
                .email("alice@example.com")
                .solde(solde)
                .banque(banqueTest())
                .build();
    }

    private Transaction txnTest(Transaction.TypeTransaction type) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .type(type)
                .montant(100.0)
                .compteSource("BK-ALICE001")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // creerCompte()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("creerCompte()")
    class CreerCompteTests {

        @Test
        @DisplayName("crée et sauvegarde un compte avec les bonnes propriétés")
        void creerCompte_succes() {
            var banque = banqueTest();
            var request = new CreerCompteRequest("Alice Dupont", "alice@example.com", banque.getId());
            var compteAttendu = compteTest("BK-ALICE001", 0.0);

            when(compteRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(banqueRepository.findById(banque.getId())).thenReturn(Optional.of(banque));
            when(compteRepository.save(any(Compte.class))).thenReturn(compteAttendu);

            Compte result = service.creerCompte(request);

            assertThat(result.getNomTitulaire()).isEqualTo("Alice Dupont");
            assertThat(result.getEmail()).isEqualTo("alice@example.com");
            assertThat(result.getSolde()).isEqualTo(0.0);
            verify(compteRepository).save(any(Compte.class));
        }

        @Test
        @DisplayName("lève EmailDejaUtiliseException si l'email existe déjà")
        void creerCompte_emailDuplique() {
            var request = new CreerCompteRequest("Bob Martin", "alice@example.com", UUID.randomUUID());
            when(compteRepository.existsByEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> service.creerCompte(request))
                    .isInstanceOf(EmailDejaUtiliseException.class)
                    .hasMessageContaining("Email déjà utilisé");

            verify(compteRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // consulterCompte()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("consulterCompte()")
    class ConsulterCompteTests {

        @Test
        @DisplayName("retourne le compte pour un numéro valide")
        void consulterCompte_succes() {
            var compte = compteTest("BK-ALICE001", 500.0);
            when(compteRepository.findByNumeroCompte("BK-ALICE001"))
                    .thenReturn(Optional.of(compte));

            Compte result = service.consulterCompte("BK-ALICE001");

            assertThat(result).isEqualTo(compte);
        }

        @Test
        @DisplayName("lève CompteIntrouvableException pour un numéro inconnu")
        void consulterCompte_introuvable() {
            when(compteRepository.findByNumeroCompte("BK-FAKE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consulterCompte("BK-FAKE"))
                    .isInstanceOf(CompteIntrouvableException.class)
                    .hasMessageContaining("Compte introuvable");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // listerComptes()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listerComptes()")
    class ListerComptesTests {

        @Test
        @DisplayName("retourne tous les comptes")
        void listerComptes_succes() {
            var comptes = List.of(
                    compteTest("BK-ALICE001", 100.0),
                    compteTest("BK-BOB002", 200.0)
            );
            when(compteRepository.findAll()).thenReturn(comptes);

            List<Compte> result = service.listerComptes();

            assertThat(result).hasSize(2);
            verify(compteRepository).findAll();
        }

        @Test
        @DisplayName("retourne une liste vide si aucun compte")
        void listerComptes_vide() {
            when(compteRepository.findAll()).thenReturn(List.of());

            assertThat(service.listerComptes()).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // supprimerCompte()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("supprimerCompte()")
    class SupprimerCompteTests {

        @Test
        @DisplayName("supprime le compte et ses transactions")
        void supprimerCompte_succes() {
            var compte = compteTest("BK-ALICE001", 0.0);
            when(compteRepository.findByNumeroCompte("BK-ALICE001"))
                    .thenReturn(Optional.of(compte));
            when(transactionRepository.countByCompteSourceOrCompteDestination(
                    "BK-ALICE001", "BK-ALICE001")).thenReturn(3L);

            var result = service.supprimerCompte("BK-ALICE001");

            assertThat(result.get("succes")).isEqualTo(true);
            assertThat(result.get("transactions_supprimees")).isEqualTo(3L);
            verify(compteRepository).delete(compte);
            verify(transactionRepository).deleteByCompte("BK-ALICE001");
        }

        @Test
        @DisplayName("lève CompteIntrouvableException si le compte n'existe pas")
        void supprimerCompte_introuvable() {
            when(compteRepository.findByNumeroCompte("BK-FAKE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.supprimerCompte("BK-FAKE"))
                    .isInstanceOf(CompteIntrouvableException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // depot()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("depot()")
    class DepotTests {

        @Test
        @DisplayName("crédite le solde et retourne la transaction")
        void depot_succes() {
            var compte = compteTest("BK-ALICE001", 100.0);
            var txn = txnTest(Transaction.TypeTransaction.depot);

            when(compteRepository.findByNumeroCompte("BK-ALICE001"))
                    .thenReturn(Optional.of(compte));
            when(compteRepository.save(any())).thenReturn(compte);
            when(transactionRepository.save(any())).thenReturn(txn);

            var request = new MontantRequest(200.0);
            Transaction result = service.depot("BK-ALICE001", request);

            assertThat(compte.getSolde()).isEqualTo(300.0);
            assertThat(result.getType()).isEqualTo(Transaction.TypeTransaction.depot);
            verify(compteRepository).save(compte);
        }

        @Test
        @DisplayName("lève CompteIntrouvableException pour un compte inconnu")
        void depot_compteInconnu() {
            when(compteRepository.findByNumeroCompte("BK-FAKE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.depot("BK-FAKE", new MontantRequest(100.0)))
                    .isInstanceOf(CompteIntrouvableException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // retrait()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("retrait()")
    class RetraitTests {

        @Test
        @DisplayName("débite le solde et retourne la transaction")
        void retrait_succes() {
            var compte = compteTest("BK-ALICE001", 500.0);
            var txn = txnTest(Transaction.TypeTransaction.retrait);

            when(compteRepository.findByNumeroCompte("BK-ALICE001"))
                    .thenReturn(Optional.of(compte));
            when(compteRepository.save(any())).thenReturn(compte);
            when(transactionRepository.save(any())).thenReturn(txn);

            service.retrait("BK-ALICE001", new MontantRequest(200.0));

            assertThat(compte.getSolde()).isEqualTo(300.0);
        }

        @Test
        @DisplayName("lève SoldeInsuffisantException si solde < montant")
        void retrait_soldeInsuffisant() {
            var compte = compteTest("BK-ALICE001", 50.0);
            when(compteRepository.findByNumeroCompte("BK-ALICE001"))
                    .thenReturn(Optional.of(compte));

            assertThatThrownBy(() -> service.retrait("BK-ALICE001", new MontantRequest(200.0)))
                    .isInstanceOf(SoldeInsuffisantException.class)
                    .hasMessageContaining("Solde insuffisant");
        }

        @Test
        @DisplayName("n'effectue pas de sauvegarde si solde insuffisant")
        void retrait_soldeInsuffisant_pasSauvegarde() {
            var compte = compteTest("BK-ALICE001", 50.0);
            when(compteRepository.findByNumeroCompte("BK-ALICE001"))
                    .thenReturn(Optional.of(compte));

            assertThatThrownBy(() -> service.retrait("BK-ALICE001", new MontantRequest(200.0)))
                    .isInstanceOf(SoldeInsuffisantException.class);

            verify(compteRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // virement()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("virement()")
    class VirementTests {

        @Test
        @DisplayName("transfère le montant entre les deux comptes")
        void virement_succes() {
            var source = compteTest("BK-ALICE001", 1000.0);
            var dest   = compteTest("BK-BOB002",   0.0);
            dest.setEmail("bob@example.com");

            when(compteRepository.findByNumeroCompte("BK-ALICE001"))
                    .thenReturn(Optional.of(source));
            when(compteRepository.findByNumeroCompte("BK-BOB002"))
                    .thenReturn(Optional.of(dest));
            when(compteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any()))
                    .thenReturn(txnTest(Transaction.TypeTransaction.virement));

            service.virement("BK-ALICE001",
                    new VirementRequest("BK-BOB002", 300.0));

            assertThat(source.getSolde()).isEqualTo(700.0);
            assertThat(dest.getSolde()).isEqualTo(300.0);
            verify(compteRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("lève VirementInvalideException pour un virement vers soi-même")
        void virement_memeCompte() {
            assertThatThrownBy(() ->
                    service.virement("BK-ALICE001",
                            new VirementRequest("BK-ALICE001", 100.0)))
                    .isInstanceOf(VirementInvalideException.class)
                    .hasMessageContaining("même compte");
        }

        @Test
        @DisplayName("lève SoldeInsuffisantException si solde insuffisant")
        void virement_soldeInsuffisant() {
            var source = compteTest("BK-ALICE001", 50.0);
            var dest   = compteTest("BK-BOB002",   0.0);

            when(compteRepository.findByNumeroCompte("BK-ALICE001"))
                    .thenReturn(Optional.of(source));
            when(compteRepository.findByNumeroCompte("BK-BOB002"))
                    .thenReturn(Optional.of(dest));

            assertThatThrownBy(() ->
                    service.virement("BK-ALICE001",
                            new VirementRequest("BK-BOB002", 9999.0)))
                    .isInstanceOf(SoldeInsuffisantException.class);
        }

        @Test
        @DisplayName("lève CompteIntrouvableException si destination inconnue")
        void virement_destinationInconnue() {
            var source = compteTest("BK-ALICE001", 1000.0);
            when(compteRepository.findByNumeroCompte("BK-ALICE001"))
                    .thenReturn(Optional.of(source));
            when(compteRepository.findByNumeroCompte("BK-FAKE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.virement("BK-ALICE001",
                            new VirementRequest("BK-FAKE", 100.0)))
                    .isInstanceOf(CompteIntrouvableException.class)
                    .hasMessageContaining("destination");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // historiqueTransactions()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("historiqueTransactions()")
    class HistoriqueTests {

        @Test
        @DisplayName("retourne les transactions du compte")
        void historique_succes() {
            var compte = compteTest("BK-ALICE001", 0.0);
            var txns = List.of(
                    txnTest(Transaction.TypeTransaction.depot),
                    txnTest(Transaction.TypeTransaction.retrait)
            );
            when(compteRepository.findByNumeroCompte("BK-ALICE001"))
                    .thenReturn(Optional.of(compte));
            when(transactionRepository.findByCompte("BK-ALICE001"))
                    .thenReturn(txns);

            List<Transaction> result = service.historiqueTransactions("BK-ALICE001");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("lève CompteIntrouvableException pour un compte inconnu")
        void historique_compteInconnu() {
            when(compteRepository.findByNumeroCompte("BK-FAKE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.historiqueTransactions("BK-FAKE"))
                    .isInstanceOf(CompteIntrouvableException.class);
        }
    }
}
