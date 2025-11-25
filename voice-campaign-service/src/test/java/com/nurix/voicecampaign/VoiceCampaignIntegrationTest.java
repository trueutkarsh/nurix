package com.nurix.voicecampaign;

import com.nurix.voicecampaign.dto.CampaignRequest;
import com.nurix.voicecampaign.dto.CampaignResponse;
import com.nurix.voicecampaign.model.CallStatus;
import com.nurix.voicecampaign.model.CampaignStatus;
import com.nurix.voicecampaign.model.PhoneNumber;
import com.nurix.voicecampaign.repository.PhoneNumberRepository;
import com.nurix.voicecampaign.service.CallService;
import com.nurix.voicecampaign.service.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class VoiceCampaignIntegrationTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private PhoneNumberRepository phoneNumberRepository;

    @MockBean
    private CallService callService;

    @BeforeEach
    void setup() {
        phoneNumberRepository.deleteAll();
        // Default mock behavior
        when(callService.triggerCall(anyString())).thenReturn(UUID.randomUUID().toString());
        when(callService.getCallStatus(anyString())).thenReturn(CallStatus.IN_PROGRESS);
    }

    @Test
    void testCampaignLifecycle() {
        // Mock completion
        when(callService.getCallStatus(anyString())).thenAnswer(inv -> {
             if (Math.random() > 0.5) return CallStatus.COMPLETED;
             return CallStatus.IN_PROGRESS;
        });

        CampaignRequest request = new CampaignRequest();
        request.setName("Test Campaign");
        request.setPhoneNumbers(Arrays.asList("1234567890", "0987654321"));
        request.setConcurrencyLimit(5);
        request.setRetryCount(3);

        CampaignResponse campaign = campaignService.createCampaign(request);
        campaignService.startCampaign(campaign.getId());

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<PhoneNumber> numbers = phoneNumberRepository.findAll();
            assertThat(numbers).extracting(PhoneNumber::getStatus)
                    .contains(CallStatus.IN_PROGRESS);
        });
    }

    @Test
    void testConcurrencyLimit() {
        // Ensure calls stay IN_PROGRESS
        when(callService.getCallStatus(anyString())).thenReturn(CallStatus.IN_PROGRESS);

        CampaignRequest request = new CampaignRequest();
        request.setName("Concurrency Test");
        // Create 10 numbers
        List<String> numbers = IntStream.range(0, 10).mapToObj(i -> "100000000" + i).collect(Collectors.toList());
        request.setPhoneNumbers(numbers);
        request.setConcurrencyLimit(3); // Limit to 3
        request.setRetryCount(0);

        CampaignResponse campaign = campaignService.createCampaign(request);
        campaignService.startCampaign(campaign.getId());

        // Wait for scheduler to kick in
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            long inProgressCount = phoneNumberRepository.countByCampaignIdAndStatus(campaign.getId(), CallStatus.IN_PROGRESS);
            assertThat(inProgressCount).isEqualTo(3);
        });

        // Verify it doesn't exceed 3 even after more time
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        long inProgressCount = phoneNumberRepository.countByCampaignIdAndStatus(campaign.getId(), CallStatus.IN_PROGRESS);
        assertThat(inProgressCount).isEqualTo(3);
    }

    @Test
    void testRetryLogic() {
        // Mock failure
        when(callService.getCallStatus(anyString())).thenReturn(CallStatus.FAILED);

        CampaignRequest request = new CampaignRequest();
        request.setName("Retry Test");
        request.setPhoneNumbers(Arrays.asList("5555555555"));
        request.setConcurrencyLimit(1);
        request.setRetryCount(2); // 2 retries = 3 total attempts

        CampaignResponse campaign = campaignService.createCampaign(request);
        campaignService.startCampaign(campaign.getId());

        // Wait for retries to happen. Scheduler runs every 5s, Poller every 10s.
        // This might take a while, so we increase timeout.
        // Initial -> FAILED (Poller) -> Retry 1 -> FAILED (Poller) -> Retry 2 -> FAILED (Poller) -> Stop
        
        await().atMost(45, TimeUnit.SECONDS).untilAsserted(() -> {
            PhoneNumber pn = phoneNumberRepository.findAll().get(0);
            assertThat(pn.getRetriesAttempted()).isEqualTo(2); // Should have retried twice
            assertThat(pn.getStatus()).isEqualTo(CallStatus.FAILED);
        });
    }

    @Test
    void testBusinessHours() {
        CampaignRequest request = new CampaignRequest();
        request.setName("Business Hours Test");
        request.setPhoneNumbers(Arrays.asList("9999999999"));
        
        // Set hours in the past
        LocalTime now = LocalTime.now();
        request.setStartTime(now.minusHours(5));
        request.setEndTime(now.minusHours(4));
        request.setTimezone(java.time.ZoneId.systemDefault().getId());

        CampaignResponse campaign = campaignService.createCampaign(request);
        campaignService.startCampaign(campaign.getId());

        // Wait a bit and verify no calls triggered
        try { Thread.sleep(6000); } catch (InterruptedException e) {}
        
        List<PhoneNumber> numbers = phoneNumberRepository.findAll();
        assertThat(numbers.get(0).getStatus()).isEqualTo(CallStatus.PENDING);
    }
}
