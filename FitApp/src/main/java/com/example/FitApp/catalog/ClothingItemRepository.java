package com.example.FitApp.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long>,
        JpaSpecificationExecutor<ClothingItem> {
}
