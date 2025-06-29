package com.onelubo.strongnostr.nostr;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class NostrEvent {

    @NotBlank(message = "Event ID is required")
    private String id;

    @NotBlank(message = "Public key is required")
    private String npub;

    @NotNull(message = "Event kind is required")
    private Integer kind;

    @NotNull(message = "Creation timestamp is required")
    @Positive(message = "Creation timestamp must be a positive number")
    @JsonProperty("created_at")
    private Long createdAt;

    @NotNull(message = "Content is required")
    private String content;

    @NotBlank(message = "Signature is required")
    private String signature;

    private List<List<String>> tags;

    public NostrEvent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getnPub() {
        return npub;
    }

    public void setnPub(String npub) {
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

    public boolean isAuthEvent() {
        return kind != null && kind == 22242; // NIP-46 authentication event kind
    }

}
