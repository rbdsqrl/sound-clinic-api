package com.simplehearing.util;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RazorpaySignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Verifies Razorpay payment signature.
     * Expected signature = HMAC-SHA256(orderId + "|" + paymentId, secret)
     */
    public boolean verify(String razorpayOrderId, String razorpayPaymentId,
                          String signature, String secret) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String computed = hmacSha256(payload, secret);
            return computed.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSha256(String data, String secret)
        throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec key = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(key);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
