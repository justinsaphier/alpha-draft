package com.alphadraft.skeleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.Role;
import com.alphadraft.skeleton.model.StockRequest;
import com.alphadraft.skeleton.repository.AuditingStockRepository;
import com.alphadraft.skeleton.repository.StockAuditEntry;
import com.alphadraft.skeleton.repository.UserRepository;
import com.alphadraft.skeleton.service.StockService;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

// Starts the whole app twice against the same file-backed database, the way the demo restarts
// the app against Supabase: data written before the restart is still there afterwards, and the
// seeders do not add duplicate stocks or a second admin.
class RestartPersistenceTest {

    @TempDir
    Path dataDir;

    private ConfigurableApplicationContext start() {
        String url = "jdbc:h2:file:" + dataDir.resolve("alphadraft").toAbsolutePath().toString().replace('\\', '/');
        // Passed as command-line arguments so they override application.properties.
        return new SpringApplicationBuilder(SkeletonApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + url,
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
                        "--app.jwt.expiration-minutes=60",
                        "--app.admin.email=admin@alphadraft.test",
                        "--app.admin.password=admin-password");
    }

    @Test
    void stocksUsersAndAuditLogSurviveARestart() {
        int seeded;
        try (ConfigurableApplicationContext first = start()) {
            StockService stocks = first.getBean(StockService.class);
            seeded = stocks.getAllStocks().size();
            // SNOW is not in the S&P 500 starter list, so it is a new ticker here.
            stocks.createStock(new StockRequest("SNOW", "Snowflake Inc.", "Technology"));
            assertEquals(seeded + 1, stocks.getAllStocks().size());
        }

        try (ConfigurableApplicationContext second = start()) {
            List<String> tickers = second.getBean(StockService.class).getAllStocks().stream()
                    .map(s -> s.getTicker()).toList();
            assertEquals(seeded + 1, tickers.size());
            assertEquals("SNOW", tickers.get(seeded));

            UserRepository users = second.getBean(UserRepository.class);
            assertEquals(1, users.count());
            assertEquals(Role.ADMIN, users.findByEmail("admin@alphadraft.test").orElseThrow().getRole());

            List<StockAuditEntry> log = second.getBean(AuditingStockRepository.class).getAuditLog();
            assertEquals(seeded + 1, log.size());
            assertTrue(log.stream().anyMatch(e -> "SNOW".equals(e.ticker()) && "CREATE".equals(e.action())));
        }
    }
}
