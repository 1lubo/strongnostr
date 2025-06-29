package com.onelubo.strongnostr.service

import com.onelubo.strongnostr.dto.nostr.NostrAuthRequest
import com.onelubo.strongnostr.dto.nostr.NostrUserProfile
import com.onelubo.strongnostr.model.user.User
import com.onelubo.strongnostr.nostr.NostrEvent
import com.onelubo.strongnostr.nostr.NostrEventVerifier
import com.onelubo.strongnostr.nostr.NostrKeyManager
import com.onelubo.strongnostr.security.JwtTokenProvider
import com.onelubo.strongnostr.service.nostr.NostrAuthenticationService
import com.onelubo.strongnostr.service.nostr.NostrUserService
import com.onelubo.strongnostr.util.NostrUtils
import spock.lang.Specification

import java.time.Instant

class NostrAuthenticationServiceSpec extends Specification{
    NostrUserService nostrUserService
    NostrEventVerifier nostrEventVerifier
    NostrKeyManager nostrKeyManager
    JwtTokenProvider jwtTokenProvider
    NostrAuthenticationService nostrAuthenticationService
    
    private static final String VALID_HEX = "02a1b2c3d4e5f6789abc123def456"
    private static final String VALID_USERNAME = "testuser"
    private static final String JWT_TOKEN = "jwt.token.here"

    def setup() {
        nostrUserService = Mock(NostrUserService)
        nostrEventVerifier = Mock(NostrEventVerifier)
        nostrKeyManager = Mock(NostrKeyManager)
        jwtTokenProvider = Mock(JwtTokenProvider)
        nostrAuthenticationService = new NostrAuthenticationService(nostrUserService, nostrEventVerifier, jwtTokenProvider)
    }

    def "should generate authentication challenge"() {
        when: "Generating an authentication challenge"
        def challenge = nostrAuthenticationService.generateAuthChallenge()

        then: "Should return a valid challenge"
        challenge != null
        challenge.getChallenge() != null
        challenge.getMessage() != null
        challenge.getMessage().startsWith("Strong Nostr authentication challenge: ")
        challenge.getTimestamp() > 0
        Instant.now().getEpochSecond() - challenge.getTimestamp() < 5 // Ensure the timestamp is recent
    }

    def "should generate unique challenge each time"() {
        when: "Generating multiple authentication challenges"
        def challenge1 = nostrAuthenticationService.generateAuthChallenge()
        def challenge2 = nostrAuthenticationService.generateAuthChallenge()

        then: "Should return different challenges"
        challenge1.getChallenge() != challenge2.getChallenge()
        challenge1.getMessage() != challenge2.getMessage()
    }

    def "should authenticate existing user with valid event"() {
        given: "An existing user and a valid Nostr event"
        def existingUser = createTestUser()
        def validEvent = NostrUtils.createValidAuthEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_EVENT_KIND)
        nostrUserService.getOrCreateUser(NostrUtils.VALID_NPUB, existingUser.getNostrProfile()) >> existingUser
        nostrEventVerifier.verifyEventSignature(_ as NostrEvent) >> true
        jwtTokenProvider.createAccessToken(existingUser.getnPub()) >> JWT_TOKEN

        when: "Authenticating with the valid event"
        def request = new NostrAuthRequest(validEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should return a valid JWT token and user profile"
        result.success()
        result.user() == existingUser
        result.accessToken() == JWT_TOKEN
        1 * nostrUserService.saveUser(existingUser)
    }

    def "should create new user on first authentication"() {
        given: "A valid Nostr event and no existing user"
        def validEvent = NostrUtils.createValidAuthEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_EVENT_KIND)
        def newUser = createTestUser()
        nostrEventVerifier.verifyEventSignature(validEvent) >> true
        nostrUserService.getOrCreateUser(NostrUtils.VALID_NPUB, newUser.getNostrProfile()) >> newUser
        nostrUserService.saveUser(_ as User) >> newUser
        nostrKeyManager.npubToHex(NostrUtils.VALID_NPUB) >> VALID_HEX
        nostrEventVerifier.verifyEventSignature(validEvent) >> true
        jwtTokenProvider.createAccessToken(newUser.getnPub()) >> JWT_TOKEN

        when: "Authenticating with the valid event"
        def request = new NostrAuthRequest(validEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should create a new user and return a valid JWT token"
        result.success()
        result.user() != null
        result.user() == newUser
        result.accessToken() == JWT_TOKEN
        1 * nostrUserService.saveUser(newUser)
    }

    def "should reject invalid Nostr event"() {
        given: "An invalid Nostr event"
        def invalidEvent = new NostrEvent()
        invalidEvent.setId(UUID.randomUUID().toString())
        invalidEvent.setKind(1) // Invalid kind for authentication

        when: "Authenticating with the invalid event"
        def request = new NostrAuthRequest(invalidEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should return an error indicating the event is invalid"
        !result.success()
        result.message() == "Invalid nostr event structure"
        result.user() == null
        result.accessToken() == null
        0 * nostrUserService.saveUser(_)
    }

    def "should reject an expired challenge"() {
        given: "An expired authentication challenge"
        def expiredEvent = NostrUtils.createValidAuthEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_EVENT_KIND)
        expiredEvent.setCreatedAt(Instant.now().getEpochSecond() - 400) // Set to 400 seconds in the past

        when: "Authenticating with the expired challenge"
        def request = new NostrAuthRequest(expiredEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should return an error indicating the challenge is expired"
        !result.success()
        result.message() == "Invalid or expired challenge"
        result.user() == null
        result.accessToken() == null
    }

    def "should reject an event with invalid signature"() {
        given: "A valid Nostr event with an invalid signature"
        def validEvent = NostrUtils.createValidAuthEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_EVENT_KIND)
        nostrEventVerifier.verifyEventSignature(validEvent) >> false

        when: "Authenticating with the event"
        def request = new NostrAuthRequest(validEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should return an error indicating the signature is invalid"
        !result.success()
        result.message() == "Invalid event signature"
        result.user() == null
        result.accessToken() == null
    }

    def "should reject an event with invalid content"() {
        given: "A valid Nostr event with invalid content"
        def invalidContentEvent = NostrUtils.createValidAuthEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_EVENT_KIND)
        invalidContentEvent.setContent("Invalid content format")

        when: "Authenticating with the event"
        def request = new NostrAuthRequest(invalidContentEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should return an error indicating the content is invalid"
        !result.success()
        result.message() == "Invalid or expired challenge"
        result.user() == null
        result.accessToken() == null
    }

    User createTestUser() {
        return new User(VALID_USERNAME, NostrUtils.VALID_NPUB, VALID_HEX)
    }
}
