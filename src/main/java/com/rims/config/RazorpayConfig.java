package com.rims.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key.id:rzp_test_RIMSdemoKey123}")
    private String keyId;

    @Value("${razorpay.key.secret:RIMSdemoSecret123}")
    private String keySecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    @Value("${razorpay.company.name:RIMS Equipment Rental}")
    private String companyName;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public void setKeySecret(String keySecret) {
        this.keySecret = keySecret;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    /**
     * Determines whether live/real Razorpay API keys have been provided by the user.
     */
    public boolean isLiveGatewayConfigured() {
        return keyId != null && !keyId.isBlank() 
                && !keyId.startsWith("rzp_test_RIMSdemo")
                && keySecret != null && !keySecret.isBlank() 
                && !keySecret.startsWith("RIMSdemoSecret");
    }
}
