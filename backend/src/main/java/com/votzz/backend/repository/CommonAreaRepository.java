package com.votzz.backend.repository;

import com.votzz.backend.domain.CommonArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CommonAreaRepository extends JpaRepository<CommonArea, UUID> {
}