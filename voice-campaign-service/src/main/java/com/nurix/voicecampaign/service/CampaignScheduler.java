package com.nurix.voicecampaign.service;

import com.nurix.voicecampaign.model.CallStatus;
import com.nurix.voicecampaign.model.Campaign;
import com.nurix.voicecampaign.model.CampaignStatus;
import com.nurix.voicecampaign.model.PhoneNumber;
import com.nurix.voicecampaign.repository.CampaignRepository;
import com.nurix.voicecampaign.repository.PhoneNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignScheduler {

    private final CampaignRepository campaignRepository;
    private final PhoneNumberRepository phoneNumberRepository;
    private final CallService callService;

    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    @Transactional
    public void processCampaigns() {
        List<Campaign> runningCampaigns = campaignRepository.findByStatus(CampaignStatus.RUNNING);

        for (Campaign campaign : runningCampaigns) {
            processCampaign(campaign);
        }
    }

    private void processCampaign(Campaign campaign) {
        // 1. Check Business Hours
        if (!isWithinBusinessHours(campaign)) {
            log.debug("Campaign {} is outside business hours", campaign.getId());
            return;
        }

        // 2. Check Concurrency
        long activeCalls = phoneNumberRepository.countByCampaignIdAndStatus(campaign.getId(), CallStatus.IN_PROGRESS);
        int concurrencyLimit = campaign.getConcurrencyLimit();
        
        if (activeCalls >= concurrencyLimit) {
            log.debug("Campaign {} has reached concurrency limit ({}/{})", campaign.getId(), activeCalls, concurrencyLimit);
            return;
        }

        int slotsAvailable = (int) (concurrencyLimit - activeCalls);
        
        // 3. Fetch Eligible Numbers
        List<PhoneNumber> eligibleNumbers = phoneNumberRepository.findEligibleNumbers(
                campaign.getId(), 
                campaign.getRetryCount(), 
                PageRequest.of(0, slotsAvailable)
        );

        if (eligibleNumbers.isEmpty()) {
            // Check if campaign is completed
            long pendingOrFailedRetryable = phoneNumberRepository.countByCampaignIdAndStatus(campaign.getId(), CallStatus.PENDING) +
                                            phoneNumberRepository.countByCampaignIdAndStatus(campaign.getId(), CallStatus.FAILED); // This is rough, ideally specific query
            // Actually, if findEligibleNumbers returns empty and there are no IN_PROGRESS, the campaign might be done.
            // But let's not auto-complete for now, just log.
            log.debug("No eligible numbers for campaign {}", campaign.getId());
            return;
        }

        // 4. Trigger Calls
        for (PhoneNumber phoneNumber : eligibleNumbers) {
            triggerCall(phoneNumber);
        }
    }

    private boolean isWithinBusinessHours(Campaign campaign) {
        if (campaign.getStartTime() == null || campaign.getEndTime() == null) {
            return true; // No restrictions
        }

        ZoneId zoneId = (campaign.getTimezone() != null && !campaign.getTimezone().isEmpty()) 
                ? ZoneId.of(campaign.getTimezone()) 
                : ZoneId.systemDefault();
        
        LocalTime now = ZonedDateTime.now(zoneId).toLocalTime();
        return !now.isBefore(campaign.getStartTime()) && !now.isAfter(campaign.getEndTime());
    }

    private void triggerCall(PhoneNumber phoneNumber) {
        try {
            log.info("Triggering call for number {} in campaign {}", phoneNumber.getNumber(), phoneNumber.getCampaign().getId());
            
            // Update status to IN_PROGRESS immediately to reserve slot
            phoneNumber.setStatus(CallStatus.IN_PROGRESS);
            phoneNumber.setLastAttemptTime(LocalDateTime.now());
            phoneNumber.setRetriesAttempted(phoneNumber.getRetriesAttempted() + 1); // Increment attempt count
            phoneNumberRepository.save(phoneNumber);

            // Trigger actual call (mock)
            String callId = callService.triggerCall(phoneNumber.getNumber());
            phoneNumber.setExternalCallId(callId);
            phoneNumberRepository.save(phoneNumber);
            
        } catch (Exception e) {
            log.error("Failed to trigger call for {}", phoneNumber.getNumber(), e);
            phoneNumber.setStatus(CallStatus.FAILED);
            phoneNumberRepository.save(phoneNumber);
        }
    }

    @Scheduled(fixedRate = 10000) // Check status every 10 seconds
    @Transactional
    public void updateCallStatuses() {
        List<Campaign> runningCampaigns = campaignRepository.findByStatus(CampaignStatus.RUNNING);
        for (Campaign campaign : runningCampaigns) {
             List<PhoneNumber> inProgressNumbers = phoneNumberRepository.findByCampaignIdAndStatus(
                     campaign.getId(), 
                     CallStatus.IN_PROGRESS, 
                     PageRequest.of(0, 100)
             );
             
             for (PhoneNumber phoneNumber : inProgressNumbers) {
                 if (phoneNumber.getExternalCallId() != null) {
                     CallStatus status = callService.getCallStatus(phoneNumber.getExternalCallId());
                     if (status != null && status != CallStatus.IN_PROGRESS) {
                         log.info("Updating status for number {} to {}", phoneNumber.getNumber(), status);
                         phoneNumber.setStatus(status);
                         phoneNumberRepository.save(phoneNumber);
                     }
                 }
             }
        }
    }
}
