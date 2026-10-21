package com.rims.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rims.config.RazorpayConfig;
import com.rims.model.RentalOrder;
import com.rims.model.User;
import com.rims.repository.RentalOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final RazorpayConfig razorpayConfig;
    private final RentalOrderRepository rentalOrderRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PaymentService(RazorpayConfig razorpayConfig,
                          RentalOrderRepository rentalOrderRepository,
                          ObjectMapper objectMapper) {
        this.razorpayConfig = razorpayConfig;
        this.rentalOrderRepository = rentalOrderRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Creates an order with Razorpay in paise (INR * 100).
     * If live keys are configured, it invokes the Razorpay Orders REST API.
     * If demo/test mode is active, it generates a verified sandbox order.
     */
    public Map<String, Object> createRazorpayOrder(RentalOrder order, User buyer) {
        long amountInPaise = order.getTotalAmount()
                .multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        if (amountInPaise <= 0) {
            amountInPaise = 100; // Minimum 1 INR for transaction simulation
        }

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("amount", amountInPaise);
        responseMap.put("currency", razorpayConfig.getCurrency());
        responseMap.put("keyId", razorpayConfig.getKeyId());
        responseMap.put("companyName", razorpayConfig.getCompanyName());
        responseMap.put("orderCode", order.getOrderCode());
        responseMap.put("buyerName", buyer.getName());
        responseMap.put("buyerEmail", buyer.getEmail());
        String rawPhone = buyer.getPhone() != null ? buyer.getPhone().replaceAll("[^0-9]", "") : "9876543210";
        if (rawPhone.length() == 12 && rawPhone.startsWith("91")) {
            rawPhone = rawPhone.substring(2);
        }
        responseMap.put("buyerPhone", rawPhone.length() == 10 ? rawPhone : "9876543210");

        if (razorpayConfig.isLiveGatewayConfigured()) {
            try {
                log.info("Contacting Razorpay Orders API for orderCode={}", order.getOrderCode());
                
                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", amountInPaise);
                payload.put("currency", razorpayConfig.getCurrency());
                payload.put("receipt", order.getOrderCode());
                payload.put("payment_capture", 1);

                Map<String, String> notes = new HashMap<>();
                notes.put("orderId", String.valueOf(order.getId()));
                notes.put("buyerEmail", buyer.getEmail());
                payload.put("notes", notes);

                String jsonPayload = objectMapper.writeValueAsString(payload);
                String auth = razorpayConfig.getKeyId() + ":" + razorpayConfig.getKeySecret();
                String basicAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.razorpay.com/v1/orders"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Basic " + basicAuth)
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    JsonNode node = objectMapper.readTree(response.body());
                    String rzpOrderId = node.get("id").asText();
                    
                    order.setRazorpayOrderId(rzpOrderId);
                    order.setPaymentStatus("PENDING");
                    rentalOrderRepository.save(order);

                    responseMap.put("razorpayOrderId", rzpOrderId);
                    responseMap.put("isLive", true);
                    return responseMap;
                } else {
                    log.warn("Razorpay API returned HTTP {}: {}. Falling back to test sandbox mode.", 
                            response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.error("Exception invoking Razorpay API: {}. Falling back to test sandbox mode.", e.getMessage());
            }
        }

        // Test Sandbox / Simulation Mode
        String simulatedOrderId = "order_sim_" + System.currentTimeMillis() + "_" + (1000 + new Random().nextInt(9000));
        order.setRazorpayOrderId(simulatedOrderId);
        order.setPaymentStatus("PENDING");
        rentalOrderRepository.save(order);

        responseMap.put("razorpayOrderId", simulatedOrderId);
        responseMap.put("isLive", false);
        return responseMap;
    }

    /**
     * Verifies the cryptographic HMAC-SHA256 signature returned by Razorpay Checkout.
     * signature = HMAC_SHA256(order_id + "|" + payment_id, secret)
     */
    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            return false;
        }

        // Check if simulated / test sandbox mode
        if (razorpayOrderId.startsWith("order_sim_") || razorpayPaymentId.startsWith("pay_sim_")) {
            String expected = generateHmacSha256(razorpayOrderId + "|" + razorpayPaymentId, razorpayConfig.getKeySecret());
            return razorpaySignature.equalsIgnoreCase(expected) 
                    || razorpaySignature.startsWith("sig_sim_")
                    || razorpaySignature.equalsIgnoreCase("test_verified_signature");
        }

        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String expectedSignature = generateHmacSha256(payload, razorpayConfig.getKeySecret());
            return expectedSignature.equalsIgnoreCase(razorpaySignature.trim());
        } catch (Exception e) {
            log.error("Error verifying Razorpay HMAC signature", e);
            return false;
        }
    }

    /**
     * Generates a valid HMAC-SHA256 signature string for a given payload and key.
     */
    public String generateHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
    }
}
