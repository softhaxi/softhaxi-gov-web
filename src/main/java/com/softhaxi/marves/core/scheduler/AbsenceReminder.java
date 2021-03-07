package com.softhaxi.marves.core.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.master.CalendarEvent;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.master.CalendarEventRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;

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
    private SystemParameterRepository parameterRepo;

    @Autowired
    private CalendarEventRepository calendarEventRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    @Qualifier("oneSignalRestTemplate")
    private RestTemplate restTemplate;

    @Value("${onesignal.app.id}")
    private String appId;

    @Value("${onesignal.notification.endpoint}")
    private String notificationEndPoint;

    @Scheduled(cron = "${cron.clockin.batch}")
    public void sendClockInNotification() {
        logger.debug("[sendClockInNotification] Start at " + LocalDateTime.now());

        try {
            SystemParameter enabledClockIn = parameterRepo.findByCode("CLOCKIN_REMINDER_ENABLED")
                .orElse(new SystemParameter().value("Y"));
            if(enabledClockIn.getValue().equalsIgnoreCase("N")) {
                logger.debug("[sendClockInNotification] Not send clock in reminder due to disabled in parameter");
                return;
            }

            CalendarEvent event = calendarEventRepo.findOneHolidayByDate(LocalDate.now()).orElse(null);
            if (event != null) {
                logger.debug("[sendClockInNotification] Not send clock in reminder due to holiday " + event.getName());
                return;
            }
            SystemParameter clockInTime = parameterRepo.findByCode("CLOCKIN_REMINDER_TIME")
                .orElse(new SystemParameter().value("7:30"));
            String[] times = clockInTime.getValue().split(":");
            ZonedDateTime now = ZonedDateTime.now();
            if(now.getHour() != Integer.parseInt(times[0]) || now.getMinute() != Integer.parseInt(times[1])) {
                logger.debug("[sendClockInNotification] Not send clock in reminder due to not clock in time ");
                return;
            }
            
            Collection<User> activeUsers = userRepo.findAllActiveMobileUser();
            Collection<String> oneSignalIds = new LinkedList<>();
            activeUsers.forEach((user) -> {
                if (user.getStatus().equalsIgnoreCase("active") && user.getOneSignalId() != null
                        && !user.getOneSignalId().isEmpty()) {
                    if (!oneSignalIds.contains(user.getOneSignalId()))
                        oneSignalIds.add(user.getOneSignalId());
                }
            });

            logger.info("[sendClockInNotification] Number of ids..." + oneSignalIds.toString());
            if (oneSignalIds != null && !oneSignalIds.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", "application/json; charset=utf-8");
                Map<?, ?> body = Maps.of("app_id", appId,
                        // "included_segments", Arrays.asList("Maritim Users"),
                        // "excluded_segments", Arrays.asList("Tester Users"),
                        "template_id", "8d98a080-04d2-4873-bf29-3c463ce6866a", "include_player_ids", oneSignalIds
                // "exclude_player_ids", Arrays.asList("eb73dd6a-1a65-4b7c-9a65-472b9060a4c7")
                );
                HttpEntity<Map<?, ?>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<?> response = restTemplate.postForEntity(notificationEndPoint, entity, Map.class);

                logger.info("[sendClockInNotification] Result....{}", response.getBody());
            }
        } finally {
            logger.debug("[sendClockInNotification] Finish at " + LocalDateTime.now());
        }

        
    }

    @Scheduled(cron = "${cron.clockout.batch}")
    public void sendClockOutNotification() {
        logger.debug("[sendClockOutNotification] Start at " + LocalDateTime.now());

        try {
            SystemParameter enabledClockOut = parameterRepo.findByCode("CLOCKOUT_REMINDER_ENABLED")
                .orElse(new SystemParameter().value("Y"));
            if(enabledClockOut.getValue().equalsIgnoreCase("N")) {
                logger.debug("[sendClockOutNotification] Not send clock out reminder due to disabled in parameter");
                return;
            }

            CalendarEvent event = calendarEventRepo.findOneHolidayByDate(LocalDate.now()).orElse(null);
            if (event != null) {
                logger.debug("[sendClockOutNotification] Not send clock out reminder due to holiday " + event.getName());
                return;
            }
            SystemParameter clockOutTime = parameterRepo.findByCode("CLOCKOUT_REMINDER_TIME")
                .orElse(new SystemParameter().value("7:30"));
            String[] times = clockOutTime.getValue().split(":");
            ZonedDateTime now = ZonedDateTime.now();
            if(now.getHour() != Integer.parseInt(times[0]) || now.getMinute() != Integer.parseInt(times[1])) {
                logger.debug("[sendClockOutNotification] Not send clock out reminder due to not clock in time ");
                return;
            }
            
            Collection<User> activeUsers = userRepo.findAllActiveMobileUser();
            Collection<String> oneSignalIds = new LinkedList<>();
            activeUsers.forEach((user) -> {
                if (user.getStatus().equalsIgnoreCase("active") && user.getOneSignalId() != null
                        && !user.getOneSignalId().isEmpty()) {
                    if (!oneSignalIds.contains(user.getOneSignalId()))
                        oneSignalIds.add(user.getOneSignalId());
                }
            });

            logger.info("[sendClockOutNotification] Number of ids..." + oneSignalIds.toString());
            if (oneSignalIds != null && !oneSignalIds.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", "application/json; charset=utf-8");
                Map<?, ?> body = Maps.of("app_id", appId,
                        // "included_segments", Arrays.asList("Maritim Users"),
                        // "excluded_segments", Arrays.asList("Tester Users"),
                        "template_id", "d94fe39c-f8c4-4d76-8105-15e147bf8894", "include_player_ids", oneSignalIds
                // "exclude_player_ids", Arrays.asList("eb73dd6a-1a65-4b7c-9a65-472b9060a4c7")
                );
                HttpEntity<Map<?, ?>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<?> response = restTemplate.postForEntity(notificationEndPoint, entity, Map.class);

                logger.info("[sendClockOutNotification] Result....{}", response.getBody());
            }
        } finally {
            logger.debug("[sendClockOutNotification] Finish at " + LocalDateTime.now());
        }
    }
}
