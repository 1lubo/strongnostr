package com.onelubo.strongnostr.rest.nostr;

import com.onelubo.strongnostr.dto.nostr.NostrAuthChallenge;
import com.onelubo.strongnostr.dto.nostr.NostrAuthRequest;
import com.onelubo.strongnostr.dto.nostr.NostrAuthResult;
import com.onelubo.strongnostr.service.nostr.NostrAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/nostr/auth")
public class NostrAuthController {

    Logger logger = LoggerFactory.getLogger(NostrAuthController.class);

    private final NostrAuthenticationService nostrAuthenticationService;

    public NostrAuthController(NostrAuthenticationService nostrAuthenticationService) {
        this.nostrAuthenticationService = nostrAuthenticationService;
    }

    @PostMapping("/challenge")
    public ResponseEntity<NostrAuthChallenge> createChallenge() {
        NostrAuthChallenge challenge = nostrAuthenticationService.generateAuthChallenge();
        return  ResponseEntity.ok(challenge);
    }

    @PostMapping("/login")
    public ResponseEntity<NostrAuthResult> loginWithNostrEvent(@RequestBody NostrAuthRequest nostrAuthRequest) {
        NostrAuthResult nostrAuthResult = nostrAuthenticationService.authenticateWithNostrEvent(nostrAuthRequest);
        if (nostrAuthResult.success()) {
            return ResponseEntity.ok(nostrAuthResult);
        } else {
            logger.info("Authentication failed: {}", nostrAuthResult.message());
            return ResponseEntity.badRequest().body(nostrAuthResult);
        }
    }
}
