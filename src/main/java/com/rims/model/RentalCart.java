package com.rims.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RentalCart implements Serializable {

    private List<CartItem> items = new ArrayList<>();
    private int defaultDays = 1;

    public RentalCart() {}

    public List<CartItem> getItems() {
        return items;
    }

    public int getDefaultDays() {
        return defaultDays;
    }

    public void setDefaultDays(int defaultDays) {
        this.defaultDays = defaultDays > 0 ? defaultDays : 1;
    }

    public int getRentalDays() {
        if (!items.isEmpty()) {
            return items.get(0).getRentalDays();
        }
        return defaultDays > 0 ? defaultDays : 1;
    }

    public void setRentalDays(int rentalDays) {
        int days = rentalDays > 0 ? rentalDays : 1;
        this.defaultDays = days;
        for (CartItem ci : items) {
            ci.setRentalDays(days);
        }
    }

    public CartItem findItem(Long itemId) {
        if (itemId == null) return null;
        for (CartItem ci : items) {
            if (ci.getItem() != null && itemId.equals(ci.getItem().getId())) {
                return ci;
            }
        }
        return null;
    }

    public void addItem(Item item, int quantity, int rentalDays) {
        int days = rentalDays > 0 ? rentalDays : defaultDays;
        this.defaultDays = days;
        int qty = quantity > 0 ? quantity : 1;
        for (CartItem ci : items) {
            if (ci.getItem().getId().equals(item.getId())) {
                ci.setQuantity(qty);
                ci.setRentalDays(days);
                return;
            }
        }
        items.add(new CartItem(item, qty, days));
    }

    public void removeItem(Long itemId) {
        items.removeIf(ci -> ci.getItem().getId().equals(itemId));
    }

    public void updateItem(Long itemId, int quantity, int days) {
        for (CartItem ci : items) {
            if (ci.getItem().getId().equals(itemId)) {
                if (quantity <= 0) {
                    removeItem(itemId);
                } else {
                    ci.setQuantity(quantity);
                    int d = days > 0 ? days : 1;
                    ci.setRentalDays(d);
                    this.defaultDays = d;
                }
                return;
            }
        }
    }

    public void clear() {
        items.clear();
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal getTotalRentalFee() {
        return items.stream()
                .map(CartItem::getRentalFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalDeposit() {
        return items.stream()
                .map(CartItem::getDepositAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
