package com.coffee.platform.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustOrderRepository extends JpaRepository<CustOrder, Long> {

    List<CustOrder> findByCustomerIdOrderByPlacedAtDesc(Long customerId);

    Optional<CustOrder> findByCustomerIdAndIdempotencyKey(Long customerId, String idempotencyKey);
}
