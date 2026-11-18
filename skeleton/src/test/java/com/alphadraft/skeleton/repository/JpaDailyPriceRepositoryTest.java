package com.alphadraft.skeleton.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphadraft.skeleton.model.DailyPrice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

// In-memory H2, rolled back after each test; no Supabase credentials needed.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:daily-price-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class JpaDailyPriceRepositoryTest {

    private static final LocalDate FRIDAY = LocalDate.of(2026, 11, 13);

    @Autowired
    private DailyPriceJpaRepository dailyPriceJpaRepository;

    private DailyPriceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaDailyPriceRepository(dailyPriceJpaRepository);
    }

    @Test
    void existsIsTrueOnlyForTheSavedStockAndDay() {
        repository.save(new DailyPrice(1, FRIDAY, new BigDecimal("230.5000"), "twelvedata"));

        assertTrue(repository.exists(1, FRIDAY));
        assertFalse(repository.exists(1, FRIDAY.minusDays(1)));
        assertFalse(repository.exists(2, FRIDAY));
    }

    @Test
    void latestForEachStockIsTheNewestCloseOfEachStock() {
        repository.save(new DailyPrice(1, FRIDAY.minusDays(1), new BigDecimal("225.0000"), "twelvedata"));
        repository.save(new DailyPrice(1, FRIDAY, new BigDecimal("230.5000"), "twelvedata"));
        repository.save(new DailyPrice(2, FRIDAY, new BigDecimal("310.2500"), "twelvedata"));

        List<DailyPrice> latest = repository.findLatestForEachStock();

        assertEquals(2, latest.size());
        assertEquals(new BigDecimal("230.5000"),
                latest.stream().filter(p -> p.getStockId() == 1).findFirst().orElseThrow().getClosePrice().setScale(4));
        assertEquals(new BigDecimal("310.2500"),
                latest.stream().filter(p -> p.getStockId() == 2).findFirst().orElseThrow().getClosePrice().setScale(4));
    }

    @Test
    void theDatabaseRefusesASecondPriceForTheSameStockAndDay() {
        repository.save(new DailyPrice(1, FRIDAY, new BigDecimal("230.5000"), "twelvedata"));

        assertThrows(DataIntegrityViolationException.class, () ->
                dailyPriceJpaRepository.saveAndFlush(new DailyPrice(1, FRIDAY, new BigDecimal("231.0000"), "twelvedata")));
    }
}
