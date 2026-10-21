package com.rims.service;

import com.rims.model.Category;
import com.rims.model.Item;
import com.rims.model.ItemImage;
import com.rims.model.User;
import com.rims.repository.CategoryRepository;
import com.rims.repository.ItemImageRepository;
import com.rims.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final CategoryRepository categoryRepository;

    public ItemService(ItemRepository itemRepository, 
                       ItemImageRepository itemImageRepository,
                       CategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.itemImageRepository = itemImageRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Item> getAllActiveItems() {
        return itemRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public List<Item> getItemsByCategory(String slug) {
        Optional<Category> catOpt = categoryRepository.findBySlug(slug);
        return catOpt.map(itemRepository::findByCategoryAndActiveTrueOrderByCreatedAtDesc)
                .orElse(Collections.emptyList());
    }

    public List<Item> getItemsByTag(String tag) {
        return itemRepository.findByTag(tag);
    }

    public List<Item> search(String query, String categorySlug) {
        if (query == null || query.isBlank()) {
            if (categorySlug != null && !categorySlug.isBlank()) {
                return getItemsByCategory(categorySlug);
            }
            return getAllActiveItems();
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            return itemRepository.searchItemsWithCategory(query.trim(), categorySlug);
        }
        return itemRepository.searchItems(query.trim());
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Optional<Item> findByItemCode(String itemCode) {
        return itemRepository.findByItemCode(itemCode);
    }

    public List<Item> getItemsBySeller(User seller) {
        return itemRepository.findBySellerOrderByCreatedAtDesc(seller);
    }

    public List<Item> getAllItemsForAdmin() {
        return itemRepository.findAll();
    }

    @Transactional
    public Item registerItem(String customItemCode, String name, String description, Long categoryId,
                             BigDecimal pricePerDay, BigDecimal depositAmount, Integer quantity,
                             String tags, List<String> imageUrls, User seller) {

        Category category;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseGet(this::getOrCreateOtherCategory);
        } else {
            category = getOrCreateOtherCategory();
        }

        String finalItemCode = (customItemCode != null && !customItemCode.isBlank()) 
                ? customItemCode.trim().toUpperCase() 
                : generateUniqueItemCode(category.getSlug());

        // Ensure uniqueness
        if (itemRepository.existsByItemCode(finalItemCode)) {
            finalItemCode = generateUniqueItemCode(category.getSlug());
        }

        Item item = new Item(finalItemCode, name, description, category, pricePerDay, 
                quantity != null && quantity > 0 ? quantity : 1, tags, seller);
        
        if (depositAmount != null) {
            item.setDepositAmount(depositAmount);
        }

        Item savedItem = itemRepository.save(item);

        // Add photos
        if (imageUrls != null && !imageUrls.isEmpty()) {
            boolean first = true;
            for (String url : imageUrls) {
                if (url != null && !url.isBlank()) {
                    ItemImage img = new ItemImage(savedItem, url.trim(), first);
                    savedItem.addImage(img);
                    itemImageRepository.save(img);
                    first = false;
                }
            }
        } else {
            // Default stock image
            ItemImage defaultImg = new ItemImage(savedItem, 
                    "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?auto=format&fit=crop&w=600&q=80", true);
            savedItem.addImage(defaultImg);
            itemImageRepository.save(defaultImg);
        }

        return savedItem;
    }

    @Transactional
    public void deleteOrDelistItemByAdmin(Long itemId) {
        itemRepository.findById(itemId).ifPresent(item -> {
            item.setActive(false);
            itemRepository.save(item);
        });
    }

    @Transactional
    public void permanentlyRemoveItemByAdmin(Long itemId) {
        itemRepository.deleteById(itemId);
    }

    @Transactional
    public void toggleItemActiveBySeller(Long itemId, User seller) {
        itemRepository.findById(itemId).ifPresent(item -> {
            if (item.getSeller().getId().equals(seller.getId())) {
                item.setActive(!item.isActive());
                itemRepository.save(item);
            }
        });
    }

    public long countActiveItems() {
        return itemRepository.countByActiveTrue();
    }

    public String generateUniqueItemCode(String prefix) {
        String cleanPrefix = prefix != null ? prefix.toUpperCase().replaceAll("[^A-Z]", "") : "RIMS";
        if (cleanPrefix.length() > 4) cleanPrefix = cleanPrefix.substring(0, 4);
        int rand = 1000 + new Random().nextInt(9000);
        String code = "RIMS-" + cleanPrefix + "-" + rand;
        while (itemRepository.existsByItemCode(code)) {
            rand = 1000 + new Random().nextInt(9000);
            code = "RIMS-" + cleanPrefix + "-" + rand;
        }
        return code;
    }

    public Category getOrCreateOtherCategory() {
        return categoryRepository.findBySlug("other")
                .orElseGet(() -> categoryRepository.save(new Category(
                        "Other",
                        "other",
                        "General equipment, specialized tools, accessories, and unclassified rental assets.",
                        "package"
                )));
    }
}
