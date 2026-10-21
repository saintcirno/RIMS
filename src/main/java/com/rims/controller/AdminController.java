package com.rims.controller;

import com.rims.model.Item;
import com.rims.model.RentalOrder;
import com.rims.model.User;
import com.rims.service.ItemService;
import com.rims.service.RentalService;
import com.rims.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final ItemService itemService;
    private final RentalService rentalService;

    public AdminController(UserService userService, ItemService itemService, RentalService rentalService) {
        this.userService = userService;
        this.itemService = itemService;
        this.rentalService = rentalService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("currentUser");
        List<User> users = userService.getAllUsers();
        List<Item> items = itemService.getAllItemsForAdmin();
        List<RentalOrder> orders = rentalService.getAllOrders();

        BigDecimal platformVolume = orders.stream()
                .map(RentalOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("totalItems", items.size());
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("platformVolume", platformVolume);
        model.addAttribute("recentOrders", orders.size() > 5 ? orders.subList(0, 5) : orders);
        model.addAttribute("recentUsers", users.size() > 5 ? users.subList(0, 5) : users);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String viewAllUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("[Admin Action] Toggled user status for userId={}", id);
        userService.toggleUserActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "User account status toggled successfully.");
        return "redirect:/admin/users";
    }

    @GetMapping("/items")
    public String viewAllItems(Model model) {
        List<Item> items = itemService.getAllItemsForAdmin();
        model.addAttribute("items", items);
        return "admin/items";
    }

    @PostMapping("/items/{id}/remove")
    public String removeItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("[Admin Action] Delisting item id={}", id);
        itemService.deleteOrDelistItemByAdmin(id);
        redirectAttributes.addFlashAttribute("successMessage", "Item successfully delisted / removed from the public marketplace.");
        return "redirect:/admin/items";
    }

    @PostMapping("/items/{id}/delete-permanent")
    public String permanentlyDeleteItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            log.info("[Admin Action] Purging item id={}", id);
            itemService.permanentlyRemoveItemByAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage", "Item permanently purged from the database.");
        } catch (Exception e) {
            log.warn("[Admin Action] Item id={} cannot be purged (foreign key constraint), delisting instead", id);
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot purge item linked to existing orders. Item was delisted instead.");
            itemService.deleteOrDelistItemByAdmin(id);
        }
        return "redirect:/admin/items";
    }

    @GetMapping("/orders")
    public String viewAllOrders(Model model) {
        List<RentalOrder> orders = rentalService.getAllOrders();
        model.addAttribute("orders", orders);
        return "admin/orders";
    }
}
