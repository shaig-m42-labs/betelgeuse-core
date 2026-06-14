package com.m42.betelgeuse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:core-flow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CoreFlowTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    EventPublisher events;

    @Test
    void createsCatalogHealthCheckAndIdempotentIncident() throws Exception {
        JsonNode org = json(mockMvc.perform(post("/organizations")
                        .header("X-User-Email", "demo@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Demo Org\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String organizationId = org.path("data").path("id").asText();

        mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "name": "Platform"
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk());

        JsonNode service = json(mockMvc.perform(post("/services")
                        .header("X-Correlation-Id", "corr-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "name": "orders-api",
                                  "description": "Demo service"
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String serviceId = service.path("data").path("id").asText();

        JsonNode environment = json(mockMvc.perform(post("/services/%s/environments".formatted(serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "prod",
                                  "baseUrl": "http://example.invalid"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String environmentId = environment.path("data").path("id").asText();

        JsonNode healthCheck = json(mockMvc.perform(post("/services/%s/health-checks".formatted(serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environmentId": "%s",
                                  "name": "prod-health",
                                  "url": "http://example.invalid/health",
                                  "method": "GET",
                                  "intervalSeconds": 30,
                                  "timeoutSeconds": 3,
                                  "enabled": true
                                }
                                """.formatted(environmentId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String healthCheckId = healthCheck.path("data").path("id").asText();

        mockMvc.perform(get("/internal/health-checks/active"))
                .andExpect(status().isOk());

        String failure = """
                {
                  "healthCheckConfigId": "%s",
                  "serviceId": "%s",
                  "url": "http://example.invalid/health",
                  "statusCode": 500,
                  "error": "unhealthy_status"
                }
                """.formatted(healthCheckId, serviceId);

        mockMvc.perform(post("/internal/health-check-failures")
                        .header("X-Correlation-Id", "corr-failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failure))
                .andExpect(status().isOk());
        mockMvc.perform(post("/internal/health-check-failures")
                        .header("X-Correlation-Id", "corr-failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failure))
                .andExpect(status().isOk());

        JsonNode incidents = json(mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        if (incidents.path("data").size() != 1) {
            throw new AssertionError("expected exactly one active incident for duplicate failures");
        }

        String incidentId = incidents.path("data").get(0).path("id").asText();
        mockMvc.perform(patch("/incidents/%s/resolve".formatted(incidentId))
                        .header("X-Correlation-Id", "corr-resolve"))
                .andExpect(status().isOk());

        verify(events, atLeastOnce()).publish(anyString(), anyString(), anyString(), any());
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }
}
