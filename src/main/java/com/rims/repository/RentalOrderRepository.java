package com.rims.repository;

import com.rims.model.RentalOrder;
import com.rims.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {
    Optional<RentalOrder> findByOrderCode(String orderCode);
    Optional<RentalOrder> findByRazorpayOrderId(String razorpayOrderId);
    List<RentalOrder> findByBuyerOrderByCreatedAtDesc(User buyer);
    List<RentalOrder> findAllByOrderByCreatedAtDesc();
}
