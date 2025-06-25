package com.onelubo.strongnostr.nostr

import com.onelubo.strongnostr.util.NostrUtils
import spock.lang.Specification

class NostrKeyManagerSpec extends Specification {

    NostrKeyManager nostrKeyManager

    def setup() {
        nostrKeyManager = new NostrKeyManager()
    }

    def "should generate valid key pair"() {
        when: "a new key pair is generated"
        def keyPair = nostrKeyManager.generateKeyPair()

        then: "the key pair should be valid"
        keyPair != null
        keyPair.publicKey != null
        keyPair.publicKey.length() > 0
        keyPair.publicKey.startsWith("npub1")
        keyPair.privateKey != null
        keyPair.privateKey.length() > 0
        keyPair.privateKey.startsWith("nsec1")
        NostrUtils.isValidHex(keyPair.publicKeyHex)
        NostrUtils.isValidHex(keyPair.privateKeyHex)
        keyPair.publicKey.length() == 63
        keyPair.privateKey.length() == 63
        keyPair.publicKeyHex.length() == 64
        keyPair.privateKeyHex.length() == 64
    }

    def "should generate unique key pairs"() {
        when: "multiple key pairs are generated"
        def keyPairs = (1..10).collect { nostrKeyManager.generateKeyPair() }

        then: "all key pairs should be unique"
        keyPairs.size() == keyPairs.unique().size()
    }

    

    def "should generate secp256k1 keys"() {
        when: "a new key pair is generated"
        def keyPair = nostrKeyManager.generateKeyPair()

        then: "the keys should be valid secp256k1 curve keys"
        //private key is not the all-zero value (invalid) or the curve order (also invalid)
        "0000000000000000000000000000000000000000000000000000000000000000" != keyPair.getPrivateKeyHex()
        "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141" != keyPair.getPrivateKeyHex()

        and: "the public key is derived correctly from the private key"
        def derivedPublicKey = nostrKeyManager.derivePublicKeyFromPrivate(keyPair.privateKeyHex)
        derivedPublicKey == keyPair.publicKeyHex
    }

    def "should convert npub to hex"() {
        when: "converting a valid npub hex string"
        def hex = nostrKeyManager.npubToHex(NostrUtils.VALID_NPUB)

        then: "the hex should match the expected public key hex"
        hex != null
        hex.length() == 64
        NostrUtils.isValidHex(hex)
        hex == NostrUtils.VALID_PUBLIC_KEY_HEX
    }

    def "should convert hex to npub"() {
        when: "converting a valid public key hex string"
        def npub = nostrKeyManager.hexToNpub(NostrUtils.VALID_PUBLIC_KEY_HEX)

        then: "the npub should match the expected npub string"
        npub != null
        npub.length() == 63
        npub.startsWith("npub1")
        npub == NostrUtils.VALID_NPUB
    }

    def "should handle round trip conversion"() {
        when: "converting hex to npub and back"
        def npub = nostrKeyManager.hexToNpub(NostrUtils.VALID_PUBLIC_KEY_HEX)
        def backToHex = nostrKeyManager.npubToHex(npub)

        then: "the npub to hex and back should yield the same hex"
        NostrUtils.VALID_PUBLIC_KEY_HEX.toLowerCase() == backToHex.toLowerCase()
    }

    def "should handle round trip npub conversion"() {
        when: "converting npub to hex and back"
        def hex = nostrKeyManager.npubToHex(NostrUtils.VALID_NPUB)
        def backToNpub = nostrKeyManager.hexToNpub(hex)

        then: "the npub to hex and back should yield the same npub"
        NostrUtils.VALID_NPUB == backToNpub
    }

    def "should convert nsec to hex"() {
        when: "converting a valid nsec to hex"
        def hex = nostrKeyManager.nsecToHex(NostrUtils.VALID_NSEC)

        then: "the hex should match the expected private key hex"
        hex != null
        hex.length() == 64
        NostrUtils.isValidHex(hex)
        hex == NostrUtils.VALID_PRIVATE_KEY_HEX
    }

    def "should convert hex to nsec"() {
        when: "converting a valid private key hex to nsec"
        def nsec = nostrKeyManager.hexToNsec(NostrUtils.VALID_PRIVATE_KEY_HEX)

        then: "the nsec should match the expected nsec string"
        nsec != null
        nsec.length() == 63
        nsec.startsWith("nsec1")
        nsec == NostrUtils.VALID_NSEC
    }

    def "should validate valid npub"() {
        when: "valid npub is checked"
        def isValid = nostrKeyManager.isValidNostrPublicKey(NostrUtils.VALID_NPUB)

        then: "it should return true"
        isValid
    }

    def "should validate valid hex public key"() {
        when: "valid hex public key is checked"
        def isValid = nostrKeyManager.isValidNostrPublicKey(NostrUtils.VALID_PUBLIC_KEY_HEX)

        then: "it should return true"
        isValid
    }

    def "should reject invalid npub"() {
        when: "invalid npub is checked"
        def isValid = nostrKeyManager.isValidNostrPublicKey(invalidNpub)

        then: "it should return false"
        !isValid

        where: "invalid npub values"
        invalidNpub << [
                "npub1",              // Too short
                "npub1too_long_key_that_exceeds_the_length_limit", // Too long
                "npub1invalidcharacters!@#\$%^&*()", // Contains invalid characters
                null, // Null value
                "" // Empty string
        ]
    }

