package com.rims.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guaranteed unique identifier across all products for reference and tracking
    @Column(nullable = false, unique = true, length = 64)
    private String itemCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(precision = 10, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer availableQuantity = 1;

    // Comma-separated tags (e.g. "4K,Cinema,Sony,Full-Frame,Studio")
    @Column(length = 500)
    private String tags;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemImage> images = new ArrayList<>();

    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Item() {}

    public Item(String itemCode, String name, String description, Category category, 
                BigDecimal pricePerDay, Integer availableQuantity, String tags, User seller) {
        this.itemCode = itemCode;
        this.name = name;
        this.description = description;
        this.category = category;
        this.pricePerDay = pricePerDay;
        this.availableQuantity = availableQuantity;
        this.tags = tags;
        this.seller = seller;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public BigDecimal getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(BigDecimal pricePerDay) { this.pricePerDay = pricePerDay; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public List<ItemImage> getImages() { return images; }
    public void setImages(List<ItemImage> images) { this.images = images; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void addImage(ItemImage image) {
        images.add(image);
        image.setItem(this);
    }

    public String getPrimaryImageUrl() {
        if (images != null && !images.isEmpty()) {
            for (ItemImage img : images) {
                if (img.isPrimary()) {
                    return img.getImageUrl();
                }
            }
            return images.get(0).getImageUrl();
        }
        return "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?auto=format&fit=crop&w=600&q=80";
    }

    public List<String> getTagList() {
        if (tags == null || tags.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
