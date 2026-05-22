package com.scrolldoom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrolldoom.dto.CreateBreachRequest;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.BreachEventRepository;
import com.scrolldoom.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BreachControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BreachEventRepository breachEventRepository;

    private static final String TEST_UID = "test-breach-user-uid";
    private ObjectId testUserId;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .firebaseUid(TEST_UID)
                .displayName("Breach Tester")
                .email("breach@test.com")
                .createdAt(new Date())
                .lastActiveAt(new Date())
                .build();
        user = userRepository.save(user);
        testUserId = user.getId();
    }

    @AfterEach
    void tearDown() {
        breachEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testReportBreach_Success() throws Exception {
        CreateBreachRequest req = new CreateBreachRequest();
        req.setPackageName("com.test.app");
        req.setAppLabel("Test App");
        req.setLimitMinutes(60);
        req.setActualMinutes(90);

        mockMvc.perform(post("/api/v1/breaches")
                        .header("X-Test-UserId", TEST_UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.packageName").value("com.test.app"))
                .andExpect(jsonPath("$.appLabel").value("Test App"))
                .andExpect(jsonPath("$.limitMinutes").value(60))
                .andExpect(jsonPath("$.actualMinutes").value(90))
                .andExpect(jsonPath("$.partnerNotified").value(false))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void testReportBreach_Deduplication() throws Exception {
        CreateBreachRequest req = new CreateBreachRequest();
        req.setPackageName("com.test.app");
        req.setAppLabel("Test App");
        req.setLimitMinutes(60);
        req.setActualMinutes(90);

        mockMvc.perform(post("/api/v1/breaches")
                        .header("X-Test-UserId", TEST_UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/breaches")
                        .header("X-Test-UserId", TEST_UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.packageName").value("com.test.app"))
                .andExpect(jsonPath("$.actualMinutes").value(90));
    }

    @Test
    void testReportBreach_NoAuth() throws Exception {
        CreateBreachRequest req = new CreateBreachRequest();
        req.setPackageName("com.test.app");
        req.setAppLabel("Test App");
        req.setLimitMinutes(60);
        req.setActualMinutes(90);

        mockMvc.perform(post("/api/v1/breaches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetMyBreaches_Empty() throws Exception {
        mockMvc.perform(get("/api/v1/breaches/me")
                        .header("X-Test-UserId", TEST_UID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
