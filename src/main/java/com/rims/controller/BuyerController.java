package com.rims.controller;

import com.rims.config.RazorpayConfig;
import com.rims.model.RentalCart;
import com.rims.model.RentalOrder;
import com.rims.model.User;
import com.rims.service.PaymentService;
import com.rims.service.RentalService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class BuyerController {

    private static final Logger log = LoggerFactory.getLogger(BuyerController.class);

    private final RentalService rentalService;
    private final PaymentService paymentService;
    private final RazorpayConfig razorpayConfig;

    public BuyerController(RentalService rentalService,
                           PaymentService paymentService,
                           RazorpayConfig razorpayConfig) {
        this.rentalService = rentalService;
        this.paymentService = paymentService;
        this.razorpayConfig = razorpayConfig;
    }

    @GetMapping("/buyer/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User buyer = (User) session.getAttribute("currentUser");
        List<RentalOrder> orders = rentalService.getOrdersByBuyer(buyer);

        long activeRentalsCount = orders.stream()
                .filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus()) || "ACTIVE".equalsIgnoreCase(o.getStatus()))
                .count();

        model.addAttribute("buyer", buyer);
        model.addAttribute("orders", orders);
        model.addAttribute("activeRentalsCount", activeRentalsCount);
        model.addAttribute("totalOrdersCount", orders.size());

        return "buyer/dashboard";
    }

    @GetMapping("/checkout")
    public String showCheckoutPage(HttpSession session, Model model) {
        RentalCart cart = (RentalCart) session.getAttribute("rentalCart");
        if (cart == null || cart.isEmpty()) {
            return "redirect:/cart?empty=true";
        }
        User buyer = (User) session.getAttribute("currentUser");
        model.addAttribute("cart", cart);
        model.addAttribute("buyer", buyer);
        model.addAttribute("defaultStartDate", LocalDate.now().plusDays(1));
        model.addAttribute("razorpayKeyId", razorpayConfig.getKeyId());
        model.addAttribute("isLiveGateway", razorpayConfig.isLiveGatewayConfigured());
        model.addAttribute("companyName", razorpayConfig.getCompanyName());
        model.addAttribute("currency", razorpayConfig.getCurrency());
        return "buyer/checkout";
    }

    @PostMapping("/checkout/initiate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> initiateRazorpayOrder(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "1") int rentalDays,
            @RequestParam(required = false) String notes,
            HttpSession session) {

        User buyer = (User) session.getAttribute("currentUser");
        RentalCart cart = (RentalCart) session.getAttribute("rentalCart");

        if (cart == null || cart.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Rental cart is empty.");
            return ResponseEntity.badRequest().body(err);
        }

        if (rentalDays > 0) {
            cart.setRentalDays(rentalDays);
        }

        try {
            RentalOrder order = rentalService.createPendingOrder(buyer, cart, startDate, rentalDays, notes);
            Map<String, Object> rzpData = paymentService.createRazorpayOrder(order, buyer);
            rzpData.put("success", true);
            rzpData.put("orderId", order.getId());
            rzpData.put("orderCode", order.getOrderCode());
            return ResponseEntity.ok(rzpData);
        } catch (Exception e) {
            log.error("[Checkout Error] Failed to initiate Razorpay order", e);
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Failed to initiate transaction: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    @PostMapping("/checkout/verify-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @RequestBody Map<String, String> payload,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        User buyer = (User) session.getAttribute("currentUser");

        try {
            Long orderId = Long.valueOf(payload.get("orderId"));
            String razorpayPaymentId = payload.get("razorpayPaymentId");
            String razorpayOrderId = payload.get("razorpayOrderId");
            String razorpaySignature = payload.get("razorpaySignature");

            // Access Control: Verify ownership of order
            Optional<RentalOrder> orderOpt = rentalService.findById(orderId);
            if (orderOpt.isEmpty() || !orderOpt.get().getBuyer().getId().equals(buyer.getId())) {
                log.warn("[Security Alert] IDOR attempt or invalid order access: userId={} attempted to verify orderId={}", 
                        buyer != null ? buyer.getId() : "anonymous", orderId);
                response.put("success", false);
                response.put("message", "Unauthorized access: You do not own this order.");
                return ResponseEntity.status(403).body(response);
            }

            boolean isValid = paymentService.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
            if (!isValid) {
                log.warn("[Payment Security] Razorpay signature mismatch for orderId={}, paymentId={}", orderId, razorpayPaymentId);
                response.put("success", false);
                response.put("message", "Razorpay payment cryptographic signature verification failed.");
                return ResponseEntity.badRequest().body(response);
            }

            rentalService.confirmPaidOrder(orderId, razorpayPaymentId, razorpayOrderId, razorpaySignature);

            // Clear the cart upon successful verification
            RentalCart cart = (RentalCart) session.getAttribute("rentalCart");
            if (cart != null) {
                cart.clear();
            }

            response.put("success", true);
            response.put("redirectUrl", "/buyer/orders/" + orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[Payment Error] Exception verifying transaction", e);
            response.put("success", false);
            response.put("message", "Error verifying transaction: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                  @RequestParam(defaultValue = "1") int rentalDays,
                                  @RequestParam(required = false) String notes,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User buyer = (User) session.getAttribute("currentUser");
        RentalCart cart = (RentalCart) session.getAttribute("rentalCart");

        if (cart == null || cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cart is empty.");
            return "redirect:/cart";
        }

        if (rentalDays > 0) {
            cart.setRentalDays(rentalDays);
        }

        try {
            RentalOrder order = rentalService.processCheckout(buyer, cart, startDate, rentalDays, notes);
            // Record simulated transaction ID for direct fallback
            order.setRazorpayPaymentId("pay_direct_" + System.currentTimeMillis());
            order.setPaymentStatus("PAID");
            rentalService.updateOrderStatus(order.getId(), "CONFIRMED");
            cart.clear(); // Clear the cart upon success
            redirectAttributes.addFlashAttribute("successOrder", true);
            return "redirect:/buyer/orders/" + order.getId();
        } catch (Exception e) {
            log.error("[Checkout Error] Direct checkout failed", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Checkout failed: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/buyer/orders/{id}")
    public String viewOrderReceipt(@PathVariable Long id, HttpSession session, Model model) {
        User buyer = (User) session.getAttribute("currentUser");
        Optional<RentalOrder> orderOpt = rentalService.findById(id);

        if (orderOpt.isEmpty() || !orderOpt.get().getBuyer().getId().equals(buyer.getId())) {
            log.warn("[Security Alert] Unauthorized access to order receipt id={} by user={}", id, buyer != null ? buyer.getId() : "null");
            return "redirect:/buyer/dashboard?error=not_found";
        }

        RentalOrder order = orderOpt.get();
        model.addAttribute("order", order);
        model.addAttribute("companyName", razorpayConfig.getCompanyName());
        return "buyer/order-receipt";
    }

    @PostMapping("/buyer/orders/{id}/return")
    public String requestReturn(@PathVariable Long id,
                                @RequestParam(required = false) String returnNotes,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User buyer = (User) session.getAttribute("currentUser");
        try {
            rentalService.requestReturn(id, buyer, returnNotes);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Equipment return initiated! The vendor has been alerted to inspect and check in the hardware units.");
        } catch (Exception e) {
            log.error("[Return Error] Failed to initiate return for order id={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to initiate return: " + e.getMessage());
        }
        return "redirect:/buyer/orders/" + id;
    }
}

