package com.alphadraft.skeleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alphadraft.skeleton.model.Role;
import com.alphadraft.skeleton.repository.StockAuditJpaRepository;
import com.alphadraft.skeleton.repository.StockJpaRepository;
import com.alphadraft.skeleton.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// The TA demo, end to end through the real app (security, services, JPA) on an H2 database:
// a player signs up, the configured admin signs in, and the admin-only action is accepted for
// the admin and refused for the player.
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:demo-flow-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
    "app.jwt.expiration-minutes=60",
    "app.admin.email=admin@alphadraft.test",
    "app.admin.password=admin-password"
})
@AutoConfigureMockMvc
class DemoFlowTest {

    // SNOW is not in the S&P 500 starter list, so it can be added on top of the seeded stocks.
    private static final String SNOW_JSON =
            "{\"ticker\":\"SNOW\",\"name\":\"Snowflake Inc.\",\"sector\":\"Technology\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private StockJpaRepository stocks;

    @Autowired
    private StockAuditJpaRepository auditLog;

    private String tokenFrom(String path, String email, String password, int expectedStatus) throws Exception {
        String body = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void adminAccountIsCreatedFromConfigurationOnStartup() {
        assertEquals(Role.ADMIN, users.findByEmail("admin@alphadraft.test").orElseThrow().getRole());
    }

    @Test
    void playerAndAdminSeeTheirRolesAndOnlyTheAdminCanChangeStocks() throws Exception {
        String player = tokenFrom("/auth/signup", "player-demo@example.com", "player-password", 201);
        String admin = tokenFrom("/auth/signin", "admin@alphadraft.test", "admin-password", 200);

        mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"player-demo@example.com\",\"password\":\"player-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PLAYER"));

        // Both roles can read the pool.
        mockMvc.perform(get("/stocks").header(HttpHeaders.AUTHORIZATION, bearer(player)))
                .andExpect(status().isOk());

        // The admin-only action: refused for the player, nothing stored.
        mockMvc.perform(post("/stocks").header(HttpHeaders.AUTHORIZATION, bearer(player))
                        .contentType(MediaType.APPLICATION_JSON).content(SNOW_JSON))
                .andExpect(status().isForbidden());
        assertTrue(stocks.findAll().stream().noneMatch(s -> s.getTicker().equals("SNOW")));

        // Accepted for the admin, stored in the stocks table and recorded in the audit table.
        long auditBefore = auditLog.count();
        String created = mockMvc.perform(post("/stocks").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(SNOW_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");
        assertTrue(stocks.existsById(id));
        assertEquals(auditBefore + 1, auditLog.count());

        // Delete follows the same rule.
        mockMvc.perform(delete("/stocks/" + id).header(HttpHeaders.AUTHORIZATION, bearer(player)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/stocks/" + id).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void stocksNeedASignedInUser() throws Exception {
        mockMvc.perform(get("/stocks")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/stocks").contentType(MediaType.APPLICATION_JSON).content(SNOW_JSON))
                .andExpect(status().isUnauthorized());
    }
}
