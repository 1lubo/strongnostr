package com.onelubo.strongnostr.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "users")
public class User {
    @Id
    private String id;

    @NotBlank(message = "Username is required")
    @Indexed(unique = true)
    private String username;

    /**
     * Nostr public key in npub format (bech32 encoded)
     * This is the PRIMARY identifier - required for all users
     * Example: npub1abc123def456...
     */
    @NotBlank(message = "Nostr public key is required")
    @Indexed(unique = true)
    private String nostrPublicKey;

    /**
     * Nostr public key in hex format (for internal operations & verification)
     * Example: 02a1b2c3d4e5f6789...
     */
    @NotBlank(message = "Nostr public key hex is required")
    @Indexed(unique = true)
    private String nostrPublicKeyHex;

    /**
     * Nostr profile metadata (NIP-01 standard)
     */
//    private NostrProfile nostrProfile;
}
