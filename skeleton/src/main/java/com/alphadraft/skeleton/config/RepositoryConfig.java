package com.alphadraft.skeleton.config;

import com.alphadraft.skeleton.repository.AuditingStockRepository;
import com.alphadraft.skeleton.repository.DailyPriceJpaRepository;
import com.alphadraft.skeleton.repository.DailyPriceRepository;
import com.alphadraft.skeleton.repository.JpaDailyPriceRepository;
import com.alphadraft.skeleton.repository.JpaStockAuditLog;
import com.alphadraft.skeleton.repository.JpaStockRepository;
import com.alphadraft.skeleton.repository.StockAuditJpaRepository;
import com.alphadraft.skeleton.repository.StockJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Chooses the StockRepository everyone else gets. The decoration happens here, in one
// line: the database-backed repository wrapped in the auditing decorator, which writes its
// entries to the database-backed audit log.
@Configuration
public class RepositoryConfig {

    @Bean
    public AuditingStockRepository stockRepository(StockJpaRepository stockJpaRepository,
                                                   StockAuditJpaRepository stockAuditJpaRepository) {
        return new AuditingStockRepository(
                new JpaStockRepository(stockJpaRepository),
                new JpaStockAuditLog(stockAuditJpaRepository));
    }

    @Bean
    public DailyPriceRepository dailyPriceRepository(DailyPriceJpaRepository dailyPriceJpaRepository) {
        return new JpaDailyPriceRepository(dailyPriceJpaRepository);
    }
}
