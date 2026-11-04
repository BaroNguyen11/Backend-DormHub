package com.dormhub.controller;

import com.dormhub.dto.request.SepayWebhookPayload;
import com.dormhub.security.SepayWebhookSignatureVerifier;
import com.dormhub.service.SepayWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/webhooks/sepay")
public class SepayWebhookController {

    private final SepayWebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final SepayWebhookService webhookService;

    public SepayWebhookController(SepayWebhookSignatureVerifier signatureVerifier, ObjectMapper objectMapper,
                                  SepayWebhookService webhookService) {
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
        this.webhookService = webhookService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "X-SePay-Signature", required = false) String signature,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestamp,
            @RequestBody byte[] rawBody) {
        if (!signatureVerifier.verify(signature, timestamp, rawBody)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }

        final SepayWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, SepayWebhookPayload.class);
        } catch (IOException exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }

        final SepayWebhookService.Outcome outcome;
        try {
            outcome = webhookService.reconcile(payload);
        } catch (RuntimeException exception) {
            if (!webhookService.isTransactionIdUniqueViolation(exception)) {
                throw exception;
            }
            return response(webhookService.resolveDuplicate(payload));
        }
        return response(outcome);
    }

    private ResponseEntity<Map<String, Object>> response(SepayWebhookService.Outcome outcome) {
        if (outcome == SepayWebhookService.Outcome.INVALID) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }
        if (outcome == SepayWebhookService.Outcome.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}
