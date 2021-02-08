package com.softhaxi.marves.core.scheduler;

import static java.util.Map.entry;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.domain.messaging.NotificationStatus;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationStatusRepository;
import com.softhaxi.marves.core.service.employee.EmployeeInfoService;
import com.softhaxi.marves.core.service.message.MessageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BirthdayScheduler {
    private static final Logger logger = LoggerFactory.getLogger(BirthdayScheduler.class);

    @Autowired
    private EmployeeInfoService employeeInfoService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private NotificationStatusRepository statusRepo;

    @Autowired
    private MessageService messageService;

    @Scheduled(cron = "${cron.birthday.batch}")
    public void saveBirthdayNotification() {
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        logger.debug("[saveBirthdayNotification] Start at " + LocalDateTime.now());
        Collection<Map<?, ?>> data = (Collection<Map<?, ?>>) employeeInfoService.getTodayBirthdayList();
        Collection<User> activeUsers = userRepo.findAllActiveMobileUser();
        Collection<String> oneSignalIds = new LinkedList<>();
        activeUsers.forEach((user) -> {
            if(user.getStatus().equalsIgnoreCase("active") && user.getOneSignalId() != null &&
                !user.getOneSignalId().isEmpty()) {
                    oneSignalIds.add(user.getOneSignalId());
                }
        });

        data.forEach(item -> {
            User user = userRepo.findByUsername((String)item.get("email")).orElse(null);
            String name = item.get("name").toString();
            String division = item.get("unit").toString();
            // String birthDate = item.get("birthDate").toString();
            Notification notification = new Notification()
                .level("PUBLIC")
                .assignee("ALL")
                .category("BIRTHDAY")
                .deepLink("core://marves.dev/notification/birthday")
                .referenceId(user != null ? user.getId().toString() : (String)item.get("email"))
                .uri("/notification");
            notification.setContent(String.format("%s|%s|%s", name, division, item.get("birthDate")));
            
            notification.setDateTime(ZonedDateTime.now());

            Collection<NotificationStatus> oneSignalStatuses = new LinkedList<>();
            Collection<NotificationStatus> statuses = new LinkedList<>();
            activeUsers.forEach((activeUser) -> {
                if(activeUser.getStatus().equalsIgnoreCase("active") && activeUser.getOneSignalId() != null &&
                    !activeUser.getOneSignalId().isEmpty()) {
                        oneSignalStatuses.add(new NotificationStatus(notification, activeUser, false, false));
                    } else {
                        statuses.add(new NotificationStatus(notification, activeUser, false, false));
                    }
            });

            notificationRepo.save(notification);
            statusRepo.saveAll(statuses);

            Map<String, Object> body = new HashMap<>(Map.ofEntries(
                entry("headings", Map.of("en", "Selamat Ulang Tahun")),
                entry("contents", Map.of("en", String.format("Sdr/i %s (%s) berulang tahun hari ini", name, division))),
                entry("data", Map.of("deepLink", notification.getDeepLink(), 
                    "view", "detail", 
                    "refId", notification.getId())),
                entry("include_player_ids", oneSignalIds),
                entry("small_icon", "ic_stat_marves"),
                entry("android_accent_color", "FF19A472"),
                entry("android_channel_id", "066ee9a7-090b-4a42-b084-0dcbbeb7f158"),
                entry("android_group", "birthday")
            ));
            messageService.sendPushNotification(notification, oneSignalStatuses, body);
            
            // logger.info(notification.toString());
        });
        

        logger.debug("[saveBirthdayNotification] Finish at " + LocalDateTime.now());
    }
}
