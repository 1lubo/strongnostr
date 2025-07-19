package com.onelubo.strongnostr.nostr

import spock.lang.Specification

class NostrSignatureVerifierSpec extends Specification {


    def "valid Schnorr signature returns true"() {
        given: "valid public key, message, and signature"
        // Example test vector (replace with real valid values)
        def pubkeyHex = "63f1576a55f79a6cfd2c2b19412120f4cc1a1f48bace59ba4ca8e0675d52f2ba"
        def messageHex = "baf9e5f8f4fdb1460bc1922113437a9bae3d09bb268749ecc3523a9198fbabd4"
        def signatureHex = "221021f11bbf5f48803812e83ce8879fd103bd46fc0a5fb98e168d2d39641997fcdbccfff86943d1e1f36296a08b4a651acaf97932467d9e07675dd92b01c6f1"

        when: "verifying the signature"
        def result = NostrSignatureVerifier.verifySchnorrSignature(pubkeyHex, messageHex, signatureHex)

        then: "the signature verification should succeed"
        result
    }

    def "invalid Schnorr signature returns false"() {
        given: "invalid public key, message, and signature"
        def pubkeyHex = "f1c1a9e2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0"
        def messageHex = "48656c6c6f20576f726c64"
        def signatureHex = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

        when: "the signature verification should fail"
        def result = NostrSignatureVerifier.verifySchnorrSignature(pubkeyHex, messageHex, signatureHex)

        then: "the signature verification should fail"
        !result
    }

    def "invalid public key length throws exception"() {
        given: "invalid public key length"
        def pubkeyHex = "deadbeef" // too short
        def messageHex = "48656c6c6f20576f726c64"
        def signatureHex = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2" +
                "b1a2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2"

        when: "verifying the signature"
        def result = NostrSignatureVerifier.verifySchnorrSignature(pubkeyHex, messageHex, signatureHex)

        then: "verification should fail"
        !result
    }

    def "invalid signature length throws exception"() {
        given: "invalid signature length"
        def pubkeyHex = "f1c1a9e2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0"
        def messageHex = "48656c6c6f20576f726c64"
        def signatureHex = "deadbeef" // too short

        when: "verifying the signature"
        def result = NostrSignatureVerifier.verifySchnorrSignature(pubkeyHex, messageHex, signatureHex)

        then: "verification should fail"
        !result
    }
}
