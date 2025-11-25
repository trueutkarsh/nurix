package com.nurix.voicecampaign.controller;

import com.nurix.voicecampaign.model.CallStatus;
import com.nurix.voicecampaign.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;

    @PostMapping
    public ResponseEntity<Map<String, String>> triggerCall(@RequestBody Map<String, String> payload) {
        String phoneNumber = payload.get("phone_number");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String callId = callService.triggerCall(phoneNumber);
        return ResponseEntity.ok(Map.of("call_id", callId, "status", "IN_PROGRESS"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCallStatus(@PathVariable String id) {
        CallStatus status = callService.getCallStatus(id);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("call_id", id, "status", status));
    }
}
