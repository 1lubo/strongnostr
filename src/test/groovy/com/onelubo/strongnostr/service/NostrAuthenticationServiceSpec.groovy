package com.onelubo.strongnostr.service

import com.onelubo.strongnostr.dto.NostrAuthRequest
import com.onelubo.strongnostr.dto.NostrUserProfile
import com.onelubo.strongnostr.model.user.User
import com.onelubo.strongnostr.nostr.NostrEvent
import com.onelubo.strongnostr.nostr.NostrEventVerifier
import com.onelubo.strongnostr.nostr.NostrKeyManager
import com.onelubo.strongnostr.repository.UserRepository
import com.onelubo.strongnostr.security.JwtTokenProvider
import spock.lang.Specification

import java.time.Instant

class NostrAuthenticationServiceSpec extends Specification{
    UserRepository userRepository
    NostrEventVerifier nostrEventVerifier
    NostrKeyManager nostrKeyManager
    JwtTokenProvider jwtTokenProvider
    NostrAuthenticationService nostrAuthenticationService

    private static final String VALID_NPUB = "npub1abc123def456ghi789";
    private static final String VALID_HEX = "02a1b2c3d4e5f6789abc123def456";
    private static final String VALID_USERNAME = "testuser";
    private static final String JWT_TOKEN = "jwt.token.here";
    private static final int VALID_EVENT_KIND = 22242; // Assuming this is the kind for authentication events

    def setup() {
        userRepository = Mock(UserRepository)
        nostrEventVerifier = Mock(NostrEventVerifier)
        nostrKeyManager = Mock(NostrKeyManager)
        jwtTokenProvider = Mock(JwtTokenProvider)
        nostrAuthenticationService = new NostrAuthenticationService(userRepository, nostrEventVerifier, nostrKeyManager, jwtTokenProvider)
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
        def validEvent = createValidAuthEvent()
        userRepository.findByNostrPubKey(VALID_NPUB) >> Optional.of(existingUser)
        nostrEventVerifier.verifyEventSignature(_) >> true
        jwtTokenProvider.generateToken(existingUser.getId()) >> JWT_TOKEN

        when: "Authenticating with the valid event"
        def request = new NostrAuthRequest(validEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should return a valid JWT token and user profile"
        result.isSuccess()
        result.getUser() == existingUser
        result.getJwtToken() == JWT_TOKEN
        1 * userRepository.save(existingUser)
    }

    def "should create new user on first authentication"() {
        given: "A valid Nostr event and no existing user"
        def validEvent = createValidAuthEvent()
        def newUser = createTestUser()
        nostrEventVerifier.verifyEventSignature(validEvent) >> true
        userRepository.findByNostrPubKey(VALID_NPUB) >> Optional.empty()
        userRepository.save(_ as User) >> newUser
        nostrKeyManager.npubToHex(VALID_NPUB) >> VALID_HEX
        nostrEventVerifier.verifyEventSignature(validEvent) >> true
        jwtTokenProvider.generateToken(newUser.getId()) >> JWT_TOKEN

        when: "Authenticating with the valid event"
        def request = new NostrAuthRequest(validEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should create a new user and return a valid JWT token"
        result.isSuccess()
        result.getUser() != null
        result.getUser() == newUser
        result.getJwtToken() == JWT_TOKEN
        1 * userRepository.save(newUser)
    }

    def "should update user profile from nostr data"() {
        given: "An existing user and a valid Nostr profile"
        def existingUser = createTestUser()
        def outdatedProfile = new NostrUserProfile()
        outdatedProfile.setName("Old Name")
        outdatedProfile.setAbout("Old about text.")
        outdatedProfile.setAvatarUrl("https://old.example.com/avatar.jpg")
        existingUser.setNostrProfile(outdatedProfile)
        def newName = "Updated Name"
        def newAbout = "Updated about text."
        def mewAvatarUrl = "https://new.example.com/avatar.jpg"
        def newProfile = new NostrUserProfile()
        newProfile.setName(newName)
        newProfile.setAbout(newAbout)
        newProfile.setAvatarUrl(mewAvatarUrl)
        def validEvent = createValidAuthEvent()
        userRepository.findByNostrPubKey(VALID_NPUB) >> Optional.of(existingUser)
        nostrEventVerifier.verifyEventSignature(validEvent) >> true
        jwtTokenProvider.generateToken(existingUser.getId()) >> JWT_TOKEN

        when: "Authenticating with the valid event"
        def request = new NostrAuthRequest(validEvent,  newProfile)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should update the user profile and return a valid JWT token"
        result.isSuccess()
        result.getUser() != null
        result.getUser().getNostrProfile().getName() == newName
        result.getUser().getNostrProfile().getAbout() == newAbout
        result.getUser().getNostrProfile().getAvatarUrl() == mewAvatarUrl
        result.getJwtToken() == JWT_TOKEN
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
        !result.isSuccess()
        result.getMessage() == "Invalid nostr event structure"
        result.getUser() == null
        result.getJwtToken() == null
        0 * userRepository.save(_)
    }

    def "should reject an expired challenge"() {
        given: "An expired authentication challenge"
        def expiredEvent = createValidAuthEvent()
        expiredEvent.setCreatedAt(Instant.now().getEpochSecond() - 400) // Set to 400 seconds in the past

        when: "Authenticating with the expired challenge"
        def request = new NostrAuthRequest(expiredEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should return an error indicating the challenge is expired"
        !result.isSuccess()
        result.getMessage() == "Invalid or expired challenge"
        result.getUser() == null
        result.getJwtToken() == null
    }

    def "should reject an event with invalid signature"() {
        given: "A valid Nostr event with an invalid signature"
        def validEvent = createValidAuthEvent()
        nostrEventVerifier.verifyEventSignature(validEvent) >> false

        when: "Authenticating with the event"
        def request = new NostrAuthRequest(validEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should return an error indicating the signature is invalid"
        !result.isSuccess()
        result.getMessage() == "Invalid event signature"
        result.getUser() == null
        result.getJwtToken() == null
    }

    def "should reject an event with invalid content"() {
        given: "A valid Nostr event with invalid content"
        def invalidContentEvent = createValidAuthEvent()
        invalidContentEvent.setContent("Invalid content format")

        when: "Authenticating with the event"
        def request = new NostrAuthRequest(invalidContentEvent)
        def result = nostrAuthenticationService.authenticateWithNostrEvent(request)

        then: "Should return an error indicating the content is invalid"
        !result.isSuccess()
        result.getMessage() == "Invalid or expired challenge"
        result.getUser() == null
        result.getJwtToken() == null
    }

    User createTestUser() {
        return new User(VALID_USERNAME, VALID_NPUB, VALID_HEX)
    }

    NostrUserProfile createTestProfile() {
        NostrUserProfile profile = new NostrUserProfile()
        profile.setName("Test User")
        profile.setAbout("This is a test user profile.")
        profile.setAvatarUrl("https://example.com/profile.jpg")
        profile.setLud16("testLightningAddress")
        profile.setNip05("nostrPleb1@nostrplebs.com")
    }

    NostrEvent createValidAuthEvent() {
        NostrEvent event = new NostrEvent()
        event.setId(UUID.randomUUID().toString())
        event.setPubkey(VALID_NPUB)
        event.setCreatedAt(Instant.now().getEpochSecond())
        event.setKind(VALID_EVENT_KIND)
        event.setContent("Strong Nostr authentication challenge: " + UUID.randomUUID().toString() + " at " + Instant.now().getEpochSecond())
        event.setSig("validSignature")
        return event
    }
}
