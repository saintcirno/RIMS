package com.rims.controller;

import com.rims.model.Category;
import com.rims.model.Item;
import com.rims.model.RentalOrder;
import com.rims.model.RentalOrderItem;
import com.rims.model.User;
import com.rims.service.CategoryService;
import com.rims.service.ItemService;
import com.rims.service.RentalService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/seller")
public class SellerController {

    private static final Logger log = LoggerFactory.getLogger(SellerController.class);

    private final ItemService itemService;
    private final CategoryService categoryService;
    private final RentalService rentalService;

    public SellerController(ItemService itemService, CategoryService categoryService, RentalService rentalService) {
        this.itemService = itemService;
        this.categoryService = categoryService;
        this.rentalService = rentalService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User seller = (User) session.getAttribute("currentUser");
        List<Item> items = itemService.getItemsBySeller(seller);
        List<RentalOrderItem> rentals = rentalService.getRentalsBySeller(seller);

        BigDecimal totalRevenue = rentals.stream()
                .map(RentalOrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeItems = items.stream().filter(Item::isActive).count();

        model.addAttribute("seller", seller);
        model.addAttribute("items", items);
        model.addAttribute("recentRentals", rentals.size() > 5 ? rentals.subList(0, 5) : rentals);
        model.addAttribute("totalItemsCount", items.size());
        model.addAttribute("activeItemsCount", activeItems);
        model.addAttribute("totalRentalsCount", rentals.size());
        model.addAttribute("totalRevenue", totalRevenue);

        return "seller/dashboard";
    }

    @GetMapping("/items")
    public String manageItems(HttpSession session, Model model) {
        User seller = (User) session.getAttribute("currentUser");
        List<Item> items = itemService.getItemsBySeller(seller);
        model.addAttribute("seller", seller);
        model.addAttribute("items", items);
        return "seller/items";
    }

    @GetMapping("/items/new")
    public String showRegisterItemPage(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        return "seller/item-create";
    }

    @PostMapping("/items/new")
    public String registerSingleItem(@RequestParam(required = false) String customItemCode,
                                     @RequestParam String name,
                                     @RequestParam String description,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam BigDecimal pricePerDay,
                                     @RequestParam(required = false) BigDecimal depositAmount,
                                     @RequestParam(defaultValue = "1") Integer quantity,
                                     @RequestParam(required = false) String tags,
                                     @RequestParam(required = false) String photosText,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User seller = (User) session.getAttribute("currentUser");

        List<String> imageUrls = parsePhotos(photosText);

        try {
            Item item = itemService.registerItem(customItemCode, name, description, categoryId,
                    pricePerDay, depositAmount, quantity, tags, imageUrls, seller);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Successfully registered item \"" + item.getName() + "\" with Tracking ID [" + item.getItemCode() + "]!");
            return "redirect:/seller/items";
        } catch (Exception e) {
            log.error("[Seller Error] Registration failed for item: {}", name, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "redirect:/seller/items/new";
        }
    }

    @PostMapping("/items/batch")
    public String registerMultipleItems(@RequestParam("name[]") List<String> names,
                                        @RequestParam("description[]") List<String> descriptions,
                                        @RequestParam(value = "categoryId[]", required = false) List<Long> categoryIds,
                                        @RequestParam("pricePerDay[]") List<BigDecimal> prices,
                                        @RequestParam("quantity[]") List<Integer> quantities,
                                        @RequestParam(value = "tags[]", required = false) List<String> tagsList,
                                        @RequestParam(value = "photosText[]", required = false) List<String> photosList,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        User seller = (User) session.getAttribute("currentUser");
        int registeredCount = 0;

        try {
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                if (name == null || name.isBlank()) continue;

                String desc = (descriptions != null && i < descriptions.size()) ? descriptions.get(i) : "";
                Long catId = (categoryIds != null && i < categoryIds.size()) ? categoryIds.get(i) : null;
                BigDecimal price = (prices != null && i < prices.size()) ? prices.get(i) : BigDecimal.valueOf(50);
                Integer qty = (quantities != null && i < quantities.size()) ? quantities.get(i) : 1;
                String tags = (tagsList != null && i < tagsList.size()) ? tagsList.get(i) : "";
                String photos = (photosList != null && i < photosList.size()) ? photosList.get(i) : "";

                List<String> imageUrls = parsePhotos(photos);

                itemService.registerItem(null, name, desc, catId, price, BigDecimal.ZERO, qty, tags, imageUrls, seller);
                registeredCount++;
            }

            redirectAttributes.addFlashAttribute("successMessage", 
                    "Batch registration complete: " + registeredCount + " rental items have been published successfully!");
            return "redirect:/seller/items";
        } catch (Exception e) {
            log.error("[Seller Error] Batch registration failed", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Batch registration error: " + e.getMessage());
            return "redirect:/seller/items/new";
        }
    }

    @PostMapping("/items/{id}/toggle")
    public String toggleItemStatus(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User seller = (User) session.getAttribute("currentUser");
        itemService.toggleItemActiveBySeller(id, seller);
        redirectAttributes.addFlashAttribute("infoMessage", "Item availability status updated.");
        return "redirect:/seller/items";
    }

    @GetMapping("/rentals")
    public String viewSellerRentals(HttpSession session, Model model) {
        User seller = (User) session.getAttribute("currentUser");
        List<RentalOrderItem> rentals = rentalService.getRentalsBySeller(seller);
        model.addAttribute("seller", seller);
        model.addAttribute("rentals", rentals);
        return "seller/rentals";
    }

    @PostMapping("/rentals/{id}/return")
    public String markRentalItemReturned(@PathVariable Long id,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        User seller = (User) session.getAttribute("currentUser");
        try {
            RentalOrderItem item = rentalService.markItemAsReturned(id, seller);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Asset [" + item.getItemCodeSnapshot() + " - " + item.getItemNameSnapshot() + "] marked as RETURNED. Fleet inventory stock restored (+" + item.getQuantity() + " units)!");
        } catch (Exception e) {
            log.error("[Seller Error] Error updating item return status for orderItemId={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating return status: " + e.getMessage());
        }
        return "redirect:/seller/rentals";
    }

    @PostMapping("/orders/{id}/return")
    public String markOrderReturned(@PathVariable Long id,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User seller = (User) session.getAttribute("currentUser");
        try {
            RentalOrder order = rentalService.markOrderAsReturned(id, seller);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Order [" + order.getOrderCode() + "] assets marked as RETURNED. Fleet inventory stock restored!");
        } catch (Exception e) {
            log.error("[Seller Error] Error updating order return for orderId={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating order return: " + e.getMessage());
        }
        return "redirect:/seller/rentals";
    }

    private List<String> parsePhotos(String photosText) {
        List<String> imageUrls = new ArrayList<>();
        if (photosText != null && !photosText.isBlank()) {
            String[] lines = photosText.split("[\r\n,]+");
            for (String l : lines) {
                if (!l.trim().isEmpty()) {
                    imageUrls.add(l.trim());
                }
            }
        }
        return imageUrls;
    }
}
