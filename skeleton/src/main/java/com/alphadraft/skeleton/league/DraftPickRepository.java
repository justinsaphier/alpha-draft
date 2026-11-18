package com.alphadraft.skeleton.league;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftPickRepository extends JpaRepository<DraftPick, Long> {

    List<DraftPick> findAllByOrderByOverallAsc();

    Optional<DraftPick> findFirstByStockIdIsNullOrderByOverallAsc();
}
