package com.rims.controller;

import com.rims.model.User;
import com.rims.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // --- BUYER AUTHENTICATION ---

    @GetMapping("/login/buyer")
    public String showBuyerLoginPage(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("role", "BUYER");
        model.addAttribute("roleTitle", "Customer / Buyer");
        model.addAttribute("error", error);
        return "auth/login-buyer";
    }

    @PostMapping("/login/buyer")
    public String processBuyerLogin(@RequestParam String email, 
                                    @RequestParam String password, 
                                    HttpServletRequest request,
                                    HttpSession session, 
                                    RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userService.authenticate(email, password, "ROLE_BUYER");
        if (userOpt.isPresent()) {
            request.changeSessionId(); // Protect against session fixation
            session.setAttribute("currentUser", userOpt.get());
            log.info("[Auth] Buyer logged in successfully: {}", email);
            return "redirect:/buyer/dashboard";
        }
        log.warn("[Auth Security] Failed login attempt for Buyer email: {}", email);
        redirectAttributes.addFlashAttribute("errorMessage", "Invalid Customer email or password. Please try again.");
        return "redirect:/login/buyer";
    }

    @GetMapping("/register/buyer")
    public String showBuyerRegisterPage(Model model) {
        model.addAttribute("role", "BUYER");
        return "auth/register-buyer";
    }

    @PostMapping("/register/buyer")
    public String processBuyerRegister(@RequestParam String name,
                                       @RequestParam String email,
                                       @RequestParam String password,
                                       @RequestParam(required = false) String phone,
                                       @RequestParam(required = false) String address,
                                       HttpServletRequest request,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        try {
            User newUser = userService.registerBuyer(name, email, password, phone, address);
            request.changeSessionId(); // Protect against session fixation
            session.setAttribute("currentUser", newUser);
            log.info("[Auth] New buyer registered: {}", email);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully! Welcome to RIMS.");
            return "redirect:/buyer/dashboard";
        } catch (Exception e) {
            log.error("[Auth Error] Buyer registration failed for email: {} - Reason: {}", email, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register/buyer";
        }
    }

    // --- SELLER AUTHENTICATION ---

    @GetMapping("/login/seller")
    public String showSellerLoginPage(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("role", "SELLER");
        model.addAttribute("roleTitle", "Seller Organization & Institution");
        model.addAttribute("error", error);
        return "auth/login-seller";
    }

    @PostMapping("/login/seller")
    public String processSellerLogin(@RequestParam String email, 
                                     @RequestParam String password, 
                                     HttpServletRequest request,
                                     HttpSession session, 
                                     RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userService.authenticate(email, password, "ROLE_SELLER");
        if (userOpt.isPresent()) {
            request.changeSessionId(); // Protect against session fixation
            session.setAttribute("currentUser", userOpt.get());
            log.info("[Auth] Seller logged in successfully: {}", email);
            return "redirect:/seller/dashboard";
        }
        log.warn("[Auth Security] Failed login attempt for Seller email: {}", email);
        redirectAttributes.addFlashAttribute("errorMessage", "Invalid Seller credentials. Please verify your email and password.");
        return "redirect:/login/seller";
    }

    @GetMapping("/register/seller")
    public String showSellerRegisterPage(Model model) {
        model.addAttribute("role", "SELLER");
        return "auth/register-seller";
    }

    @PostMapping("/register/seller")
    public String processSellerRegister(@RequestParam String name,
                                        @RequestParam String email,
                                        @RequestParam String password,
                                        @RequestParam String organizationName,
                                        @RequestParam(required = false) String registrationId,
                                        @RequestParam(required = false) String contactPerson,
                                        @RequestParam(required = false) String phone,
                                        @RequestParam(required = false) String address,
                                        HttpServletRequest request,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        try {
            User newSeller = userService.registerSeller(name, email, password, organizationName, 
                    registrationId, contactPerson, phone, address);
            request.changeSessionId(); // Protect against session fixation
            session.setAttribute("currentUser", newSeller);
            log.info("[Auth] New seller registered: {}", email);
            redirectAttributes.addFlashAttribute("successMessage", "Institutional Seller Account successfully registered!");
            return "redirect:/seller/dashboard";
        } catch (Exception e) {
            log.error("[Auth Error] Seller registration failed for email: {} - Reason: {}", email, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register/seller";
        }
    }

    // --- SUDO ADMIN AUTHENTICATION ---

    @GetMapping("/login/admin")
    public String showAdminLoginPage(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("role", "ADMIN");
        model.addAttribute("roleTitle", "SUDO Administrator");
        model.addAttribute("error", error);
        return "auth/login-admin";
    }

    @PostMapping("/login/admin")
    public String processAdminLogin(@RequestParam String email, 
                                    @RequestParam String password, 
                                    HttpServletRequest request,
                                    HttpSession session, 
                                    RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userService.authenticate(email, password, "ROLE_ADMIN");
        if (userOpt.isPresent()) {
            request.changeSessionId(); // Protect against session fixation
            session.setAttribute("currentUser", userOpt.get());
            log.info("[Auth] SUDO Admin logged in successfully: {}", email);
            return "redirect:/admin/dashboard";
        }
        log.warn("[Auth Security] FAILED SUDO Admin login attempt for email: {}", email);
        redirectAttributes.addFlashAttribute("errorMessage", "Access Denied: Invalid Administrative credentials.");
        return "redirect:/login/admin";
    }

    // --- LOGOUT ---

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("infoMessage", "You have been logged out securely.");
        return "redirect:/";
    }
}
