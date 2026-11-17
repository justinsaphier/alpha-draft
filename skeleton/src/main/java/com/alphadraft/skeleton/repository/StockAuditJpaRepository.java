package com.alphadraft.skeleton.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data generates the implementation at startup.
public interface StockAuditJpaRepository extends JpaRepository<StockAuditEntry, Long> {

    List<StockAuditEntry> findAllByOrderByIdAsc();
}
