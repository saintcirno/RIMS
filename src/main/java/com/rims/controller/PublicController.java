package com.rims.controller;

import com.rims.model.Category;
import com.rims.model.Item;
import com.rims.model.RentalCart;
import com.rims.service.CategoryService;
import com.rims.service.ItemService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class PublicController {

    private final ItemService itemService;
    private final CategoryService categoryService;

    public PublicController(ItemService itemService, CategoryService categoryService) {
        this.itemService = itemService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        List<Item> featuredItems = itemService.getAllActiveItems();
        if (featuredItems.size() > 6) {
            featuredItems = featuredItems.subList(0, 6);
        }
        model.addAttribute("categories", categories);
        model.addAttribute("featuredItems", featuredItems);
        return "public/index";
    }

    @GetMapping("/items")
    public String browseItems(@RequestParam(required = false) String category,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String tag,
                              Model model) {
        List<Item> items;
        if (tag != null && !tag.isBlank()) {
            items = itemService.getItemsByTag(tag.trim());
            model.addAttribute("activeTag", tag);
        } else if (q != null && !q.isBlank()) {
            items = itemService.search(q, category);
            model.addAttribute("searchQuery", q);
        } else if (category != null && !category.isBlank()) {
            items = itemService.getItemsByCategory(category);
            model.addAttribute("activeCategorySlug", category);
        } else {
            items = itemService.getAllActiveItems();
        }

        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("items", items);
        model.addAttribute("categories", categories);
        return "public/items";
    }

    @GetMapping("/items/{id}")
    public String itemDetail(@PathVariable Long id, Model model) {
        Optional<Item> itemOpt = itemService.findById(id);
        if (itemOpt.isEmpty() || !itemOpt.get().isActive()) {
            return "redirect:/items?error=not_found";
        }
        Item item = itemOpt.get();
        model.addAttribute("item", item);
        
        // Similar items in category
        List<Item> related = itemService.getItemsByCategory(item.getCategory().getSlug());
        related.removeIf(i -> i.getId().equals(item.getId()));
        if (related.size() > 3) {
            related = related.subList(0, 3);
        }
        model.addAttribute("relatedItems", related);

        return "public/item-detail";
    }

    // --- CART ACTIONS ---

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        RentalCart cart = getOrCreateCart(session);
        model.addAttribute("cart", cart);
        return "buyer/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long itemId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam(defaultValue = "3") int rentalDays,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Optional<Item> itemOpt = itemService.findById(itemId);
        if (itemOpt.isPresent() && itemOpt.get().isActive()) {
            Item item = itemOpt.get();
            RentalCart cart = getOrCreateCart(session);
            cart.addItem(item, quantity, rentalDays);
            redirectAttributes.addFlashAttribute("successMessage", "Added \"" + item.getName() + "\" to your rental cart.");
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long itemId,
                             @RequestParam int quantity,
                             @RequestParam int rentalDays,
                             HttpSession session) {
        RentalCart cart = getOrCreateCart(session);
        cart.updateItem(itemId, quantity, rentalDays);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId, HttpSession session, RedirectAttributes redirectAttributes) {
        RentalCart cart = getOrCreateCart(session);
        cart.removeItem(itemId);
        redirectAttributes.addFlashAttribute("infoMessage", "Item removed from cart.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        RentalCart cart = getOrCreateCart(session);
        cart.clear();
        return "redirect:/cart";
    }

    private RentalCart getOrCreateCart(HttpSession session) {
        RentalCart cart = (RentalCart) session.getAttribute("rentalCart");
        if (cart == null) {
            cart = new RentalCart();
            session.setAttribute("rentalCart", cart);
        }
        return cart;
    }
}
