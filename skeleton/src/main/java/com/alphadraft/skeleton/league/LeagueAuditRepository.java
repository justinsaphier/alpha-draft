package com.alphadraft.skeleton.league;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueAuditRepository extends JpaRepository<LeagueAudit, Long> {

    List<LeagueAudit> findTop50ByOrderByAtDesc();
}
