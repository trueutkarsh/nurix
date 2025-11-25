package com.nurix.voicecampaign.service;

import com.nurix.voicecampaign.model.CallStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CallService {

    private final Map<String, CallStatus> callStore = new ConcurrentHashMap<>();

    public String triggerCall(String phoneNumber) {
        String callId = UUID.randomUUID().toString();
        // Simulate initial status
        callStore.put(callId, CallStatus.IN_PROGRESS);
        
        // Simulate async processing (in a real app this would be a callback or polling)
        // For this mock, we'll just randomly determine the outcome when queried or after a delay
        // But to keep it simple for the "check status" API, we can just return a random status if enough time has passed
        // Or better, let's just store it.
        
        return callId;
    }

    public CallStatus getCallStatus(String callId) {
        if (!callStore.containsKey(callId)) {
            return null;
        }
        
        // Simulate status change over time
        // For simplicity, let's just randomly return COMPLETED or FAILED if it's IN_PROGRESS
        // In a real mock, we might want to control this better, but random is fine for now.
        CallStatus currentStatus = callStore.get(callId);
        if (currentStatus == CallStatus.IN_PROGRESS) {
             // 80% success rate
            if (ThreadLocalRandom.current().nextInt(100) < 80) {
                currentStatus = CallStatus.COMPLETED;
            } else {
                currentStatus = CallStatus.FAILED;
            }
            callStore.put(callId, currentStatus);
        }
        
        return currentStatus;
    }
}
