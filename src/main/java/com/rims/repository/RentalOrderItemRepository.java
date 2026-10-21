package com.rims.repository;

import com.rims.model.RentalOrderItem;
import com.rims.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalOrderItemRepository extends JpaRepository<RentalOrderItem, Long> {

    @Query("SELECT roi FROM RentalOrderItem roi WHERE roi.seller = :seller ORDER BY roi.order.createdAt DESC")
    List<RentalOrderItem> findBySellerOrderByOrderDateDesc(@Param("seller") User seller);

    List<RentalOrderItem> findByItemCodeSnapshot(String itemCode);
}
