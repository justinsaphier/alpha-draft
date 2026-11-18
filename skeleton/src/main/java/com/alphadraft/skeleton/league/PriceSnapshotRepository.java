package com.alphadraft.skeleton.league;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    Optional<PriceSnapshot> findByStockIdAndMonthIndex(int stockId, int monthIndex);

    List<PriceSnapshot> findByMonthIndex(int monthIndex);
}
