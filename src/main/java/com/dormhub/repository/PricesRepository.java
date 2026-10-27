package com.dormhub.repository;

import com.dormhub.entity.Prices;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricesRepository extends JpaRepository<Prices, String> {
}
