package com.onelubo.strongnostr

import com.onelubo.strongnostr.dto.nostr.NostrAuthChallenge
import com.onelubo.strongnostr.dto.nostr.NostrAuthRequest
import com.onelubo.strongnostr.dto.nostr.NostrAuthResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class NostrAuthenticationSpec extends BaseNostrSpec {


    def "Complete Nostr authentication flow should work end-to-end"() {
        given: "a test user with Nostr keys"
        def nostrKeyPair = createNostrKeyPair()
        def npub = nostrKeyPair.getnPub()
        def nSecHex = nostrKeyPair.getnSecHex()

        when: "the user requests a challenge"
        ResponseEntity<NostrAuthChallenge> challengeResponse = restTemplate.postForEntity("${baseUrl}/api/v1/nostr/auth/challenge",
                [npub: npub],
                NostrAuthChallenge
        )

        then: "challenge is returned successfully"
        challengeResponse.getStatusCode() == HttpStatus.OK
        def challenge = challengeResponse.getBody()
        challenge.getChallenge() != null
        challenge.getMessage() != null
        challenge.getTimestamp() > 0

        when: "the user signs and submits the challenge"
        // Use the new method that properly signs the event ID
        def nostrEvent = createSignedNostrEvent(npub, nSecHex, challenge.getChallenge())

        def authRequest = [
                nostrEvent: nostrEvent,
                userProfile: createNostrUserProfile("fiatJaf@nostr.com", "fiatjaf", "https://example.com/avatar.png")
        ] as NostrAuthRequest

        ResponseEntity<NostrAuthResult> authResponse = restTemplate.postForEntity("${baseUrl}/api/v1/nostr/auth/login",
                authRequest,
                NostrAuthResult
        )

        then: "authentication is successful and JWT token is returned"
        authResponse.getStatusCode() == HttpStatus.OK
        def authResult = authResponse.getBody()
        authResult != null
        authResult.accessToken() != null
        authResult.refreshToken() != null
        authResult.success()
        authResult.message() == "Authentication successful"
        authResult.user() != null
    }
}
