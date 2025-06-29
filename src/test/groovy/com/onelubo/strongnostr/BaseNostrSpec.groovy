package com.onelubo.strongnostr


import com.onelubo.strongnostr.dto.nostr.NostrUserProfile
import com.onelubo.strongnostr.nostr.NostrEvent
import com.onelubo.strongnostr.nostr.NostrEventVerifier
import com.onelubo.strongnostr.nostr.NostrKeyManager
import com.onelubo.strongnostr.nostr.NostrKeyManager.NostrKeyPair
import com.onelubo.strongnostr.security.JwtTokenProvider
import com.onelubo.strongnostr.util.SchnorrSigner
import de.flapdoodle.embed.mongo.commands.ServerAddress
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.reverse.TransitionWalker
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaseNostrSpec extends Specification {

    NostrEventVerifier nostrEventVerifier
    TestRestTemplate restTemplate
    NostrKeyManager nostrKeyManager
    SchnorrSigner schnorrSigner
    JwtTokenProvider jwtTokenProvider
    TransitionWalker.ReachedState<RunningMongodProcess> running
    ServerAddress serverAddress
    @LocalServerPort int port
    String baseUrl

    def setup() {
        ImmutableMongod mongodConfig = Mongod.instance()
        Version.Main version = Version.Main.V8_0

        running = mongodConfig.start(version)
        serverAddress = running.current().getServerAddress() as ServerAddress
        schnorrSigner = new SchnorrSigner()
        restTemplate = new TestRestTemplate()
        nostrKeyManager = new NostrKeyManager()
        nostrEventVerifier = new NostrEventVerifier(nostrKeyManager)
        jwtTokenProvider = new JwtTokenProvider()
        baseUrl = "http://localhost:${port}"
    }

    NostrKeyPair createNostrKeyPair() {
        nostrKeyManager.generateKeyPair()
    }


    NostrEvent createSignedNostrEvent(String npub, String nSecHex, String challenge) {
        // First create the event without signature to compute the ID
        def event = new NostrEvent(
                id: null,
                kind: 22242,
                npub: npub,
                content: challenge,
                tags: [],
                createdAt: System.currentTimeMillis() / 1000, // Convert to seconds
                signature: null
        )

        // Compute the event ID
        String eventId = nostrEventVerifier.computeEventId(event)

        // Sign the event ID (not the challenge content)
        String signature = schnorrSigner.signEventId(nSecHex, eventId)

        // Set both ID and signature
        event.setId(eventId)
        event.setSignature(signature)

        return event
    }

    NostrUserProfile createNostrUserProfile(String nip05, String name, String picture) {
        new NostrUserProfile(
                name: name,
                avatarUrl: picture,
                nip05: nip05
        )
    }
}
