package com.onelubo.strongnostr.nostr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Schema(description = "Nostr event for authentication")
public class NostrEvent {

    @Schema(description = "Event ID (SHA-256 hash of serialized event)")
    @NotBlank(message = "Event ID is required")
    private String id;

    @Schema(description = "Nostr public key in Nostr URI format (npub)")
    @NotBlank(message = "Public key is required")
    private String npub;

    @Schema(description = "Nostr public key in hex format (for serialization)")
    @NotBlank(message = "Public key hex is required")
    private String pubkey;

    @Schema(description = "Event kind")
    @NotNull(message = "Event kind is required")
    private Integer kind;

    @Schema(description = "Unix timestamp when the event was created (in seconds)")
    @NotNull(message = "Creation timestamp is required")
    @Positive(message = "Creation timestamp must be a positive number")
    @JsonProperty("created_at")
    private Long createdAt;

    @Schema(description = "Event content")
    @NotNull(message = "Content is required")
    private String content;

    @Schema(description = "BIP340 Schnorr signature of the event ID")
    @NotBlank(message = "Signature is required")
    private String signature;

    @Schema(description = "Event tags")
    private List<List<String>> tags;

    public NostrEvent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNpub() {
        return npub;
    }

    public void setNpub(String npub) {
        this.npub = npub;
    }

    public int getKind() {
        return kind;
    }

    public void setKind(Integer kind) {
        if (kind != null) {
            this.kind = kind;
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        if (createdAt != null) {
            this.createdAt = createdAt;
        }
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public List<List<String>> getTags() {
        return tags;
    }

    public void setTags(List<List<String>> tags) {
        this.tags = tags;
    }

    public String getPubkey() { return pubkey; }

    public void setPubkey(String pubkey) {
        this.pubkey = pubkey;
    }

    @Override
    public String toString() {
        return "NostrEvent{" +
                "id='" + id + '\'' +
                ", npub='" + npub + '\'' +
                ", kind=" + kind +
                ", createdAt=" + createdAt +
                ", content='" + content + '\'' +
                ", signature='" + signature + '\'' +
                ", tags=" + tags +
                '}';
    }
}
