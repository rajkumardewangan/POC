package com.example.salon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class MetaWhatsAppMessageSender implements WhatsAppMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(MetaWhatsAppMessageSender.class);

    private final RestClient restClient;
    private final String accessToken;
    private final String phoneNumberId;
    private final String graphApiVersion;

    public MetaWhatsAppMessageSender(@Value("${salon.whatsapp.access-token:}") String accessToken,
                                    @Value("${salon.whatsapp.phone-number-id:}") String phoneNumberId,
                                    @Value("${salon.whatsapp.graph-api-version:v22.0}") String graphApiVersion) {
        this.restClient = RestClient.builder().baseUrl("https://graph.facebook.com").build();
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
        this.graphApiVersion = graphApiVersion;
    }

    @Override
    public void sendText(String recipient, String message) {
        if (accessToken.isBlank() || phoneNumberId.isBlank()) {
            logger.warn("WhatsApp reply not sent because Meta access token or phone number ID is not configured.");
            return;
        }

        try {
            restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages", graphApiVersion, phoneNumberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "recipient_type", "individual",
                            "to", recipient,
                            "type", "text",
                            "text", Map.of("preview_url", false, "body", message)
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            logger.error("Failed to send WhatsApp reply to configured recipient.", ex);
        }
    }
}