    def "rejects invalid hex public key"() {
        when: "invalid hex public key is checked"
        def isValid = nostrKeyManager.isValidNostrPublicKey(invalidHex)

        then: "it should return false"
        !isValid

        where: "invalid hex public key values"
        invalidHex << [
                "123", // Too short
                "invalidHex",
                "abcdefghijhklmnop" // Contains invalid characters
        ]
    }

    def "should validate private key format"() {
        when: "valid private key is checked"
        def isValid = nostrKeyManager.isValidNostrPrivateKey(NostrUtils.VALID_NSEC)

        then: "it should return true"
        isValid
    }

    def "should validate private key hex"() {
        when: "valid private key hex is checked"
        def isValid = nostrKeyManager.isValidNostrPrivateKey(NostrUtils.VALID_PRIVATE_KEY_HEX)

        then: "it should return true"
        isValid
    }

    def "should reject invalid nsec"() {
        when: "invalid nsec is checked"
        def isValid = nostrKeyManager.isValidNostrPrivateKey(invalidNsec)

        then: "it should return false"
        !isValid

        where: "invalid nsec values"
        invalidNsec << [
                "nsec1",              // Too short
                "nsec1too_long_key_that_exceeds_the_length_limit", // Too long
                "nsec1invalidcharacters!@#\$%^&*()", // Contains invalid characters
                null, // Null value
                "" // Empty string
        ]
    }

    def "should reject invalid hex private key"() {
        when: "invalid hex private key is checked"
        def isValid = nostrKeyManager.isValidNostrPrivateKey(invalidHex)

        then: "it should return false"
        !isValid

        where: "invalid hex private key values"
        invalidHex << [
                "123", // Too short
                "invalidHex",
                "abcdefghijhklmnop" // Contains invalid characters
        ]
    }

    def "should encode bech 32"() {
        given: "some simple test data"
        byte[] testData = [0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A]

        when: "encoding the data to bech32 with 'test' prefix"
        def encoded = nostrKeyManager.encodeBech32("test", testData)

        then: "the encoded string should not be null or empty"
        encoded != null
        !encoded.isEmpty()
        encoded.startsWith("test1")

        and: "should decode back to the original data"
        def decoded = nostrKeyManager.decodeBech32(encoded)
        decoded.hrp() == "test"
        decoded.data() == testData
    }

    def "should encode and decode bech32 with valid data"() {
        given: "a valid npub string"
        byte[] data = NostrUtils.hexStringToByteArray(NostrUtils.VALID_PUBLIC_KEY_HEX)

        when: "encoding the npub to bech32"
        def encoded = nostrKeyManager.encodeBech32("npub", data)

        then: "the encoded string should not be null or empty"
        encoded != null
        !encoded.isEmpty()
        encoded.startsWith("npub1")

        and: "should decode back to the original npub"
        def decoded = nostrKeyManager.decodeBech32(encoded)
        decoded.hrp() == "npub"
        decoded.data() == data
    }

    def "should handle bech32 round trip"() {
        given: "some test data"
        byte[] originalData = NostrUtils.hexStringToByteArray("1afe0c74e3d7784eba93a5e3fa554a6eeb01928d12739ae8ba4832786808e36d")

        when: "encoding to bech32 and then decoding"
        def encoded = nostrKeyManager.encodeBech32("test", originalData)
        def decoded = nostrKeyManager.decodeBech32(encoded)

        then: "the decoded data should match the original data"
        decoded.hrp() == "test"
        decoded.data() == originalData
    }

    def "should throw IllegalArgumentException for null input"() {
        when:
        nostrKeyManager.npubToHex(null)
        nostrKeyManager.hexToNpub(null)
        nostrKeyManager.nsecToHex(null)
        nostrKeyManager.hexToNsec(null)
        nostrKeyManager.encodeBech32(null, new byte[0])
        nostrKeyManager.decodeBech32(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "should throw IllegalArgumentException for invalid input"() {
        when:
        nostrKeyManager.decodeBech32("invalidBech32String")
        nostrKeyManager.decodeBech32("1234567890")
        nostrKeyManager.decodeBech32("")
        nostrKeyManager.hexToNpub("invalidHex")
        nostrKeyManager.hexToNpub("123")

        then:
        thrown(IllegalArgumentException)
    }

    def "should derive consistent public key from private key"() {
        when: "deriving public key from a valid private key"
        def derived1 = nostrKeyManager.derivePublicKeyFromPrivate(NostrUtils.VALID_PRIVATE_KEY_HEX)
        def derived2 = nostrKeyManager.derivePublicKeyFromPrivate(NostrUtils.VALID_PRIVATE_KEY_HEX)

        then: "the derived public keys should be consistent"
        derived1 == derived2
        derived1.length() == 64

        and: "the derived public key should not have a compression prefix"
        !derived1.startsWith("02") && !derived1.startsWith("03")

        and: "the derived public key should match the expected public key"
        derived1 == NostrUtils.PUBLIC_FROM_PRIVATE_KEY_HEX
    }
}