package com.rims.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rental_orders")
public class RentalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String orderCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal rentalFeeTotal = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal depositTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status = "CONFIRMED"; // CONFIRMED, ACTIVE, RETURNED, CANCELLED

    private LocalDate rentalStartDate;
    private LocalDate rentalEndDate;
    private int rentalDays = 1;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RentalOrderItem> items = new ArrayList<>();

    @Column(length = 32)
    private String paymentStatus = "PAID"; // PENDING, PAID, FAILED

    @Column(length = 32)
    private String paymentMethod = "RAZORPAY";

    @Column(length = 100)
    private String razorpayOrderId;

    @Column(length = 100)
    private String razorpayPaymentId;

    @Column(length = 255)
    private String razorpaySignature;

    private LocalDateTime paidAt;

    private LocalDateTime returnInitiatedAt;

    @Column(length = 1000)
    private String returnNotes;

    private LocalDateTime returnedAt;

    @Column(nullable = true)
    private Boolean depositRefunded = Boolean.FALSE;

    private LocalDateTime createdAt = LocalDateTime.now();

    public RentalOrder() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public User getBuyer() { return buyer; }
    public void setBuyer(User buyer) { this.buyer = buyer; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getRentalFeeTotal() { return rentalFeeTotal; }
    public void setRentalFeeTotal(BigDecimal rentalFeeTotal) { this.rentalFeeTotal = rentalFeeTotal; }

    public BigDecimal getDepositTotal() { return depositTotal; }
    public void setDepositTotal(BigDecimal depositTotal) { this.depositTotal = depositTotal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getRentalStartDate() { return rentalStartDate; }
    public void setRentalStartDate(LocalDate rentalStartDate) { this.rentalStartDate = rentalStartDate; }

    public LocalDate getRentalEndDate() { return rentalEndDate; }
    public void setRentalEndDate(LocalDate rentalEndDate) { this.rentalEndDate = rentalEndDate; }

    public int getRentalDays() { return rentalDays; }
    public void setRentalDays(int rentalDays) { this.rentalDays = rentalDays; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<RentalOrderItem> getItems() { return items; }
    public void setItems(List<RentalOrderItem> items) { this.items = items; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getReturnInitiatedAt() { return returnInitiatedAt; }
    public void setReturnInitiatedAt(LocalDateTime returnInitiatedAt) { this.returnInitiatedAt = returnInitiatedAt; }

    public String getReturnNotes() { return returnNotes; }
    public void setReturnNotes(String returnNotes) { this.returnNotes = returnNotes; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }

    public boolean isDepositRefunded() { return Boolean.TRUE.equals(depositRefunded); }
    public void setDepositRefunded(Boolean depositRefunded) { this.depositRefunded = depositRefunded != null ? depositRefunded : Boolean.FALSE; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void addItem(RentalOrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
