package com.onelubo.strongnostr.nostr

import com.onelubo.strongnostr.util.NostrUtils
import spock.lang.Specification

class NostrEventVerifierSpec extends Specification {

    NostrEventVerifier verifier

    def setup() {
        verifier = new NostrEventVerifier()
    }

    def "should validate a valid event"() {
        given: "A valid Nostr event with a valid NPUB and kind"
        def event = NostrUtils.createValidAuthEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_EVENT_KIND)

        when: "Verifying the event"
        def result = verifier.verifyEventSignature(event)

        then: "Verification should succeed"
        result
    }

    def "should fail to validate an event with an invalid signature"() {
        given: "A valid Nostr event with an invalid signature"
        def event = NostrUtils.createValidAuthEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_EVENT_KIND)
        event.setSignature(NostrUtils.INVALID_SIGNATURE)

        when: "Verifying the event"
        def result = verifier.verifyEventSignature(event)

        then: "Verification should fail"
        !result
    }
}
