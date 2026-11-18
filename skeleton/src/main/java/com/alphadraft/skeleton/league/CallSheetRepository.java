package com.alphadraft.skeleton.league;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallSheetRepository extends JpaRepository<CallSheet, Long> {

    Optional<CallSheet> findByTeamIdAndMonthIndex(long teamId, int monthIndex);
}
