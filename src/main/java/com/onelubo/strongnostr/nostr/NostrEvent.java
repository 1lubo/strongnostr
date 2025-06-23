package com.onelubo.strongnostr.nostr;

public class NostrEvent {
    private String id;
    private String pubkey;
    private int kind;
    private long createdAt;
    private String content;
    private String Sig;

    public NostrEvent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPubkey() {
        return pubkey;
    }

    public void setPubkey(String pubkey) {
        this.pubkey = pubkey;
    }

    public int getKind() {
        return kind;
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSig() {
        return Sig;
    }

    public void setSig(String sig) {
        Sig = sig;
    }
}
