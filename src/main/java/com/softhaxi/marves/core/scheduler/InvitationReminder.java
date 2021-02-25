package com.softhaxi.marves.core.scheduler;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Map.entry;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Invitation;
import com.softhaxi.marves.core.domain.employee.InvitationMember;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.domain.messaging.NotificationStatus;
import com.softhaxi.marves.core.repository.employee.InvitationMemberRepository;
import com.softhaxi.marves.core.repository.employee.InvitationRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationStatusRepository;
import com.softhaxi.marves.core.service.message.MessageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InvitationReminder {
    private static final Logger logger = LoggerFactory.getLogger(InvitationReminder.class);

    @Autowired
    private SystemParameterRepository parameterRepo;

    @Autowired
    private InvitationRepository invitationRepo;

    @Autowired
    private InvitationMemberRepository memberRepo;

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private NotificationStatusRepository statusRepo;

    @Autowired
    private MessageService messageService;

    @Scheduled(cron = "${cron.incoming.invitation.batch}")
    public void sendIncoming() {
        logger.debug("[sendIncoming] Start at " + LocalDateTime.now());
        try {
            SystemParameter intervalParameter = parameterRepo.findByCode("AGENDA_REMINDER_BEFORE").orElse(new SystemParameter().value("15"));

            ZonedDateTime date = ZonedDateTime.now();
            date = date.plusMinutes(Long.parseLong(intervalParameter.getValue()));
            logger.debug("[sendIncoming] Invitation date..." + date);
            Collection<Invitation> invitations = invitationRepo.findAllDailyInvitationByTime(date.toLocalDate(),
                date.format(DateTimeFormatter.ofPattern("HH:mm")));
            
            logger.debug("[sendIncoming] Number of invitation ..." + invitations.size());
            Notification notification;
            List<NotificationStatus> oneSignalStatuses;
            List<NotificationStatus> statuses;
            List<String> oneSignalIds;

            for(Invitation invitation: invitations) {
                Collection<User> users = memberRepo.findAllUserByInvitation(invitation);
                notification = new Notification()
                    .level("PRIVATE")
                    .assignee("SPECIFIC")
                    .category("TASK")
                    .deepLink("core://marves.dev/invitation/reminder")
                    .referenceId(invitation.getId().toString())
                    .uri("/invitation");
                notification.setContent(String.format("%s|%s|%s", invitation.getTitle(), 
                    invitation.getStartTime().toString(), 
                    intervalParameter.getValue()));
                notification.setDateTime(ZonedDateTime.now());
                notificationRepo.save(notification);

                oneSignalStatuses = new LinkedList<>();
                statuses = new LinkedList<>();
                oneSignalIds = new LinkedList<>();
                for(User user: users) {
                    NotificationStatus status = new NotificationStatus(notification, user, false, false);
                    if(user.getStatus().equalsIgnoreCase("active") && user.getOneSignalId() != null 
                        && !user.getOneSignalId().isEmpty()) {
                        oneSignalStatuses.add(status);
                        if(!oneSignalIds.contains(user.getOneSignalId()))
                            oneSignalIds.add(user.getOneSignalId());
                    } else
                        statuses.add(status);
                }
                if(notification != null && oneSignalIds != null && !oneSignalIds.isEmpty()) {
                    Map<String, Object> body = new HashMap<>(Map.ofEntries(
                        entry("headings", Map.of("en", "Pengingat Agenda")),
                        entry("contents", Map.of("en", String.format("Agenda %s akan berlansung dalam %s menit", 
                            invitation.getTitle(), 
                            intervalParameter.getValue()))),
                        entry("data", Map.of("deepLink", notification.getDeepLink(), 
                            "view", "detail", 
                            "refId", notification.getReferenceId())),
                        entry("include_player_ids", oneSignalIds),
                        entry("small_icon", "ic_stat_marves"),
                        entry("android_channel_id", "066ee9a7-090b-4a42-b084-0dcbbeb7f158"),
                        entry("android_accent_color", "FF19A472"),
                        entry("android_group", "invitation")
                    ));
                    messageService.sendPushNotification(notification, oneSignalStatuses, body);
                }
                statusRepo.saveAll(statuses);
            }
        } catch(Exception ex) {
            logger.error("[sendIncoming] Exception... " + ex.getMessage(), ex);
        } finally {
            logger.debug("[sendIncoming] Finish at " + LocalDateTime.now());

        }
    }
}
