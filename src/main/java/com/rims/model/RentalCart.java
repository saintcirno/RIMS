package com.rims.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RentalCart implements Serializable {

    private List<CartItem> items = new ArrayList<>();
    private int defaultDays = 3;

    public RentalCart() {}

    public List<CartItem> getItems() {
        return items;
    }

    public void addItem(Item item, int quantity, int rentalDays) {
        for (CartItem ci : items) {
            if (ci.getItem().getId().equals(item.getId())) {
                ci.setQuantity(ci.getQuantity() + quantity);
                ci.setRentalDays(rentalDays > 0 ? rentalDays : ci.getRentalDays());
                return;
            }
        }
        items.add(new CartItem(item, quantity, rentalDays > 0 ? rentalDays : defaultDays));
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
                    ci.setRentalDays(days > 0 ? days : 1);
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
