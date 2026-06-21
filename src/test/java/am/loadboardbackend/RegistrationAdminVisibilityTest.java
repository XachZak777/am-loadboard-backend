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
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that all profile fields entered during carrier/broker registration
 * are visible to admin after the two-step wizard completes:
 *   1. POST /api/auth/register        — creates stub user + PENDING-xxx placeholders
 *   2. PATCH /api/carriers/profile    — fills in real profile fields
 *   3. GET  /api/admin/users/pending  — admin sees complete profile (no missing fields)
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SuppressWarnings("unchecked")
class RegistrationAdminVisibilityTest {

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

    // ── Carrier ──────────────────────────────────────────────────────────────

    @Test
    void carrierProfileFieldsVisibleToAdmin() throws Exception {
        String email = "carrier-" + UUID.randomUUID() + "@test.com";

        // Step 1 — minimal registration (creates stub with PENDING-xxx DOT/MC)
        String token = registerUser(email, "Test1234!", "CARRIER");

        // Step 2 — wizard profile PATCH (replaces PENDING-xxx with real data)
        patchCarrierProfile(token, Map.ofEntries(
                entry("companyName",      "Acme Carriers LLC"),
                entry("dbaName",          "Acme"),
                entry("dotNumber",        "DOT-123456"),
                entry("mcNumber",         "MC-654321"),
                entry("phoneNumber",      "5551234567"),
                entry("mailingAddress",   "123 Main St"),
                entry("city",             "Los Angeles"),
                entry("state",            "CA"),
                entry("zipCode",          "90001"),
                entry("insuranceCompany", "SafeDrive Insurance"),
                entry("taxIdType",        "EIN"),
                entry("taxId",            "12-3456789")
        ));

        // Step 3 — admin checks pending list
        Map<?, ?> user = findUserByEmail(email, "/api/admin/users/pending");

        assertThat(user.get("companyName")).isEqualTo("Acme Carriers LLC");
        assertThat(user.get("dbaName")).isEqualTo("Acme");
        assertThat(user.get("dotNumber")).isEqualTo("DOT-123456");
        assertThat(user.get("mcNumber")).isEqualTo("MC-654321");
        assertThat(user.get("phoneNumber")).isEqualTo("5551234567");
        assertThat(user.get("mailingAddress")).isEqualTo("123 Main St");
        assertThat(user.get("city")).isEqualTo("Los Angeles");
        assertThat(user.get("state")).isEqualTo("CA");
        assertThat(user.get("zipCode")).isEqualTo("90001");
        assertThat(user.get("insuranceCompany")).isEqualTo("SafeDrive Insurance");
        assertThat(user.get("taxIdType")).isEqualTo("EIN");
        assertThat(user.get("taxId")).isEqualTo("12-3456789");
        assertThat(user.get("profileId")).isNotNull();
        assertThat(user.get("role")).isEqualTo("CARRIER");
    }

    @Test
    void carrierWithNoProfilePatchStillAppearsInAdminList() throws Exception {
        // Registers but never calls PATCH — admin must still see the row (with null fields),
        // not get an error or silently omit the user.
        String email = "no-profile-" + UUID.randomUUID() + "@test.com";
        registerUser(email, "Test1234!", "CARRIER");

        List<?> pending = getPendingUsers();
        boolean found = pending.stream()
                .map(u -> ((Map<?, ?>) u).get("email"))
                .anyMatch(email::equals);
        assertThat(found)
                .as("Carrier with no profile PATCH must still appear in admin pending list")
                .isTrue();
    }

    @Test
    void pendingCarrierDotAndMcAreNullBeforeProfilePatch() throws Exception {
        // Before PATCH, PENDING-xxx placeholder values must be stripped to null —
        // the admin must not see internal placeholder strings.
        String email = "stub-" + UUID.randomUUID() + "@test.com";
        registerUser(email, "Test1234!", "CARRIER");

        Map<?, ?> user = findUserByEmail(email, "/api/admin/users/pending");
        assertThat(user.get("dotNumber")).isNull();
        assertThat(user.get("mcNumber")).isNull();
    }

    // ── Broker ───────────────────────────────────────────────────────────────

    @Test
    void brokerProfileFieldsVisibleToAdmin() throws Exception {
        String email = "broker-" + UUID.randomUUID() + "@test.com";

        String token = registerUser(email, "Test1234!", "BROKER");
        patchBrokerProfile(token, Map.of(
                "companyName",    "Fast Brokers Inc",
                "mcNumber",       "MC-999888",
                "phoneNumber",    "5559876543",
                "mailingAddress", "456 Oak Ave",
                "city",           "Chicago",
                "state",          "IL",
                "zipCode",        "60601"
        ));

        Map<?, ?> user = findUserByEmail(email, "/api/admin/users/pending");

        assertThat(user.get("companyName")).isEqualTo("Fast Brokers Inc");
        assertThat(user.get("mcNumber")).isEqualTo("MC-999888");
        assertThat(user.get("phoneNumber")).isEqualTo("5559876543");
        assertThat(user.get("city")).isEqualTo("Chicago");
        assertThat(user.get("state")).isEqualTo("IL");
        assertThat(user.get("role")).isEqualTo("BROKER");
        assertThat(user.get("profileId")).isNotNull();
    }

    @Test
    void adminGetUserByIdReturnsCorrectCarrierProfile() throws Exception {
        String email = "byid-" + UUID.randomUUID() + "@test.com";
        String token = registerUser(email, "Test1234!", "CARRIER");
        patchCarrierProfile(token, Map.of(
                "companyName", "ById Carrier LLC",
                "dotNumber",   "DOT-BYID",
                "mcNumber",    "MC-BYID",
                "city",        "Houston",
                "state",       "TX",
                "zipCode",     "77001"
        ));

        String userId = (String) findUserByEmail(email, "/api/admin/users/pending").get("userId");
        assertThat(userId).isNotNull();

        MvcResult result = mvc.perform(get("/api/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> detail = json.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(detail.get("email")).isEqualTo(email);
        assertThat(detail.get("companyName")).isEqualTo("ById Carrier LLC");
        assertThat(detail.get("dotNumber")).isEqualTo("DOT-BYID");
        assertThat(detail.get("city")).isEqualTo("Houston");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private void patchBrokerProfile(String token, Map<?, ?> profile) throws Exception {
        mvc.perform(patch("/api/brokers/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(profile)))
                .andExpect(status().isOk());
    }

    private List<?> getPendingUsers() throws Exception {
        MvcResult r = mvc.perform(get("/api/admin/users/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return json.readValue(r.getResponse().getContentAsString(), List.class);
    }

    private Map<?, ?> findUserByEmail(String email, String path) throws Exception {
        MvcResult r = mvc.perform(get(path)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<?, ?>> users = json.readValue(r.getResponse().getContentAsString(), List.class);
        return users.stream()
                .filter(u -> email.equals(u.get("email")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("User " + email + " not found in " + path));
    }
}
