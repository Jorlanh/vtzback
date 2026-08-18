package com.votzz.backend.repository;

import com.votzz.backend.domain.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {
}