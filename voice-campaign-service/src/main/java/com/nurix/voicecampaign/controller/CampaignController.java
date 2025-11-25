package com.nurix.voicecampaign.controller;

import com.nurix.voicecampaign.dto.CampaignRequest;
import com.nurix.voicecampaign.dto.CampaignResponse;
import com.nurix.voicecampaign.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(campaignService.createCampaign(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaign(id));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<CampaignResponse> startCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.startCampaign(id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<CampaignResponse> pauseCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.pauseCampaign(id));
    }
}
