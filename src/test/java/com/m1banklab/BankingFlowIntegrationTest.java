package com.m1banklab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class BankingFlowIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createsCustomerAccountAndMovesMoneyWithAuditAndFraudTrail() throws Exception {
        UUID customerId = createCustomer("Maya", "Chen", "maya.chen@example.com");
        UUID targetCustomerId = createCustomer("Noah", "Banks", "noah.banks@example.com");
        UUID checkingId = createAccount(customerId, "CHECKING", "1000.00");
        UUID savingsId = createAccount(targetCustomerId, "SAVINGS", "100.00");

        mockMvc.perform(post("/api/accounts/{id}/deposit", checkingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": "6000.00", "description": "payroll funding"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.type").value("DEPOSIT"));

        mockMvc.perform(post("/api/accounts/{id}/withdraw", checkingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": "999999.00", "description": "bad withdrawal"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Insufficient funds"));

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId": "%s", "targetAccountId": "%s", "amount": "250.00", "description": "first payee transfer"}
                                """.formatted(checkingId, savingsId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.status").value("POSTED"));

        mockMvc.perform(get("/api/accounts/{id}/transactions", checkingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        String fraudJson = mockMvc.perform(get("/api/fraud/assessments"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(fraudJson).contains("amount over 5000").contains("transfer to new payee");

        String auditJson = mockMvc.perform(get("/api/audit/events"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(auditJson).contains("WITHDRAWAL_FAILED").contains("FRAUD_ASSESSED");
    }

    private UUID createCustomer(String firstName, String lastName, String email) throws Exception {
        String json = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "%s", "lastName": "%s", "email": "%s", "phone": "+1-336-555-1212"}
                                """.formatted(firstName, lastName, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return UUID.fromString(node.get("id").asText());
    }

    private UUID createAccount(UUID customerId, String type, String openingBalance) throws Exception {
        String json = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "%s", "type": "%s", "openingBalance": "%s"}
                                """.formatted(customerId, type, new BigDecimal(openingBalance))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return UUID.fromString(node.get("id").asText());
    }
}
