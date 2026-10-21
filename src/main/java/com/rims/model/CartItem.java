package com.rims.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class CartItem implements Serializable {

    private Item item;
    private int quantity;
    private int rentalDays;

    public CartItem() {}

    public CartItem(Item item, int quantity, int rentalDays) {
        this.item = item;
        this.quantity = quantity;
        this.rentalDays = rentalDays;
    }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getRentalDays() { return rentalDays; }
    public void setRentalDays(int rentalDays) { this.rentalDays = rentalDays; }

    public BigDecimal getRentalFee() {
        if (item == null || item.getPricePerDay() == null) {
            return BigDecimal.ZERO;
        }
        return item.getPricePerDay()
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(rentalDays));
    }

    public BigDecimal getDepositAmount() {
        if (item == null || item.getDepositAmount() == null) {
            return BigDecimal.ZERO;
        }
        return item.getDepositAmount().multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getSubtotal() {
        return getRentalFee().add(getDepositAmount());
    }
}
