package com.onelubo.strongnostr.service

import com.onelubo.strongnostr.dto.nostr.NostrAuthRequest
import com.onelubo.strongnostr.model.user.User
import com.onelubo.strongnostr.nostr.NostrEvent
import com.onelubo.strongnostr.nostr.NostrKeyManager
import com.onelubo.strongnostr.security.JwtTokenProvider
import com.onelubo.strongnostr.service.nostr.NostrAuthenticationService
import com.onelubo.strongnostr.service.nostr.NostrUserService
import com.onelubo.strongnostr.util.NostrUtils
import spock.lang.Specification

import java.time.Instant

class NostrAuthenticationServiceSpec extends Specification{
    NostrUserService nostrUserService
    NostrKeyManager nostrKeyManager
    JwtTokenProvider jwtTokenProvider
    NostrAuthenticationService nostrAuthenticationService
    
    private static final String VALID_HEX = "02a1b2c3d4e5f6789abc123def456"
    private static final String VALID_USERNAME = "testuser"
    private static final String JWT_TOKEN = "jwt.token.here"

    def setup() {
        nostrUserService = Mock(NostrUserService)
        nostrKeyManager = Mock(NostrKeyManager)
        jwtTokenProvider = Mock(JwtTokenProvider)
        nostrAuthenticationService = new NostrAuthenticationService(nostrUserService, jwtTokenProvider)
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
        def validChallenge = NostrUtils.generateAuthChallenge()
        def validEvent = NostrUtils.createSignedNostrEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_PRIVATE_KEY_HEX, validChallenge.getChallenge())
        nostrUserService.getOrCreateUser(NostrUtils.VALID_NPUB, existingUser.getNostrProfile()) >> existingUser
        jwtTokenProvider.createAccessToken(existingUser.getnPub()) >> JWT_TOKEN

        when: "Authenticating with the valid event"
        def request = new NostrAuthRequest(validEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request, validChallenge.getChallenge())

        then: "Should return a valid JWT token and user profile"
        result.success()
        result.user() == existingUser
        result.accessToken() == JWT_TOKEN
        1 * nostrUserService.saveUser(existingUser)
    }

    def "should create new user on first authentication"() {
        given: "A valid Nostr event and no existing user"
        def newUser = createTestUser()
        def validChallenge = NostrUtils.generateAuthChallenge()
        def validEvent = NostrUtils.createSignedNostrEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_PRIVATE_KEY_HEX, validChallenge.getChallenge())
        nostrUserService.getOrCreateUser(NostrUtils.VALID_NPUB, newUser.getNostrProfile()) >> newUser
        nostrUserService.saveUser(_ as User) >> newUser
        nostrKeyManager.npubToHex(NostrUtils.VALID_NPUB) >> VALID_HEX
        jwtTokenProvider.createAccessToken(newUser.getnPub()) >> JWT_TOKEN

        when: "Authenticating with the valid event"
        def request = new NostrAuthRequest(validEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request,validChallenge.getChallenge())

        then: "Should create a new user and return a valid JWT token"
        result.success()
        result.user() != null
        result.user() == newUser
        result.accessToken() == JWT_TOKEN
        1 * nostrUserService.saveUser(newUser)
    }

    def "should reject invalid Nostr event"() {
        given: "An invalid Nostr event"
        def validChallenge = NostrUtils.generateAuthChallenge()
        def invalidEvent = new NostrEvent()
        invalidEvent.setId(UUID.randomUUID().toString())
        invalidEvent.setContent(validChallenge.getChallenge())
        invalidEvent.setKind(1) // Invalid kind for authentication

        when: "Authenticating with the invalid event"
        def request = new NostrAuthRequest(invalidEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request, validChallenge.getChallenge())

        then: "Should return an error indicating the event is invalid"
        !result.success()
        result.message() == "Invalid nostr event structure"
        result.user() == null
        result.accessToken() == null
        0 * nostrUserService.saveUser(_)
    }

    def "should reject an expired challenge"() {
        given: "An expired authentication challenge"
        def validChallenge = NostrUtils.generateAuthChallenge()
        def expiredEvent = NostrUtils.createSignedNostrEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_PRIVATE_KEY_HEX, validChallenge.getChallenge())
        expiredEvent.setCreatedAt(Instant.now().getEpochSecond() - 400) // Set to 400 seconds in the past

        when: "Authenticating with the expired challenge"
        def request = new NostrAuthRequest(expiredEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request, validChallenge.getChallenge())

        then: "Should return an error indicating the challenge is expired"
        !result.success()
        result.message() == "Invalid or expired challenge"
        result.user() == null
        result.accessToken() == null
    }


    def "should reject an event with invalid content"() {
        given: "A valid Nostr event with invalid content"
        def validChallenge = NostrUtils.generateAuthChallenge()
        def invalidContentEvent = NostrUtils.createSignedNostrEvent(NostrUtils.VALID_NPUB, NostrUtils.VALID_PRIVATE_KEY_HEX, validChallenge.getChallenge())
        invalidContentEvent.setContent("Invalid content format")

        when: "Authenticating with the event"
        def request = new NostrAuthRequest(invalidContentEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request, validChallenge.getChallenge())

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
