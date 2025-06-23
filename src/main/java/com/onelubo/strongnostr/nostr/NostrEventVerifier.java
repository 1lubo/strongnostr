package com.onelubo.strongnostr.nostr;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.HexFormat;
import java.util.List;

@Component
public class NostrEventVerifier {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public boolean verifyEventSignature(NostrEvent nostrEvent) {
        try {
            if (!isValidEventStructure(nostrEvent)) {
                return false;
            }
            
            String computedId = computedEventId(nostrEvent);
            
            if (!computedId.equals(nostrEvent.getId())) {
                return false;
            }
            
            return verifySignature(computedId, nostrEvent.getSignature(), nostrEvent.getPubkey());
        } catch (Exception e) {
            return  false;
        }
    }
    
    public String computedEventId(NostrEvent nostrEvent) throws NoSuchAlgorithmException {
        try {
            String serialized = serializedEventForId(nostrEvent);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(serialized.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute event ID", e);
        }
    }
    
    public static String serializedEventForId(NostrEvent nostrEvent) {
        StringBuilder sb = new StringBuilder();
        sb.append("[0,\"");
        sb.append(nostrEvent.getPubkey());
        sb.append("\",");
        sb.append(nostrEvent.getCreatedAt());
        sb.append(",");
        sb.append(nostrEvent.getKind());
        sb.append(",");
        sb.append(serializeTags(nostrEvent.getTags()));
        sb.append(",\"");
        sb.append(escapeString(nostrEvent.getContent()));
        sb.append("\"]");
        
        return  sb.toString();
    }
    
    private boolean verifySignature(String message, String signatureHex, String pubkeyHex) {
        try {
            if (message == null || signatureHex == null || pubkeyHex == null) {
                return false;
            }
            
            if (!isValidHexString(signatureHex) || !isValidHexString(pubkeyHex)) {
                return  false;
            }
            
            byte [] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            byte [] signatureBytes = HexFormat.of().parseHex(signatureHex);
            byte [] pubkeyBytes = HexFormat.of().parseHex(pubkeyHex);
            
            if (signatureBytes.length < 64 || pubkeyBytes.length < 32) {
                return false;
            }

            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            //TODO: Real implementation should handle secp256k1 key format and Schnorr signature verification
            
            return isValidSignatureFormat(signatureHex) && isValidPubKeyFormat(pubkeyHex);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidEventStructure(NostrEvent nostrEvent) {
        return nostrEvent != null &&
                nostrEvent.getId() != null && !nostrEvent.getId().trim().isEmpty() &&
                nostrEvent.getPubkey() != null && !nostrEvent.getPubkey().trim().isEmpty() &&
                nostrEvent.getSignature() != null && !nostrEvent.getSignature().trim().isEmpty() &&
                nostrEvent.getContent() != null &&
                nostrEvent.getCreatedAt() > 0;
    }
    
    private static String serializeTags(List<List<String>> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            List<String> tag = tags.get(i);
            sb.append("[");
            
            for (int j = 0; j < tag.size(); j++) {
                if (j > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(escapeString(tag.get(j))).append("\"");
            }
            sb.append("]");
        }
        sb.append("]");
        
        return sb.toString();
    }
    
    private static String escapeString(String str) {
        if (str == null) {
            return "";
        }
        
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private boolean isValidHexString(String hex) {
        return hex != null && hex.matches("^[0-9a-fA-F]+$") && hex.length() % 2 == 0;
    }
    
    private boolean isValidSignatureFormat(String signature) {
        return isValidHexString(signature) && signature.length() == 128; // 64 bytes in hex
    }
    
    private boolean isValidPubKeyFormat(String pubkey) {
        return isValidHexString(pubkey) && pubkey.length() == 64; // 32 bytes in hex
    }
}
