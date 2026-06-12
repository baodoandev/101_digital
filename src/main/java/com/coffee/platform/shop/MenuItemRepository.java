package com.coffee.platform.shop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByShopIdAndAvailableTrue(Long shopId);

    List<MenuItem> findByShopId(Long shopId);

    boolean existsByShopIdAndName(Long shopId, String name);
}
