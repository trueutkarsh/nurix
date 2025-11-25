package com.nurix.voicecampaign.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
public class CampaignRequest {

    @NotEmpty(message = "Name is required")
    private String name;

    @NotEmpty(message = "At least one phone number is required")
    private List<String> phoneNumbers;

    private LocalTime startTime;
    private LocalTime endTime;
    private String timezone;

    private Integer concurrencyLimit;
    private Integer retryCount;
}
