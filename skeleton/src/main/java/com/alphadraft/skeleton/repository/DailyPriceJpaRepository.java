package com.alphadraft.skeleton.repository;

import com.alphadraft.skeleton.model.DailyPrice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Spring Data generates the implementation at startup. JpaDailyPriceRepository adapts it to the
// DailyPriceRepository interface the rest of the app uses.
public interface DailyPriceJpaRepository extends JpaRepository<DailyPrice, Long> {

    boolean existsByStockIdAndTradeDate(int stockId, LocalDate tradeDate);

    @Query("select p from DailyPrice p where p.tradeDate = "
            + "(select max(q.tradeDate) from DailyPrice q where q.stockId = p.stockId)")
    List<DailyPrice> findLatestForEachStock();
}
