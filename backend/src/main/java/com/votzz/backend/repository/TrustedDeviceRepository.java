package com.votzz.backend.repository;

import com.votzz.backend.domain.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, UUID> {
    Optional<TrustedDevice> findByUserIdAndDeviceIdentifier(UUID userId, String deviceIdentifier);
}