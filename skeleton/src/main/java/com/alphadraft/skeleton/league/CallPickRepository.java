package com.alphadraft.skeleton.league;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallPickRepository extends JpaRepository<CallPick, Long> {

    List<CallPick> findByTeamIdAndMonthIndex(long teamId, int monthIndex);

    void deleteByTeamIdAndMonthIndex(long teamId, int monthIndex);
}
