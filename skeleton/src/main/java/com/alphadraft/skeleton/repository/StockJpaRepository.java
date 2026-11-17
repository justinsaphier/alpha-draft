package com.alphadraft.skeleton.repository;

import com.alphadraft.skeleton.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data generates the implementation at startup. JpaStockRepository adapts it to
// the StockRepository interface the rest of the app uses.
public interface StockJpaRepository extends JpaRepository<Stock, Integer> {

    boolean existsByTickerIgnoreCase(String ticker);

    boolean existsByTickerIgnoreCaseAndIdNot(String ticker, Integer id);
}
