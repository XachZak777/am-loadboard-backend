package am.loadboardbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StopWatch;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Performance and concurrency tests for the registration → admin visibility flow.
 *
 * Tests:
 *   1. GET /api/admin/users with 50 seeded users must respond in < 2 s.
 *   2. Full register + profile PATCH round-trip must complete in < 3 s.
 *   3. 10 concurrent registrations all succeed and appear in admin pending list
 *      with correct profile data (no lost writes, no race conditions).
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SuppressWarnings("unchecked")
class AdminUsersPerfTest {

    private static final int  SEED_COUNT          = 50;
    private static final long ADMIN_QUERY_MAX_MS  = 2_000;
    private static final long SINGLE_FLOW_MAX_MS  = 3_000;
    private static final int  CONCURRENT_USERS    = 10;

    @Autowired WebApplicationContext wac;

    final ObjectMapper json = new ObjectMapper();
    MockMvc mvc;
    String adminToken;

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        adminToken = loginAdmin();
    }

    // ── 1. Bulk admin query ───────────────────────────────────────────────────

    @Test
    void adminUsersQueryUnder2sWithFiftyUsers() throws Exception {
        seedCarriers(SEED_COUNT);

        StopWatch sw = new StopWatch();
        sw.start();
        MvcResult r = mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        sw.stop();

        List<?> users = json.readValue(r.getResponse().getContentAsString(), List.class);
        assertThat(users).hasSizeGreaterThanOrEqualTo(SEED_COUNT);
        assertThat(sw.getTotalTimeMillis())
                .as("GET /api/admin/users with %d users took %d ms (limit %d ms)",
                        SEED_COUNT, sw.getTotalTimeMillis(), ADMIN_QUERY_MAX_MS)
                .isLessThan(ADMIN_QUERY_MAX_MS);
    }

    // ── 2. Single round-trip timing ───────────────────────────────────────────

    @Test
    void singleCarrierRegistrationAndPatchUnder3s() throws Exception {
        String email = "perf-" + UUID.randomUUID() + "@test.com";

        StopWatch sw = new StopWatch();

        sw.start("register");
        String token = registerUser(email, "Test1234!", "CARRIER");
        sw.stop();

        sw.start("patch");
        patchCarrierProfile(token, sampleCarrierProfile());
        sw.stop();

        long total = sw.getTotalTimeMillis();
        assertThat(total)
                .as("Register + profile PATCH took %d ms (limit %d ms)", total, SINGLE_FLOW_MAX_MS)
                .isLessThan(SINGLE_FLOW_MAX_MS);
    }

    // ── 3. Concurrent registrations ───────────────────────────────────────────

    @Test
    void concurrentCarrierRegistrationsAllAppearInAdminWithCorrectProfile() throws Exception {
        List<String> emails = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            emails.add("concurrent-" + UUID.randomUUID() + "@test.com");
        }

        // Run all register + patch flows in parallel
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_USERS);
        List<CompletableFuture<Void>> futures = emails.stream()
                .map(email -> CompletableFuture.runAsync(() -> {
                    try {
                        String token = registerUser(email, "Test1234!", "CARRIER");
                        patchCarrierProfile(token, sampleCarrierProfile());
                    } catch (Exception e) {
                        throw new RuntimeException("Registration failed for " + email, e);
                    }
                }, pool))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        pool.shutdown();

        // All concurrent registrants must appear in pending list
        MvcResult r = mvc.perform(get("/api/admin/users/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<?, ?>> pending = json.readValue(r.getResponse().getContentAsString(), List.class);
        List<String> pendingEmails = pending.stream()
                .map(u -> (String) u.get("email"))
                .toList();

        for (String email : emails) {
            assertThat(pendingEmails)
                    .as("Concurrent registrant %s must appear in admin pending list", email)
                    .contains(email);

            Map<?, ?> user = pending.stream()
                    .filter(u -> email.equals(u.get("email")))
                    .findFirst()
                    .orElseThrow();

            assertThat(user.get("profileId"))
                    .as("profileId must not be null for %s — profile PATCH did not persist", email)
                    .isNotNull();
            assertThat(user.get("companyName"))
                    .as("companyName must be populated for %s after profile PATCH", email)
                    .isEqualTo("Perf Test Carrier LLC");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void seedCarriers(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            String email = "seed-" + UUID.randomUUID() + "@test.com";
            String token = registerUser(email, "Test1234!", "CARRIER");
            patchCarrierProfile(token, sampleCarrierProfile());
        }
    }

    private Map<String, Object> sampleCarrierProfile() {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return Map.of(
                "companyName",    "Perf Test Carrier LLC",
                "dotNumber",      "DOT-" + uid,
                "mcNumber",       "MC-" + uid,
                "phoneNumber",    "5551234567",
                "mailingAddress", "1 Speed Ln",
                "city",           "Dallas",
                "state",          "TX",
                "zipCode",        "75201",
                "taxIdType",      "EIN",
                "taxId",          "99-" + uid
        );
    }

    private String loginAdmin() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", "admin@haulius.com",
                                "password", "Test1234!"))))
                .andExpect(status().isOk())
                .andReturn();
        return (String) json.readValue(r.getResponse().getContentAsString(), Map.class).get("token");
    }

    private String registerUser(String email, String password, String role) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", email,
                                "password", password,
                                "role", role))))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        return (String) json.readValue(r.getResponse().getContentAsString(), Map.class).get("token");
    }

    private void patchCarrierProfile(String token, Map<?, ?> profile) throws Exception {
        mvc.perform(patch("/api/carriers/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(profile)))
                .andExpect(status().isOk());
    }
}
