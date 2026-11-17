package com.alphadraft.skeleton.config;

import com.alphadraft.skeleton.repository.AuditingStockRepository;
import com.alphadraft.skeleton.repository.JpaStockRepository;
import com.alphadraft.skeleton.repository.StockJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Chooses the StockRepository everyone else gets. The decoration happens here, in one
// line: the database-backed repository wrapped in the auditing decorator.
@Configuration
public class RepositoryConfig {

    @Bean
    public AuditingStockRepository stockRepository(StockJpaRepository stockJpaRepository) {
        return new AuditingStockRepository(new JpaStockRepository(stockJpaRepository));
    }
}
