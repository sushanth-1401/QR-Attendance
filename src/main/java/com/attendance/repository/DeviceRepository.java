package com.attendance.repository;

import com.attendance.entity.RegisteredDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for RegisteredDevice entity operations.
 */
@Repository
public interface DeviceRepository extends JpaRepository<RegisteredDevice, Long> {

    List<RegisteredDevice> findByStudentStudentIdAndDeviceStatus(Long studentId,
                                                                 RegisteredDevice.DeviceStatus deviceStatus);

    List<RegisteredDevice> findByStudentStudentId(Long studentId);

    long countByStudentStudentIdAndDeviceStatus(Long studentId,
                                                RegisteredDevice.DeviceStatus deviceStatus);
}
