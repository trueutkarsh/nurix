package com.nurix.voicecampaign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VoiceCampaignServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(VoiceCampaignServiceApplication.class, args);
	}

}
