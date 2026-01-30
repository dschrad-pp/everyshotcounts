package com.lektralabs.thrones.crm;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Security service for CRM webhook authentication
 * Supports both API Key and HMAC signature verification
 */
@ApplicationScoped
public class WebhookSecurityService {
    private static final Logger logger = Logger.getLogger(WebhookSecurityService.class);
    
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String API_KEY_HEADER = "X-CRM-Webhook-Key";
    private static final String SIGNATURE_HEADER = "X-CRM-Webhook-Signature";
    
    @ConfigProperty(name = "crm.webhook.api.key", defaultValue = "")
    String apiKey;
    
    @ConfigProperty(name = "crm.webhook.secret", defaultValue = "")
    String webhookSecret;
    
    @ConfigProperty(name = "crm.webhook.security.enabled", defaultValue = "true")
    boolean securityEnabled;
    
    /**
     * Validates API key from header
     * @param providedApiKey The API key provided in the request header
     * @return true if valid, false otherwise
     */
    public boolean validateApiKey(String providedApiKey) {
        if (!securityEnabled) {
            logger.warn("Webhook security is disabled - allowing request");
            return true;
        }
        
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("Webhook API key is not configured in application.properties");
            return false;
        }
        
        if (providedApiKey == null || providedApiKey.isEmpty()) {
            logger.warn("No API key provided in request");
            return false;
        }
        
        // Use constant-time comparison to prevent timing attacks
        boolean isValid = constantTimeEquals(apiKey, providedApiKey);
        
        if (!isValid) {
            logger.warn("Invalid API key provided");
        }
        
        return isValid;
    }
    
    /**
     * Validates HMAC signature from header
     * @param signature The signature from X-CRM-Webhook-Signature header
     * @param payload The request body as string
     * @return true if valid, false otherwise
     */
    public boolean validateHmacSignature(String signature, String payload) {
        if (!securityEnabled) {
            logger.warn("Webhook security is disabled - allowing request");
            return true;
        }
        
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            logger.error("Webhook secret is not configured in application.properties");
            return false;
        }
        
        if (signature == null || signature.isEmpty()) {
            logger.warn("No signature provided in request");
            return false;
        }
        
        if (payload == null) {
            logger.warn("No payload provided for signature verification");
            return false;
        }
        
        try {
            String expectedSignature = calculateHmac(payload, webhookSecret);
            boolean isValid = constantTimeEquals(signature, expectedSignature);
            
            if (!isValid) {
                logger.warn("Invalid HMAC signature provided");
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating HMAC signature", e);
            return false;
        }
    }
    
    /**
     * Calculate HMAC-SHA256 signature
     */
    private String calculateHmac(String payload, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
    
    public String getApiKeyHeaderName() {
        return API_KEY_HEADER;
    }
    
    public String getSignatureHeaderName() {
        return SIGNATURE_HEADER;
    }
}
