package com.rims.model;

import jakarta.persistence.*;

@Entity
@Table(name = "item_images")
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    private boolean isPrimary = false;

    public ItemImage() {}

    public ItemImage(String imageUrl, boolean isPrimary) {
        this.imageUrl = imageUrl;
        this.isPrimary = isPrimary;
    }

    public ItemImage(Item item, String imageUrl, boolean isPrimary) {
        this.item = item;
        this.imageUrl = imageUrl;
        this.isPrimary = isPrimary;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
}
