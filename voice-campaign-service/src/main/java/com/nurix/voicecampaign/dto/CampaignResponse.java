package com.nurix.voicecampaign.dto;

import com.nurix.voicecampaign.model.CampaignStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class CampaignResponse {
    private Long id;
    private String name;
    private CampaignStatus status;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timezone;
    private Integer concurrencyLimit;
    private Integer retryCount;
    private LocalDateTime createdAt;
    
    private long totalCalls;
    private long completedCalls;
    private long failedCalls;
    private long pendingCalls;
    private long inProgressCalls;
}
