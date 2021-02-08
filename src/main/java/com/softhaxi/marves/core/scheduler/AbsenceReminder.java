package com.softhaxi.marves.core.scheduler;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.repository.account.UserRepository;

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
    private UserRepository userRepo;

    @Autowired
    @Qualifier("oneSignalRestTemplate")
    private RestTemplate restTemplate;

    @Value("${onesignal.app.id}")
    private String appId;

    @Value("${onesignal.notification.endpoint}")
    private String notificationEndPoint;

    @Scheduled(cron = "${cron.absence.batch}") 
    public void sendNotification() {
        logger.debug("[sendNotification] Start at " + LocalDateTime.now());
        Collection<User> activeUsers = userRepo.findAllActiveMobileUser();
        Collection<String> oneSignalIds = new LinkedList<>();
        activeUsers.forEach((user) -> {
            if(user.getStatus().equalsIgnoreCase("active") && user.getOneSignalId() != null &&
                !user.getOneSignalId().isEmpty()) {
                    if(!oneSignalIds.contains(user.getOneSignalId()))
                        oneSignalIds.add(user.getOneSignalId());
                }
        });

        logger.info("[sendNotification] Number of ids..." + oneSignalIds.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json; charset=utf-8");
        Map<?, ?> body = Maps.of(
            "app_id", appId,
            // "included_segments", Arrays.asList("Maritim Users"),
            //"excluded_segments", Arrays.asList("Tester Users"),
            "template_id", "8d98a080-04d2-4873-bf29-3c463ce6866a",
            "include_player_ids", oneSignalIds
            // "exclude_player_ids", Arrays.asList("eb73dd6a-1a65-4b7c-9a65-472b9060a4c7")
        );
        HttpEntity<Map<?, ?>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<?> response = restTemplate.postForEntity(notificationEndPoint, entity, Map.class);
    
        logger.info("[sendNotification] Result....{}", response.getBody());
        logger.debug("[sendNotification] Finish at " + LocalDateTime.now());
    }
}
