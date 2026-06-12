package com.coffee.platform.shop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopQueueRepository extends JpaRepository<ShopQueue, Long> {

    List<ShopQueue> findByShopId(Long shopId);

    int countByShopId(Long shopId);
}
