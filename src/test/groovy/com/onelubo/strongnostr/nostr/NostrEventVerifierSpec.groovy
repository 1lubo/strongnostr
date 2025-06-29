package com.onelubo.strongnostr.nostr

import com.onelubo.strongnostr.service.nostr.NostrAuthenticationService
import com.onelubo.strongnostr.dto.nostr.NostrAuthChallenge
import com.onelubo.strongnostr.util.NostrUtils
import spock.lang.Specification

import java.time.Instant

class NostrEventVerifierSpec extends Specification {

    NostrEventVerifier verifier
    NostrKeyManager nostrKeyManager

    def setup() {
        nostrKeyManager = new NostrKeyManager()
        verifier = new NostrEventVerifier(nostrKeyManager)
    }

    def "should validate a valid event"() {
        given: "A valid Nostr event with a valid NPUB and kind"
        def nostrKeyPair = nostrKeyManager.generateKeyPair()
        def npub = nostrKeyPair.getnPub()
        def nSecHex = nostrKeyPair.getnSecHex()
        def id = UUID.randomUUID().toString()
        def challenge = NostrAuthenticationService.CHALLENGE_PREFIX + id
        long timestamp = Instant.now().getEpochSecond()
        def challengeRequest = new NostrAuthChallenge(id, challenge, timestamp)
        def event = NostrUtils.createSignedNostrEvent(npub, nSecHex, challengeRequest.getChallenge())

        when: "Verifying the event"
        def result = verifier.verifyEventSignature(event)

        then: "Verification should succeed"
        result
    }

    def "should fail to validate an event with an invalid signature"() {
        given: "A valid Nostr event with an invalid signature"
        def nostrKeyPair = nostrKeyManager.generateKeyPair()
        def npub = nostrKeyPair.getnPub()
        def nSecHex = nostrKeyPair.getnSecHex()
        def id = UUID.randomUUID().toString()
        def challenge = NostrAuthenticationService.CHALLENGE_PREFIX + id
        long timestamp = Instant.now().getEpochSecond()
        def challengeRequest = new NostrAuthChallenge(id, challenge, timestamp)
        def event = NostrUtils.createSignedNostrEvent(npub, nSecHex, challengeRequest.getChallenge())
        event.setSignature(NostrUtils.INVALID_SIGNATURE)

        when: "Verifying the event"
        def result = verifier.verifyEventSignature(event)

        then: "Verification should fail"
        !result
    }
}
