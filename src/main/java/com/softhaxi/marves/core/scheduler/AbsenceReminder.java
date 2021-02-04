package com.softhaxi.marves.core.scheduler;

import java.util.Arrays;
import java.util.Map;

import org.apache.groovy.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AbsenceReminder {

    private static final Logger logger = LoggerFactory.getLogger(AbsenceReminder.class);

    @Autowired
    @Qualifier("oneSignalRestTemplate")
    private RestTemplate restTemplate;

    @Value("${onesignal.app.id}")
    private String appId;

    @Value("${onesignal.notification.endpoint}")
    private String notificationEndPoint;

    //@Scheduled(cron = "${cron.absence.batch}") 
    public void sendNotification() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json; charset=utf-8");
        Map<?, ?> body = Maps.of(
            "app_id", appId,
            "included_segments", Arrays.asList("Subscribed Users"),
            "template_id", "8d98a080-04d2-4873-bf29-3c463ce6866a"
        );
        HttpEntity<Map<?, ?>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<?> response = restTemplate.postForEntity(notificationEndPoint, entity, Map.class);
    
        logger.info("[sendNotification] Result....{}", response.getBody());
    }
}
