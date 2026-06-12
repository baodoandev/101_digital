package com.coffee.platform.shop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperatorAssignmentRepository extends JpaRepository<OperatorAssignment, Long> {

    boolean existsByUserIdAndShopId(Long userId, Long shopId);

    List<OperatorAssignment> findByUserId(Long userId);
}
