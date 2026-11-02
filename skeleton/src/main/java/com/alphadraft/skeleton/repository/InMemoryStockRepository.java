package com.alphadraft.skeleton.repository;

import com.alphadraft.skeleton.model.Stock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryStockRepository implements StockRepository {

    // Keyed by id so stocks can be changed later; sorted map keeps findAll in id order.
    // Starts empty; StockDataSeeder loads the starter stocks on startup.
    private final Map<Integer, Stock> stocks = new ConcurrentSkipListMap<>();

    // Highest id ever stored or issued. Deleting a stock never lowers it, so ids are not reused.
    private final AtomicInteger highestId = new AtomicInteger();

    @Override
    public List<Stock> findAll() {
        return new ArrayList<>(stocks.values());
    }

    @Override
    public Optional<Stock> findById(int id) {
        return Optional.ofNullable(stocks.get(id));
    }

    @Override
    public Stock save(Stock stock) {
        stocks.put(stock.getId(), stock);
        highestId.accumulateAndGet(stock.getId(), Math::max);
        return stock;
    }

    @Override
    public boolean deleteById(int id) {
        return stocks.remove(id) != null;
    }

    @Override
    public int nextId() {
        return highestId.incrementAndGet();
    }
}
