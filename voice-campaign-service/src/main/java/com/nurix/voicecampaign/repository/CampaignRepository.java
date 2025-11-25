package com.nurix.voicecampaign.repository;

import com.nurix.voicecampaign.model.Campaign;
import com.nurix.voicecampaign.model.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByStatus(CampaignStatus status);
}
