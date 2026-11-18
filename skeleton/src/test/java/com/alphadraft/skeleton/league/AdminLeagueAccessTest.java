package com.alphadraft.skeleton.league;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alphadraft.skeleton.auth.TokenService;
import com.alphadraft.skeleton.config.SecurityConfig;
import com.alphadraft.skeleton.model.Role;
import com.alphadraft.skeleton.model.User;
import com.alphadraft.skeleton.repository.StockRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Only admins may reach the league's admin endpoints; players get 403 and anonymous callers 401.
@WebMvcTest(value = AdminLeagueController.class, properties = {
    "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
    "app.jwt.expiration-minutes=60"
})
@Import({SecurityConfig.class, TokenService.class})
class AdminLeagueAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private LeagueService league;

    @MockitoBean
    private DraftService draft;

    @MockitoBean
    private SeasonService season;

    @MockitoBean
    private CallService calls;

    @MockitoBean
    private StockRepository stocks;

    @MockitoBean
    private LeagueResetService reset;

    private String bearer(Role role) {
        return "Bearer " + tokenService.issue(new User(role.name().toLowerCase() + "@example.com", "hash", role));
    }

    @Test
    void aPlayerIsRefusedTheAdminConsole() throws Exception {
        mockMvc.perform(get("/admin/league").header(HttpHeaders.AUTHORIZATION, bearer(Role.PLAYER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aPlayerIsRefusedTheSeasonStart() throws Exception {
        mockMvc.perform(post("/admin/league/season/start").header(HttpHeaders.AUTHORIZATION, bearer(Role.PLAYER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAnonymousCallerIsAskedToSignIn() throws Exception {
        mockMvc.perform(get("/admin/league")).andExpect(status().isUnauthorized());
    }

    @Test
    void anAdminReachesTheDraftStart() throws Exception {
        mockMvc.perform(post("/admin/league/draft/start").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isNoContent());
    }
}
