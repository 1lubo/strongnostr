package com.onelubo.strongnostr.rest.nostr;

import com.onelubo.strongnostr.dto.nostr.NostrAuthChallenge;
import com.onelubo.strongnostr.dto.nostr.NostrAuthRequest;
import com.onelubo.strongnostr.dto.nostr.NostrAuthResult;
import com.onelubo.strongnostr.service.ChallengeStore;
import com.onelubo.strongnostr.service.nostr.NostrAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;


@RestController
@RequestMapping("/api/v1/nostr/auth")
@Tag(name = "Authentication", description = "Nostr-based authentication endpoints")
public class NostrAuthController {

    Logger logger = LoggerFactory.getLogger(NostrAuthController.class);

    private final NostrAuthenticationService nostrAuthenticationService;
    private final ChallengeStore challengeStore;

    public NostrAuthController(NostrAuthenticationService nostrAuthenticationService, ChallengeStore challengeStore) {
        this.nostrAuthenticationService = nostrAuthenticationService;
        this.challengeStore = challengeStore;
    }

    @Operation(
            summary = "Generate Nostr Authentication Challenge",
            description = """
                      Generates a unique authentication challenge that must be signed with your Nostr private key.
                      The challenge is valid for 5 minutes and can only be used once.
                      """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Challenge generated successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation =
                                            NostrAuthChallenge.class),
                                    examples = @ExampleObject(
                                            name = "Success",
                                            value = """
                                                    "challengeId": "123e4567-e89b-12d3-a456-426614174000",
                                                    "challenge": "Strong Nostr authentication challenge: c0fb86d0-56dc-43a7-aee9-64ce77dc324b",
                                                    "expiresAt": 1703520000000,
                                                    "message": "Strong Nostr authentication challenge: c0fb86d0-56dc-43a7-aee9-64ce77dc324b at 1703520000000"
                                                    """
                                    )
                            )
                    )
            }
    )
    @PostMapping("/challenge")
    public ResponseEntity<NostrAuthChallenge> createChallenge() {
        NostrAuthChallenge challenge = nostrAuthenticationService.generateAuthChallenge();
        challengeStore.storeChallenge(challenge);
        return  ResponseEntity.ok(challenge);
    }

    @Operation(
            summary = "Authenticate with Nostr Event with signed challenge",
            description = """
                          Submit a Nostr event containing the signed authentication challenge.
                          The event must be properly signed with BIP340 Schnorr signatures and contain the challenge in the content field.
                          
                          **Event Requirements:**
                          - Kind: 22242 (NIP-46 authentication event)
                          - Content: The exact challenge string received from the challenge endpoint
                          - Signature: BIP340 Schnorr signature of the event ID
                          - Created_at: Unix timestamp (should be recent)
                          """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Nostr event containing the signed challenge",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation =
                                    NostrAuthRequest.class),
                            examples = @ExampleObject(
                                    name = "Authentication Event Example",
                                    value = """
                                            {
                                                "nostrEvent": {
                                                    "id": "18d13aa311cae985676dc05ab61c27da1cbc00917b354f436cc2f153004ff8dd",
                                                    "npub": "npub17j0e4tqqvymsuq3pyntwaxe4dksvs59cfwcveprhf79jkcnhm62qsxxsv6",
                                                    "kind": 22242,
                                                    "created_at": 1703520000,
                                                    "content": "Strong Nostr authentication challenge: c0fb86d0-56dc-43a7-aee9-64ce77dc324b",
                                                    "signature": "b741ced09bb853dd91590bfb4013bf9e17c27822adf626d857a3ffed03f0fd2fe042698f5d8c647fcc618012f242ab7305417b1245c5941619f874f5dbe8defa",
                                                    "tags": []
                                                }
                                                "userProfile": {
                                                    "name": "Alice Fitness",
                                                    "about": "Fitness enthusiast",
                                                    "avatarUrl": "https://example.com/avatar.jpg",
                                                    "nip05": "alice@fitness.com",
                                                    "lud16": "alice@wallet.com"
                                                }
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation =
                                    NostrAuthResult.class),
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                                "success": true,
                                                "message": "Authentication successful",
                                                "user": {
                                                    "npub": "npub17j0e4tqqvymsuq3pyntwaxe4dksvs59cfwcveprhf79jkcnhm62qsxxsv6",
                                                    "name": "Alice Fitness",
                                                },
                                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired challenge",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation =
                                    NostrAuthResult.class),
                            examples = @ExampleObject(
                                    name = "Failure",
                                    value = """
                                            {
                                                "success": false,
                                                "message": "Invalid or expired challenge"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<NostrAuthResult> loginWithNostrEvent(@RequestBody NostrAuthRequest nostrAuthRequest) {
        Optional<ChallengeStore.StoredChallenge> storedChallenge = challengeStore.getChallenge(nostrAuthRequest.getChallengeId());

        if (storedChallenge.isEmpty() || storedChallenge.get().used()) {
            return ResponseEntity.badRequest().body(NostrAuthResult.failure("Invalid or expired challenge"));
        }

        challengeStore.markChallengeAsUsed(nostrAuthRequest.getChallengeId());

        NostrAuthResult nostrAuthResult = nostrAuthenticationService.authenticateWithNostrEvent(nostrAuthRequest);
        if (nostrAuthResult.success()) {
            return ResponseEntity.ok(nostrAuthResult);
        } else {
            logger.info("Authentication failed: {}", nostrAuthResult.message());
            return ResponseEntity.badRequest().body(nostrAuthResult);
        }
    }

}
