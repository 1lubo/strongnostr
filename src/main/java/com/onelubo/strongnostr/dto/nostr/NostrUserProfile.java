package com.onelubo.strongnostr.dto.nostr;

public class NostrUserProfile {
    private String name;
    private String about;
    private String avatarUrl;
    private String nip05;
    private String lud16;

    public NostrUserProfile() {
    }

    public String getName() {
        return name;
    }

    public String getAbout() {
        return about;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getNip05() {
        return nip05;
    }

    public String getLud16() {
        return lud16;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setNip05(String nip05) {
        this.nip05 = nip05;
    }

    public void setLud16(String lud16) {
        this.lud16 = lud16;
    }
}
