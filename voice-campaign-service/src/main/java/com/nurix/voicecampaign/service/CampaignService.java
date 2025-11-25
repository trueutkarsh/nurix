package com.nurix.voicecampaign.service;

import com.nurix.voicecampaign.dto.CampaignRequest;
import com.nurix.voicecampaign.dto.CampaignResponse;
import com.nurix.voicecampaign.model.CallStatus;
import com.nurix.voicecampaign.model.Campaign;
import com.nurix.voicecampaign.model.CampaignStatus;
import com.nurix.voicecampaign.model.PhoneNumber;
import com.nurix.voicecampaign.repository.CampaignRepository;
import com.nurix.voicecampaign.repository.PhoneNumberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final PhoneNumberRepository phoneNumberRepository;

    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request) {
        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .status(CampaignStatus.PENDING)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timezone(request.getTimezone())
                .concurrencyLimit(request.getConcurrencyLimit() != null ? request.getConcurrencyLimit() : 10) // Default 10
                .retryCount(request.getRetryCount() != null ? request.getRetryCount() : 3) // Default 3
                .build();

        Campaign savedCampaign = campaignRepository.save(campaign);

        List<PhoneNumber> phoneNumbers = request.getPhoneNumbers().stream()
                .map(num -> PhoneNumber.builder()
                        .number(num)
                        .status(CallStatus.PENDING)
                        .campaign(savedCampaign)
                        .retriesAttempted(0)
                        .build())
                .collect(Collectors.toList());

        phoneNumberRepository.saveAll(phoneNumbers);

        return mapToResponse(savedCampaign);
    }

    public CampaignResponse getCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        return mapToResponse(campaign);
    }

    @Transactional
    public CampaignResponse startCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (campaign.getStatus() == CampaignStatus.PENDING || campaign.getStatus() == CampaignStatus.PAUSED) {
            campaign.setStatus(CampaignStatus.RUNNING);
            campaignRepository.save(campaign);
        }
        return mapToResponse(campaign);
    }

    @Transactional
    public CampaignResponse pauseCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            campaign.setStatus(CampaignStatus.PAUSED);
            campaignRepository.save(campaign);
        }
        return mapToResponse(campaign);
    }

    private CampaignResponse mapToResponse(Campaign campaign) {
        long total = phoneNumberRepository.countByCampaignIdAndStatus(campaign.getId(), null); // This might need a custom query or just count all
        // Actually countByCampaignIdAndStatus with null status might not work as expected in JPA derived queries depending on impl.
        // Better to use countByCampaignId if I added it, or just sum up.
        // Let's add countByCampaignId to repo or just use the list if lazy loading isn't an issue (it is).
        // For now, let's assume we want accurate stats.
        
        long completed = phoneNumberRepository.countByCampaignIdAndStatus(campaign.getId(), CallStatus.COMPLETED);
        long failed = phoneNumberRepository.countByCampaignIdAndStatus(campaign.getId(), CallStatus.FAILED);
        long pending = phoneNumberRepository.countByCampaignIdAndStatus(campaign.getId(), CallStatus.PENDING);
        long inProgress = phoneNumberRepository.countByCampaignIdAndStatus(campaign.getId(), CallStatus.IN_PROGRESS);
        
        // Total is sum of all (or just count all by campaign id)
        // I'll add countByCampaignId to the repo in a bit, or just sum these up for now.
        long totalCalculated = completed + failed + pending + inProgress;

        return CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .status(campaign.getStatus())
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .timezone(campaign.getTimezone())
                .concurrencyLimit(campaign.getConcurrencyLimit())
                .retryCount(campaign.getRetryCount())
                .createdAt(campaign.getCreatedAt())
                .totalCalls(totalCalculated)
                .completedCalls(completed)
                .failedCalls(failed)
                .pendingCalls(pending)
                .inProgressCalls(inProgress)
                .build();
    }
}
