package com.alphadraft.skeleton.config;

import com.alphadraft.skeleton.repository.AuditingStockRepository;
import com.alphadraft.skeleton.repository.InMemoryStockRepository;
import com.alphadraft.skeleton.repository.StockRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Chooses the StockRepository everyone else gets. The decoration happens here, in one
// line: the real storage wrapped in the auditing decorator.
@Configuration
public class RepositoryConfig {

    @Bean
    public AuditingStockRepository stockRepository() {
        return new AuditingStockRepository(new InMemoryStockRepository());
    }
}
