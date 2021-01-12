package com.softhaxi.marves.core.scheduler;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;
import com.softhaxi.marves.core.service.employee.EmployeeInfoService;

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

    @Scheduled(cron = "${cron.birthday.batch}")
    public void saveBirthdayNotification() {
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        logger.debug("[saveBirthdayNotification] Start at " + LocalDateTime.now());
        Collection<Map<?, ?>> data = (Collection<Map<?, ?>>) employeeInfoService.getTodayBirthdayList();
        data.forEach(item -> {
            User user = userRepo.findByUsername((String)item.get("email")).orElse(null);
            Notification notification = new Notification()
                .level("PUBLIC")
                .assignee("ALL")
                .category("BIRTHDAY")
                .deepLink("core://marves.dev/notification/birthday")
                .referenceId(user != null ? user.getId().toString() : (String)item.get("email"))
                .uri("/notification");
            notification.setContent(String.format("%s|%s|%s", item.get("name"), item.get("unit"), item.get("birthDate")));
            
            notification.setDateTime(ZonedDateTime.now());
            notificationRepo.save(notification);
            // logger.info(notification.toString());
        });
        logger.debug("[saveBirthdayNotification] Finish at " + LocalDateTime.now());
    }
}
