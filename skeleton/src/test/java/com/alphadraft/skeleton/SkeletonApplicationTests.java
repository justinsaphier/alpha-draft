package com.alphadraft.skeleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.alphadraft.skeleton.repository.InMemoryStockRepository;
import com.alphadraft.skeleton.repository.StockRepository;
import com.alphadraft.skeleton.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SkeletonApplicationTests {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockService stockService;

    @Test
    void springInjectsInMemoryRepositoryIntoService() {
        assertInstanceOf(InMemoryStockRepository.class, stockRepository);
        assertEquals(6, stockService.getAllStocks().size());
    }
}
