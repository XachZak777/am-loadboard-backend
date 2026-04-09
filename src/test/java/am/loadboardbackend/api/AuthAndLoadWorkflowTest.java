package am.loadboardbackend.api;

import am.loadboardbackend.dto.auth.LoginRequest;
import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.RegisterAdminRequest;
import am.loadboardbackend.dto.auth.RegisterRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
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

    private <T> T read(org.springframework.test.web.servlet.ResultActions ra, Class<T> clazz) {
        try {
            String json = ra.andReturn().getResponse().getContentAsString();
            if (json == null || json.isBlank()) return null;
            return om.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
