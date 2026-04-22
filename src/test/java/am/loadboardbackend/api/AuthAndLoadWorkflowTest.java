package am.loadboardbackend.api;

import am.loadboardbackend.dto.auth.LoginRequest;
import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.MeResponse;
import am.loadboardbackend.dto.auth.RegisterAdminRequest;
import am.loadboardbackend.dto.auth.RegisterRequest;
import am.loadboardbackend.dto.document.DocumentUploadResponse;
import am.loadboardbackend.dto.load.BidResponse;
import am.loadboardbackend.dto.load.CreateBidRequest;
import am.loadboardbackend.dto.load.CreateLoadRequest;
import am.loadboardbackend.dto.load.LoadPostingDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthAndLoadWorkflowTest {

    @Autowired
    private WebApplicationContext wac;

    private final ObjectMapper om = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private MockMvc mvc;

    @BeforeEach
    void setupMvc() {
        // Apply the full Spring Security filter chain (including JwtAuthenticationFilter)
        // so Bearer tokens are resolved on every request.
        mvc = webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void brokerCreatesLoad_carrierBids_brokerApproves_brokerCancels() throws Exception {
        // Create initial admin (first admin doesn't require auth)
        RegisterAdminRequest adminReq = new RegisterAdminRequest();
        adminReq.setEmail("admin@example.com");
        adminReq.setPassword("password123");
        LoginResponse adminReg = read(mvc.perform(post("/api/auth/register-admin")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(adminReq)))
                .andExpect(status().isCreated()), LoginResponse.class);
        String adminToken = adminReg.getToken();

        // Register broker
        RegisterRequest regBroker = new RegisterRequest();
        regBroker.setEmail("broker1@example.com");
        regBroker.setPassword("password123");
        regBroker.setRole("BROKER");

        LoginResponse brokerReg = read(mvc.perform(post("/api/auth/register")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(regBroker)))
                .andExpect(status().isCreated()), LoginResponse.class);
        String brokerToken = brokerReg.getToken();
        assertThat(brokerReg.getUserId()).isNotBlank();
        assertThat(brokerReg.getRole()).isEqualTo("BROKER");

        // Approve broker
        mvc.perform(post("/api/admin/users/" + brokerReg.getUserId() + "/approve")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Broker creates load
        CreateLoadRequest loadReq = new CreateLoadRequest();
        loadReq.setPickupStreet("100 Logistics Way");
        loadReq.setPickupCity("Chicago");
        loadReq.setPickupState("IL");
        loadReq.setPickupZip("60601");
        loadReq.setPickupCountry("US");
        loadReq.setDropStreet("200 Freight Ave");
        loadReq.setDropCity("Los Angeles");
        loadReq.setDropState("CA");
        loadReq.setDropZip("90001");
        loadReq.setDropCountry("US");
        loadReq.setDescription("FTL - dry van");
        loadReq.setWeight(12000d);
        loadReq.setPrice(1500d);

        LoadPostingDto createdLoad = read(mvc.perform(post("/api/loads")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + brokerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(loadReq)))
                .andExpect(status().isOk()), LoadPostingDto.class);

        UUID loadId = createdLoad.getId();
        assertThat(loadId).isNotNull();

        // Register carrier
        RegisterRequest regCarrier = new RegisterRequest();
        regCarrier.setEmail("carrier1@example.com");
        regCarrier.setPassword("password123");
        regCarrier.setRole("CARRIER");

        LoginResponse carrierReg = read(mvc.perform(post("/api/auth/register")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(regCarrier)))
                .andExpect(status().isCreated()), LoginResponse.class);

        String carrierToken = carrierReg.getToken();
        assertThat(carrierReg.getRole()).isEqualTo("CARRIER");

        // Approve carrier
        mvc.perform(post("/api/admin/users/" + carrierReg.getUserId() + "/approve")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Carrier bids
        CreateBidRequest bidReq = new CreateBidRequest(loadId, new BigDecimal("1400"), true);
        BidResponse bid = read(mvc.perform(post("/api/loads/bid")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + carrierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(bidReq)))
                .andExpect(status().isOk()), BidResponse.class);

        UUID bidId = bid.id();
        assertThat(bid.status()).isEqualTo("PENDING");

        // Broker lists bids
        String bidsJson = mvc.perform(get("/api/loads/" + loadId + "/bids")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + brokerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        BidResponse[] bids = om.readValue(bidsJson, BidResponse[].class);
        assertThat(bids.length).isGreaterThanOrEqualTo(1);

        // Broker approves
        mvc.perform(post("/api/loads/" + loadId + "/approve/" + bidId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + brokerToken))
                .andExpect(status().isOk());

        // Broker cancels booking -> reopens
        mvc.perform(post("/api/loads/" + loadId + "/cancel")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + brokerToken))
                .andExpect(status().isOk());
    }

    @Test
    void login_returns_frontend_auth_shape() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("shape@example.com");
        reg.setPassword("password123");
        reg.setRole("BROKER");
        read(mvc.perform(post("/api/auth/register")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reg)))
                .andExpect(status().isCreated()), LoginResponse.class);

        LoginRequest login = new LoginRequest();
        login.setEmail("shape@example.com");
        login.setPassword("password123");

        LoginResponse resp = read(mvc.perform(post("/api/auth/login")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andExpect(status().isOk()), LoginResponse.class);

        assertThat(resp.getToken()).isNotBlank();
        assertThat(resp.getUserId()).isNotBlank();
        assertThat(resp.getEmail()).isEqualTo("shape@example.com");
        assertThat(resp.getRole()).isIn("BROKER", "CARRIER", "ADMIN");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEW TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testCarrierProfileCompletion() throws Exception {
        // Register carrier
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("carrier-profile@example.com");
        reg.setPassword("password123");
        reg.setRole("CARRIER");
        LoginResponse carrierReg = read(mvc.perform(post("/api/auth/register")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reg)))
                .andExpect(status().isCreated()), LoginResponse.class);
        String token = carrierReg.getToken();

        // Verify adminApproved is false in registration response
        assertThat(carrierReg.isAdminApproved()).isFalse();

        // PATCH /api/carriers/profile
        java.util.HashMap<String, Object> profileBody = new java.util.HashMap<>();
        profileBody.put("companyName", "Swift Haulers LLC");
        profileBody.put("dotNumber", "123456");
        profileBody.put("mcNumber", "MC-123456");
        profileBody.put("phoneNumber", "+14155552671");
        profileBody.put("insuranceCompany", "ABC Insurance Co.");
        profileBody.put("cargoInsurance", 100000);
        profileBody.put("liabilityInsurance", 1000000);
        profileBody.put("taxIdType", "EIN");
        profileBody.put("taxId", "12-3456789");
        profileBody.put("mailingAddress", "123 Main St");
        profileBody.put("city", "Dallas");
        profileBody.put("state", "TX");
        profileBody.put("zipCode", "75001");

        String patchResult = mvc.perform(patch("/api/carriers/profile")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(profileBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(patchResult).contains("Profile updated successfully");

        // GET /api/auth/me → profileComplete=true, adminApproved=false
        MeResponse me = read(mvc.perform(get("/api/auth/me")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()), MeResponse.class);

        assertThat(me.profileComplete()).isTrue();
        assertThat(me.adminApproved()).isFalse();
        assertThat(me.role()).isEqualTo("CARRIER");
    }

    @Test
    void testW9Upload() throws Exception {
        // Register carrier
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("carrier-w9@example.com");
        reg.setPassword("password123");
        reg.setRole("CARRIER");
        LoginResponse carrierReg = read(mvc.perform(post("/api/auth/register")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reg)))
                .andExpect(status().isCreated()), LoginResponse.class);
        String token = carrierReg.getToken();

        // Create a minimal mock PDF (just needs the right content-type / extension)
        byte[] pdfContent = "%PDF-1.4 mock content".getBytes();
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "w9_test.pdf", "application/pdf", pdfContent);

        // POST /api/carriers/documents/w9
        DocumentUploadResponse uploadResp = read(
                mvc.perform(multipart("/api/carriers/documents/w9")
                                .file(mockFile)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk()),
                DocumentUploadResponse.class);

        assertThat(uploadResp.fileId()).isNotBlank();
        assertThat(uploadResp.fileName()).isNotBlank();
        assertThat(uploadResp.fileUrl()).startsWith("/uploads/w9/");
    }

    @Test
    void testAdminApprovalGate() throws Exception {
        // Create admin
        RegisterAdminRequest adminReq = new RegisterAdminRequest();
        adminReq.setEmail("admin-gate@example.com");
        adminReq.setPassword("password123");
        LoginResponse adminReg = read(mvc.perform(post("/api/auth/register-admin")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(adminReq)))
                .andExpect(status().isCreated()), LoginResponse.class);
        String adminToken = adminReg.getToken();

        // Register carrier
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("carrier-gate@example.com");
        reg.setPassword("password123");
        reg.setRole("CARRIER");
        LoginResponse carrierReg = read(mvc.perform(post("/api/auth/register")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reg)))
                .andExpect(status().isCreated()), LoginResponse.class);
        String carrierToken = carrierReg.getToken();

        // Complete profile
        Map<String, Object> profileBody = Map.of(
                "companyName", "Gate Carrier LLC",
                "phoneNumber", "+14155550001",
                "insuranceCompany", "Gate Insurance",
                "cargoInsurance", 50000,
                "liabilityInsurance", 500000,
                "taxIdType", "EIN",
                "taxId", "99-9999999"
        );
        mvc.perform(patch("/api/carriers/profile")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + carrierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(profileBody)))
                .andExpect(status().isOk());

        // Try GET /api/loads — should get 403 (not approved)
        mvc.perform(get("/api/loads")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + carrierToken))
                .andExpect(status().isForbidden());

        // Admin approves
        mvc.perform(post("/api/admin/users/" + carrierReg.getUserId() + "/approve")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Login again to get fresh token reflecting approval
        LoginRequest login = new LoginRequest();
        login.setEmail("carrier-gate@example.com");
        login.setPassword("password123");
        LoginResponse freshLogin = read(mvc.perform(post("/api/auth/login")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andExpect(status().isOk()), LoginResponse.class);
        assertThat(freshLogin.isAdminApproved()).isTrue();

        // After approval: GET /api/loads should succeed (200)
        mvc.perform(get("/api/loads")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + freshLogin.getToken()))
                .andExpect(status().isOk());
    }

    private <T> T read(ResultActions ra, Class<T> clazz) {
        try {
            String json = ra.andReturn().getResponse().getContentAsString();
            if (json == null || json.isBlank()) return null;
            return om.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
