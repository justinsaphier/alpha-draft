package com.alphadraft.skeleton.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alphadraft.skeleton.auth.TokenService;
import com.alphadraft.skeleton.controller.StockController;
import com.alphadraft.skeleton.model.Role;
import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.model.User;
import com.alphadraft.skeleton.service.StockService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// Exercises the real SecurityConfig and JwtAuthenticationFilter with real signed tokens.
// StockService is mocked, so these tests are only about who is let through.
@WebMvcTest(value = StockController.class, properties = {
    "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
    "app.jwt.expiration-minutes=60"
})
@Import({SecurityConfig.class, TokenService.class})
class StockSecurityTest {

    private static final String NVDA_JSON =
            "{\"ticker\":\"NVDA\",\"name\":\"NVIDIA Corporation\",\"sector\":\"Technology\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private StockService stockService;

    private MockHttpServletRequestBuilder as(Role role, MockHttpServletRequestBuilder request) {
        String token = tokenService.issue(new User(role.name().toLowerCase() + "@example.com", "hash", role));
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static MockHttpServletRequestBuilder postNvda() {
        return post("/stocks").contentType(MediaType.APPLICATION_JSON).content(NVDA_JSON);
    }

    private static MockHttpServletRequestBuilder putNvda() {
        return put("/stocks/1").contentType(MediaType.APPLICATION_JSON).content(NVDA_JSON);
    }

    // ---- Anonymous: 401 everywhere on /stocks ----

    @Test
    void anonymousCannotReadStocks() throws Exception {
        mockMvc.perform(get("/stocks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/stocks/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(stockService);
    }

    @Test
    void anonymousCannotChangeStocks() throws Exception {
        mockMvc.perform(postNvda()).andExpect(status().isUnauthorized());
        mockMvc.perform(putNvda()).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/stocks/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(stockService);
    }

    @Test
    void invalidOrTamperedTokenIsTreatedAsAnonymous() throws Exception {
        mockMvc.perform(get("/stocks").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());

        String adminToken = tokenService.issue(new User("admin@example.com", "hash", Role.ADMIN));
        String tampered = adminToken.substring(0, adminToken.length() - 2) + "xx";
        mockMvc.perform(postNvda().header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(stockService);
    }

    // ---- Player: may read, may not change (403) ----

    @Test
    void playerCanReadStocks() throws Exception {
        when(stockService.getAllStocks()).thenReturn(List.of(new Stock(1, "AAPL", "Apple Inc.", "Technology")));
        when(stockService.getStockById(1)).thenReturn(Optional.of(new Stock(1, "AAPL", "Apple Inc.", "Technology")));

        mockMvc.perform(as(Role.PLAYER, get("/stocks"))).andExpect(status().isOk());
        mockMvc.perform(as(Role.PLAYER, get("/stocks/1"))).andExpect(status().isOk());
    }

    @Test
    void playerIsForbiddenFromCreatingUpdatingAndDeleting() throws Exception {
        mockMvc.perform(as(Role.PLAYER, postNvda())).andExpect(status().isForbidden());
        mockMvc.perform(as(Role.PLAYER, putNvda())).andExpect(status().isForbidden());
        mockMvc.perform(as(Role.PLAYER, delete("/stocks/1"))).andExpect(status().isForbidden());
        verifyNoInteractions(stockService);
    }

    // ---- Admin: allowed ----

    @Test
    void adminCanCreateAStock() throws Exception {
        when(stockService.createStock(any())).thenReturn(new Stock(7, "NVDA", "NVIDIA Corporation", "Technology"));

        mockMvc.perform(as(Role.ADMIN, postNvda())).andExpect(status().isCreated());
    }

    @Test
    void adminCanUpdateAStock() throws Exception {
        when(stockService.updateStock(eq(1), any()))
                .thenReturn(Optional.of(new Stock(1, "NVDA", "NVIDIA Corporation", "Technology")));

        mockMvc.perform(as(Role.ADMIN, putNvda())).andExpect(status().isOk());
    }

    @Test
    void adminCanDeleteAStock() throws Exception {
        when(stockService.deleteStock(anyInt())).thenReturn(true);

        mockMvc.perform(as(Role.ADMIN, delete("/stocks/1"))).andExpect(status().isNoContent());
    }

    @Test
    void adminCanReadStocks() throws Exception {
        when(stockService.getAllStocks()).thenReturn(List.of());

        mockMvc.perform(as(Role.ADMIN, get("/stocks"))).andExpect(status().isOk());
    }

    // ---- CORS preflight from the React dev server still works ----

    @Test
    void corsPreflightFromTheFrontendIsAllowedWithoutAToken() throws Exception {
        mockMvc.perform(options("/stocks")
                        .header(HttpHeaders.ORIGIN, SecurityConfig.FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SecurityConfig.FRONTEND_ORIGIN));
    }
}