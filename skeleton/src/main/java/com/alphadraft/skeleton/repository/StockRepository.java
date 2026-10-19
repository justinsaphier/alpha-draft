package com.alphadraft.skeleton.repository;

import com.alphadraft.skeleton.model.Stock;
import java.util.List;
import java.util.Optional;

// Storage abstraction for stocks. StockService depends on this interface, not on
// any concrete storage, so the storage can change without touching the service.
public interface StockRepository {

    List<Stock> findAll();

    Optional<Stock> findById(int id);

    Stock save(Stock stock);

    // Returns true if a stock with this id existed and was removed.
    boolean deleteById(int id);
}
