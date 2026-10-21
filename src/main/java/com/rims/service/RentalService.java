package com.rims.service;

import com.rims.model.*;
import com.rims.repository.ItemRepository;
import com.rims.repository.RentalOrderItemRepository;
import com.rims.repository.RentalOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class RentalService {

    private final RentalOrderRepository rentalOrderRepository;
    private final RentalOrderItemRepository rentalOrderItemRepository;
    private final ItemRepository itemRepository;

    public RentalService(RentalOrderRepository rentalOrderRepository,
                         RentalOrderItemRepository rentalOrderItemRepository,
                         ItemRepository itemRepository) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.rentalOrderItemRepository = rentalOrderItemRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public RentalOrder createPendingOrder(User buyer, RentalCart cart, LocalDate startDate, int rentalDays, String notes) {
        if (cart.isEmpty()) {
            throw new IllegalStateException("Your rental cart is empty.");
        }

        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (rentalDays <= 0) {
            rentalDays = 1;
        }
        LocalDate endDate = startDate.plusDays(rentalDays);

        String orderCode = "RIMS-ORD-" + (100000 + new Random().nextInt(900000));
        while (rentalOrderRepository.findByOrderCode(orderCode).isPresent()) {
            orderCode = "RIMS-ORD-" + (100000 + new Random().nextInt(900000));
        }

        RentalOrder order = new RentalOrder();
        order.setOrderCode(orderCode);
        order.setBuyer(buyer);
        order.setRentalStartDate(startDate);
        order.setRentalEndDate(endDate);
        order.setRentalDays(rentalDays);
        order.setNotes(notes);
        order.setStatus("PENDING");
        order.setPaymentStatus("PENDING");
        order.setPaymentMethod("RAZORPAY");
        order.setTotalAmount(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());

        RentalOrder savedOrder = rentalOrderRepository.save(order);
        BigDecimal grandRentalFee = BigDecimal.ZERO;
        BigDecimal grandDeposit = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (CartItem ci : cart.getItems()) {
            Item item = ci.getItem();
            BigDecimal lineRentalFee = item.getPricePerDay()
                    .multiply(BigDecimal.valueOf(ci.getQuantity()))
                    .multiply(BigDecimal.valueOf(rentalDays));

            BigDecimal depositPerUnit = item.getDepositAmount() != null ? item.getDepositAmount() : BigDecimal.ZERO;
            BigDecimal lineDeposit = depositPerUnit.multiply(BigDecimal.valueOf(ci.getQuantity()));
            BigDecimal lineTotal = lineRentalFee.add(lineDeposit);

            grandRentalFee = grandRentalFee.add(lineRentalFee);
            grandDeposit = grandDeposit.add(lineDeposit);
            grandTotal = grandTotal.add(lineTotal);

            RentalOrderItem orderItem = new RentalOrderItem(
                    savedOrder,
                    item,
                    item.getItemCode(),
                    item.getName(),
                    item.getSeller(),
                    item.getPricePerDay(),
                    ci.getQuantity(),
                    lineTotal
            );
            orderItem.setRentalFeeSubtotal(lineRentalFee);
            orderItem.setDepositPerUnit(depositPerUnit);
            orderItem.setDepositSubtotal(lineDeposit);

            savedOrder.addItem(orderItem);
            rentalOrderItemRepository.save(orderItem);
        }

        savedOrder.setRentalFeeTotal(grandRentalFee);
        savedOrder.setDepositTotal(grandDeposit);
        savedOrder.setTotalAmount(grandTotal);
        return rentalOrderRepository.save(savedOrder);
    }

    @Transactional
    public RentalOrder processCheckout(User buyer, RentalCart cart, LocalDate startDate, int rentalDays, String notes) {
        RentalOrder order = createPendingOrder(buyer, cart, startDate, rentalDays, notes);
        order.setStatus("CONFIRMED");
        order.setPaymentStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        
        // Deduct inventory
        for (CartItem ci : cart.getItems()) {
            Item item = ci.getItem();
            if (item.getAvailableQuantity() >= ci.getQuantity()) {
                item.setAvailableQuantity(item.getAvailableQuantity() - ci.getQuantity());
                itemRepository.save(item);
            }
        }
        return rentalOrderRepository.save(order);
    }

    @Transactional
    public RentalOrder confirmPaidOrder(Long orderId, String razorpayPaymentId, String razorpayOrderId, String razorpaySignature) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        order.setStatus("CONFIRMED");
        order.setPaymentStatus("PAID");
        order.setPaymentMethod("RAZORPAY");
        order.setRazorpayPaymentId(razorpayPaymentId);
        if (razorpayOrderId != null && !razorpayOrderId.isBlank()) {
            order.setRazorpayOrderId(razorpayOrderId);
        }
        order.setRazorpaySignature(razorpaySignature);
        order.setPaidAt(LocalDateTime.now());

        // Deduct available inventory
        for (RentalOrderItem roi : order.getItems()) {
            if (roi.getItem() != null) {
                Item item = roi.getItem();
                if (item.getAvailableQuantity() >= roi.getQuantity()) {
                    item.setAvailableQuantity(item.getAvailableQuantity() - roi.getQuantity());
                    itemRepository.save(item);
                }
            }
        }

        return rentalOrderRepository.save(order);
    }

    public List<RentalOrder> getOrdersByBuyer(User buyer) {
        return rentalOrderRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    public List<RentalOrderItem> getRentalsBySeller(User seller) {
        return rentalOrderItemRepository.findBySellerOrderByOrderDateDesc(seller);
    }

    public List<RentalOrder> getAllOrders() {
        return rentalOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<RentalOrder> findById(Long id) {
        return rentalOrderRepository.findById(id);
    }

    public Optional<RentalOrder> findByOrderCode(String orderCode) {
        return rentalOrderRepository.findByOrderCode(orderCode);
    }

    public long countTotalOrders() {
        return rentalOrderRepository.count();
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        rentalOrderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            rentalOrderRepository.save(order);
        });
    }

    @Transactional
    public RentalOrder requestReturn(Long orderId, User buyer, String returnNotes) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new SecurityException("Unauthorized access to order.");
        }
        if ("RETURNED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order has already been returned.");
        }
        order.setStatus("RETURN_REQUESTED");
        order.setReturnInitiatedAt(LocalDateTime.now());
        if (returnNotes != null && !returnNotes.isBlank()) {
            order.setReturnNotes(returnNotes.trim());
        }
        return rentalOrderRepository.save(order);
    }

    @Transactional
    public RentalOrderItem markItemAsReturned(Long orderItemId, User seller) {
        RentalOrderItem roi = rentalOrderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Rental item not found with ID: " + orderItemId));
        if (!roi.getSeller().getId().equals(seller.getId())) {
            throw new SecurityException("Unauthorized: item does not belong to your seller account.");
        }
        if (roi.isReturned()) {
            return roi;
        }

        roi.setReturned(true);
        roi.setReturnedAt(LocalDateTime.now());

        // Reset / replenish available stock in inventory
        if (roi.getItem() != null) {
            Item item = roi.getItem();
            int currentQty = item.getAvailableQuantity() != null ? item.getAvailableQuantity() : 0;
            item.setAvailableQuantity(currentQty + roi.getQuantity());
            itemRepository.save(item);
        }

        RentalOrderItem savedItem = rentalOrderItemRepository.save(roi);

        // If all items in this order are returned, update order status to RETURNED
        RentalOrder order = roi.getOrder();
        if (order != null) {
            boolean allReturned = order.getItems().stream().allMatch(RentalOrderItem::isReturned);
            if (allReturned) {
                order.setStatus("RETURNED");
                order.setReturnedAt(LocalDateTime.now());
                order.setDepositRefunded(true);
                rentalOrderRepository.save(order);
            }
        }
        return savedItem;
    }

    @Transactional
    public RentalOrder markOrderAsReturned(Long orderId, User seller) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        boolean sellerOwnsAny = false;
        for (RentalOrderItem roi : order.getItems()) {
            if (roi.getSeller().getId().equals(seller.getId())) {
                sellerOwnsAny = true;
                if (!roi.isReturned()) {
                    markItemAsReturned(roi.getId(), seller);
                }
            }
        }

        if (!sellerOwnsAny) {
            throw new SecurityException("Unauthorized: No items in this order belong to your seller account.");
        }

        return rentalOrderRepository.findById(orderId).orElse(order);
    }
}
