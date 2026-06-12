package com.coffee.platform.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    List<Shop> findByOwnerId(Long ownerId);

    @Query(value = """
            SELECT s.* FROM shop s
            WHERE ST_DWithin(s.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
            AND s.status = 'ACTIVE'
            ORDER BY s.location <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
            LIMIT :lim
            """, nativeQuery = true)
    List<Shop> findNearest(double lat, double lng, double radiusMeters, int lim);

    List<Shop> findByStatus(String status);
}
