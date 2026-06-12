package com.coffee.platform.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumber(String mobileNumber);

    @Modifying
    @Query("UPDATE Customer c SET c.loyaltyScore = c.loyaltyScore + 1 WHERE c.id = :customerId")
    void incrementLoyalty(Long customerId);
}
