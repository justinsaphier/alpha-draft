package com.alphadraft.skeleton.league;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByOwnerEmailIgnoreCase(String ownerEmail);

    List<Team> findByStatusIn(Collection<TeamStatus> statuses);

    List<Team> findByStatusOrderByRequestedAtAsc(TeamStatus status);
}
