package com.attendance.repository;

import com.attendance.entity.DeviceChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DeviceChallenge entity operations.
 */
@Repository
public interface DeviceChallengeRepository extends JpaRepository<DeviceChallenge, Long> {

    Optional<DeviceChallenge> findByChallengeHash(String challengeHash);

    List<DeviceChallenge> findByDeviceDeviceIdAndConsumedFalse(Long deviceId);
}
