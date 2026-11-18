package com.alphadraft.skeleton.config;

import com.alphadraft.skeleton.model.Stock;
import com.alphadraft.skeleton.repository.StockRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Loads the starter stocks (every S&P 500 constituent) when the app starts, so neither
// StockService nor the repository has to know what the starting data is. The table lives in
// Supabase and survives restarts, so the seeder only fills an empty table: restarting never adds
// duplicates, and stocks an admin added or deleted are left alone.
@Component
public class StockDataSeeder implements CommandLineRunner {

    // One stock per line as ticker|name|sector. Lines starting with # are comments.
    static final String STARTER_STOCKS_FILE = "/starter-stocks.txt";

    // Id 0 lets the database assign ids in insert order (1..N on an empty table).
    public static final List<Stock> STARTER_STOCKS = loadStarterStocks();

    private final StockRepository stockRepository;

    public StockDataSeeder(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public void run(String... args) {
        if (!stockRepository.findAll().isEmpty()) {
            return;
        }
        // Save a fresh copy of each starter stock. JPA writes the generated id back into the
        // object it saves, so saving the shared constants would change them for later runs.
        STARTER_STOCKS.forEach(stock -> stockRepository.save(
                new Stock(0, stock.getTicker(), stock.getName(), stock.getSector())));
    }

    private static List<Stock> loadStarterStocks() {
        try (InputStream input = StockDataSeeder.class.getResourceAsStream(STARTER_STOCKS_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Missing seed file " + STARTER_STOCKS_FILE);
            }
            List<Stock> stocks = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.split("\\|", -1);
                    if (fields.length != 3) {
                        throw new IllegalStateException(
                                "Bad line " + lineNumber + " in " + STARTER_STOCKS_FILE + ": " + line);
                    }
                    stocks.add(new Stock(0, fields[0], fields[1], fields[2]));
                }
            }
            return List.copyOf(stocks);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + STARTER_STOCKS_FILE, e);
        }
    }
}
