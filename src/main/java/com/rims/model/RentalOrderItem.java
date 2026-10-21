package com.rims.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_order_items")
public class RentalOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id")
    private Item item;

    // Snapshot to preserve unique ID and name even if item changes or gets removed
    @Column(nullable = false)
    private String itemCodeSnapshot;

    @Column(nullable = false)
    private String itemNameSnapshot;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal rentalFeeSubtotal = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal depositPerUnit = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal depositSubtotal = BigDecimal.ZERO;

    @Column(nullable = true)
    private Boolean returned = Boolean.FALSE;

    private LocalDateTime returnedAt;

    public RentalOrderItem() {}

    public RentalOrderItem(RentalOrder order, Item item, String itemCodeSnapshot, String itemNameSnapshot,
                           User seller, BigDecimal pricePerDay, int quantity, BigDecimal subtotal) {
        this.order = order;
        this.item = item;
        this.itemCodeSnapshot = itemCodeSnapshot;
        this.itemNameSnapshot = itemNameSnapshot;
        this.seller = seller;
        this.pricePerDay = pricePerDay;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RentalOrder getOrder() { return order; }
    public void setOrder(RentalOrder order) { this.order = order; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getItemCodeSnapshot() { return itemCodeSnapshot; }
    public void setItemCodeSnapshot(String itemCodeSnapshot) { this.itemCodeSnapshot = itemCodeSnapshot; }

    public String getItemNameSnapshot() { return itemNameSnapshot; }
    public void setItemNameSnapshot(String itemNameSnapshot) { this.itemNameSnapshot = itemNameSnapshot; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public BigDecimal getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(BigDecimal pricePerDay) { this.pricePerDay = pricePerDay; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getRentalFeeSubtotal() { return rentalFeeSubtotal; }
    public void setRentalFeeSubtotal(BigDecimal rentalFeeSubtotal) { this.rentalFeeSubtotal = rentalFeeSubtotal; }

    public BigDecimal getDepositPerUnit() { return depositPerUnit; }
    public void setDepositPerUnit(BigDecimal depositPerUnit) { this.depositPerUnit = depositPerUnit; }

    public BigDecimal getDepositSubtotal() { return depositSubtotal; }
    public void setDepositSubtotal(BigDecimal depositSubtotal) { this.depositSubtotal = depositSubtotal; }

    public boolean isReturned() {
        if (Boolean.TRUE.equals(returned)) return true;
        return order != null && "RETURNED".equalsIgnoreCase(order.getStatus());
    }
    public void setReturned(Boolean returned) { this.returned = returned != null ? returned : Boolean.FALSE; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
}
