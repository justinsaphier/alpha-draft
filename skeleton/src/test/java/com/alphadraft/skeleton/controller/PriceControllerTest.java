package com.alphadraft.skeleton.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alphadraft.skeleton.auth.TokenService;
import com.alphadraft.skeleton.config.SecurityConfig;
import com.alphadraft.skeleton.model.DailyPrice;
import com.alphadraft.skeleton.model.Role;
import com.alphadraft.skeleton.model.User;
import com.alphadraft.skeleton.price.PriceRefreshService;
import com.alphadraft.skeleton.price.PriceRefreshService.StartOutcome;
import com.alphadraft.skeleton.price.PriceRefreshStatus;
import com.alphadraft.skeleton.repository.DailyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = PriceController.class, properties = {
    "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
    "app.jwt.expiration-minutes=60"
})
@Import({SecurityConfig.class, TokenService.class})
class PriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private DailyPriceRepository prices;

    @MockitoBean
    private PriceRefreshService refresh;

    private String bearer(Role role) {
        return "Bearer " + tokenService.issue(new User(role.name().toLowerCase() + "@example.com", "hash", role));
    }

    private static PriceRefreshStatus idle() {
        return new PriceRefreshStatus(true, false, null, null, 503, null);
    }

    @Test
    void latestPricesAreReturnedToAnySignedInPlayer() throws Exception {
        when(prices.findLatestForEachStock()).thenReturn(List.of(
                new DailyPrice(1, LocalDate.of(2026, 11, 13), new BigDecimal("230.5000"), "twelvedata")));

        mockMvc.perform(get("/prices/latest").header(HttpHeaders.AUTHORIZATION, bearer(Role.PLAYER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockId").value(1))
                .andExpect(jsonPath("$[0].closePrice").value(230.5))
                .andExpect(jsonPath("$[0].tradeDate").value("2026-11-13"));
    }

    @Test
    void latestPricesNeedASignIn() throws Exception {
        mockMvc.perform(get("/prices/latest")).andExpect(status().isUnauthorized());
    }

    @Test
    void statusIsReturnedToPlayers() throws Exception {
        when(refresh.status()).thenReturn(idle());

        mockMvc.perform(get("/prices/refresh").header(HttpHeaders.AUTHORIZATION, bearer(Role.PLAYER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.running").value(false))
                .andExpect(jsonPath("$.lastStored").value(503));
    }

    @Test
    void anAdminCanStartARefreshAndGetsAccepted() throws Exception {
        when(refresh.start()).thenReturn(StartOutcome.STARTED);
        when(refresh.status()).thenReturn(new PriceRefreshStatus(true, true, null, null, null, null));

        mockMvc.perform(post("/prices/refresh").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.running").value(true));
    }

    @Test
    void aRefreshAlreadyRunningIsConflict() throws Exception {
        when(refresh.start()).thenReturn(StartOutcome.ALREADY_RUNNING);
        when(refresh.status()).thenReturn(new PriceRefreshStatus(true, true, null, null, null, null));

        mockMvc.perform(post("/prices/refresh").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isConflict());
    }

    @Test
    void aRefreshWithoutAnApiKeyIsUnavailable() throws Exception {
        when(refresh.start()).thenReturn(StartOutcome.NOT_CONFIGURED);
        when(refresh.status()).thenReturn(new PriceRefreshStatus(false, false, null, null, null, null));

        mockMvc.perform(post("/prices/refresh").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    void aPlayerCannotStartARefresh() throws Exception {
        mockMvc.perform(post("/prices/refresh").header(HttpHeaders.AUTHORIZATION, bearer(Role.PLAYER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAnonymousRequestCannotStartARefresh() throws Exception {
        mockMvc.perform(post("/prices/refresh").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
