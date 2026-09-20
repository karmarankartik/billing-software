package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.WaterMeterAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterAssignmentRepository
        extends JpaRepository<WaterMeterAssignment, UUID> {

    Optional<WaterMeterAssignment> findByActiveAssignmentKey(UUID activeAssignmentKey);

    Optional<WaterMeterAssignment> findByWaterMeterIdAndUnassignedAtIsNull(
            UUID waterMeterId
    );

    List<WaterMeterAssignment> findByWaterMeterIdOrderByAssignedAtDesc(
            UUID waterMeterId
    );

    List<WaterMeterAssignment> findByUserIdOrderByAssignedAtDesc(
            UUID userId
    );

    @Query("""
        SELECT a
        FROM WaterMeterAssignment a
        WHERE a.waterMeterId = :waterMeterId
          AND a.assignedAt <= :pointInTime
          AND (a.unassignedAt IS NULL OR a.unassignedAt > :pointInTime)
        ORDER BY a.assignedAt DESC
        """)
    Optional<WaterMeterAssignment> findAssignmentAt(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("pointInTime") OffsetDateTime pointInTime
    );

    @Query("""
        SELECT COUNT(a)
        FROM WaterMeterAssignment a
        WHERE a.waterMeterId = :waterMeterId
          AND a.assignedAt < :periodEnd
          AND (a.unassignedAt IS NULL OR a.unassignedAt > :periodStart)
        """)
    long countOverlappingAssignments(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("periodStart") OffsetDateTime periodStart,
            @Param("periodEnd") OffsetDateTime periodEnd
    );
}