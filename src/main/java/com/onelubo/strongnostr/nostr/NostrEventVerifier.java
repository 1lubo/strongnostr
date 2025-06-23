package com.onelubo.strongnostr.nostr;

import org.springframework.stereotype.Component;

@Component
public class NostrEventVerifier {

    public boolean verifyEventSignature(NostrEvent nostrEvent) {
        return true;
    }
}
