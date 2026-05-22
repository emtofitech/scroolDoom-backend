package com.scrolldoom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrolldoom.dto.AcceptInviteRequest;
import com.scrolldoom.model.Partnership;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.PartnershipRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PartnershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PartnershipRepository partnershipRepository;

    private static final String USER_A_UID = "test-partner-a-uid";
    private static final String USER_B_UID = "test-partner-b-uid";
    private ObjectId userAId;
    private ObjectId userBId;

    @BeforeEach
    void setUp() {
        User userA = userRepository.save(User.builder()
                .firebaseUid(USER_A_UID)
                .displayName("User A")
                .email("userA@test.com")
                .createdAt(new Date())
                .lastActiveAt(new Date())
                .build());
        userAId = userA.getId();

        User userB = userRepository.save(User.builder()
                .firebaseUid(USER_B_UID)
                .displayName("User B")
                .email("userB@test.com")
                .createdAt(new Date())
                .lastActiveAt(new Date())
                .build());
        userBId = userB.getId();
    }

    @AfterEach
    void tearDown() {
        partnershipRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testGenerateInvite_Success() throws Exception {
        mockMvc.perform(post("/api/v1/partnerships/invite")
                        .header("X-Test-UserId", USER_A_UID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.inviteCode").isNotEmpty())
                .andExpect(jsonPath("$.inviteCode").isString());
    }

    @Test
    void testAcceptInvite_Success() throws Exception {
        String inviteCode = mockMvc.perform(post("/api/v1/partnerships/invite")
                        .header("X-Test-UserId", USER_A_UID))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String code = extractJsonValue(inviteCode, "inviteCode");

        AcceptInviteRequest req = new AcceptInviteRequest();
        req.setInviteCode(code);

        mockMvc.perform(post("/api/v1/partnerships/accept")
                        .header("X-Test-UserId", USER_B_UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.inviteCode").value(code))
                .andExpect(jsonPath("$.partner").isNotEmpty())
                .andExpect(jsonPath("$.partner.firebaseUid").value(USER_A_UID));
    }

    @Test
    void testAcceptInvite_ExpiredCode() throws Exception {
        Partnership expired = Partnership.builder()
                .senderUserId(userAId)
                .status("pending")
                .inviteCode("EXPIRED")
                .inviteExpiresAt(new Date(System.currentTimeMillis() - 1000))
                .createdAt(new Date(System.currentTimeMillis() - 86400000))
                .build();
        partnershipRepository.save(expired);

        AcceptInviteRequest req = new AcceptInviteRequest();
        req.setInviteCode("EXPIRED");

        mockMvc.perform(post("/api/v1/partnerships/accept")
                        .header("X-Test-UserId", USER_B_UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(containsString("expired")));
    }

    @Test
    void testAcceptInvite_OwnCode() throws Exception {
        String inviteCode = mockMvc.perform(post("/api/v1/partnerships/invite")
                        .header("X-Test-UserId", USER_A_UID))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String code = extractJsonValue(inviteCode, "inviteCode");

        AcceptInviteRequest req = new AcceptInviteRequest();
        req.setInviteCode(code);

        mockMvc.perform(post("/api/v1/partnerships/accept")
                        .header("X-Test-UserId", USER_A_UID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(containsString("own")));
    }

    private String extractJsonValue(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx == -1) return "";
        int colonIdx = json.indexOf(":", keyIdx);
        int startIdx = json.indexOf("\"", colonIdx) + 1;
        int endIdx = json.indexOf("\"", startIdx);
        return json.substring(startIdx, endIdx);
    }
}
