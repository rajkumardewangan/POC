package com.example.salon.controller;

import com.example.salon.service.WhatsAppBotService;
import com.example.salon.service.WhatsAppMessageSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@RestController
@RequestMapping("/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private final WhatsAppBotService botService;
    private final WhatsAppMessageSender messageSender;
    private final ObjectMapper objectMapper;
    private final String verifyToken;
    private final String appSecret;

    public WhatsAppWebhookController(WhatsAppBotService botService,
                                    WhatsAppMessageSender messageSender,
                                    ObjectMapper objectMapper,
                                    @Value("${salon.whatsapp.verify-token:}") String verifyToken,
                                    @Value("${salon.whatsapp.app-secret:}") String appSecret) {
        this.botService = botService;
        this.messageSender = messageSender;
        this.objectMapper = objectMapper;
        this.verifyToken = verifyToken;
        this.appSecret = appSecret;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(@RequestParam(name = "hub.mode", required = false) String mode,
                                                @RequestParam(name = "hub.verify_token", required = false) String token,
                                                @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (!"subscribe".equals(mode) || !StringUtils.hasText(verifyToken) || !verifyToken.equals(token)
                || !StringUtils.hasText(challenge)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Webhook verification failed");
        }
        return ResponseEntity.ok(challenge);
    }

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody String payload,
                                                 @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {
        if (!StringUtils.hasText(appSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Webhook signature is not configured");
        }
        if (!isValidSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            for (JsonNode entry : root.path("entry")) {
                for (JsonNode change : entry.path("changes")) {
                    for (JsonNode message : change.path("value").path("messages")) {
                        if (!"text".equals(message.path("type").asText())) {
                            continue;
                        }
                        String sender = message.path("from").asText("");
                        String text = message.path("text").path("body").asText("");
                        Optional<String> reply = botService.handleMessage(sender, text);
                        reply.ifPresent(response -> messageSender.sendText(sender, response));
                    }
                }
            }
            return ResponseEntity.ok("EVENT_RECEIVED");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Invalid webhook payload");
        }
    }

    private boolean isValidSignature(String payload, String signature) {
        if (!StringUtils.hasText(signature) || !signature.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HexFormat.of().parseHex(signature.substring("sha256=".length()));
            return MessageDigest.isEqual(expected, supplied);
        } catch (Exception ex) {
            return false;
        }
    }
}