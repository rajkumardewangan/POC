package com.example.salon.service;

public interface WhatsAppMessageSender {
    void sendText(String recipient, String message);
}