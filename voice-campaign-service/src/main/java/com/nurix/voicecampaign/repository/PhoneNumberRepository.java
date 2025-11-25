package com.nurix.voicecampaign.repository;

import com.nurix.voicecampaign.model.CallStatus;
import com.nurix.voicecampaign.model.PhoneNumber;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, Long> {
    
    @Query("SELECT p FROM PhoneNumber p WHERE p.campaign.id = :campaignId AND p.status = :status")
    List<PhoneNumber> findByCampaignIdAndStatus(@Param("campaignId") Long campaignId, @Param("status") CallStatus status, Pageable pageable);

    long countByCampaignIdAndStatus(Long campaignId, CallStatus status);

    @Query("SELECT p FROM PhoneNumber p WHERE p.campaign.id = :campaignId AND (p.status = 'PENDING' OR (p.status = 'FAILED' AND p.retriesAttempted < :maxRetries)) ORDER BY p.lastAttemptTime ASC NULLS FIRST")
    List<PhoneNumber> findEligibleNumbers(@Param("campaignId") Long campaignId, @Param("maxRetries") int maxRetries, Pageable pageable);
}
