package com.softhaxi.marves.core.controller.common;

import static java.util.Map.entry;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Invitation;
import com.softhaxi.marves.core.domain.employee.InvitationMember;
import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.domain.messaging.NotificationStatus;
import com.softhaxi.marves.core.model.request.InvitationRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.employee.InvitationMemberRepository;
import com.softhaxi.marves.core.repository.employee.InvitationRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationStatusRepository;
import com.softhaxi.marves.core.service.message.MessageService;
import com.softhaxi.marves.core.service.storage.FileStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * AgendaController
 */
@Controller
@RequestMapping("/agenda")
public class AgendaController {

    Logger logger = LoggerFactory.getLogger(AgendaController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private InvitationMemberRepository invitationMemberRepo;

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private NotificationStatusRepository notificationStatusRepo;

    @Autowired
    private FileStorageService storageService;

    @Autowired
    private MessageService messageService;

    @GetMapping()
    public String index() {
        return "agenda/index-temp";
    }

    @PostMapping()
    public ResponseEntity<?> action(@ModelAttribute InvitationRequest request) {
        
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = null;
        if (principal != null) {
            if (principal instanceof LdapUserDetailsImpl) {
                LdapUserDetails ldapUser = (LdapUserDetailsImpl) principal;
                userId = ldapUser.getUsername();
            } else {
                userId = principal.toString();
            }
        } else {
            userId = principal.toString();
        }
        User user = userRepository.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));

        Invitation invitation = null;
        if(request.getAction().equalsIgnoreCase("delete")) {
            invitation = invitationRepository.findById(UUID.fromString(request.getId())).orElse(null);
            if(invitation == null) {
                return new ResponseEntity<>(
                    new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), "item.not.found"),
                    HttpStatus.NOT_FOUND
                );
            }
            invitation.setDeleted(true);
            invitationRepository.save(invitation);
            return new ResponseEntity<>(
                new GeneralResponse(
                    HttpStatus.OK.value(),
                    HttpStatus.OK.getReasonPhrase(),
                    Map.of("status", "deleted", "id", invitation.getId())
                ),
                HttpStatus.OK
            );
        }

        String path = null;
        if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
            try {
                String folder = String.format("/%s/%s", "invitation", new SimpleDateFormat("yyyyMMdd").format(new Date()));
                path = storageService.store(folder, null, request.getAttachment());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        LocalDate startDate = request.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = request.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        ZonedDateTime startTime = ZonedDateTime.parse(request.getStartTime(),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        ZonedDateTime endTime = ZonedDateTime.parse(request.getEndTime(),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        if(request.getAction().equalsIgnoreCase("create")) {
            invitation = new Invitation()
                .code(request.getCode())
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription() : request.getTitle())
                .location(request.getLocation())
                .startDate(startDate)
                .endDate(endDate)
                .startTime(startTime.toInstant().atZone(ZoneId.systemDefault()))
                .endTime(endTime.toInstant().atZone(ZoneId.systemDefault()))
                .category(request.getCategory().toUpperCase())
                .user(user);
            if(path != null) {
                invitation.setFileName(request.getAttachment().getOriginalFilename());
                invitation.setAttachement(path);
            }
        }
        invitationRepository.save(invitation);

        Notification notification = new Notification()
                .level("PRIVATE")
                .assignee("SPECIFIC")
                .category("TASK")
                .deepLink("core://marves.dev/invitation")
                .referenceId(invitation.getId().toString())
                .uri("/invitation");
        notification.setUser(user);
        notification.setContent(String.format("%s|%s", invitation.getTitle(), invitation.getStartTime().toString()));
        notification.setDateTime(ZonedDateTime.now());
        notificationRepo.save(notification);
        
        List<String> inviteeEmails = request.getInvitee() != null ? new LinkedList<>(Arrays.asList(request.getInvitee().split(";"))) : null;
        List<InvitationMember> invitees = new ArrayList<>();
        List<String> oneSignalIds = new LinkedList<>();
        List<NotificationStatus> oneSignalStatuses = new LinkedList<>();
        List<NotificationStatus> statuses = new LinkedList<>();
        invitees.add(new InvitationMember()
            .invitation(invitation)
            .user(user)
            .organizer(true)
            .response("ACCEPT"));
        if(inviteeEmails != null && !inviteeEmails.isEmpty()) {
            for(String email: inviteeEmails) {
                User invitee = userRepository.findByUsernameOrEmailIgnoreCase(email).orElse(null);
                if(invitee == null) {
                    invitee = new User()
                        .email(email)
                        .username(email.substring(0, email.indexOf("@")).toUpperCase())
                        .status("INACTIVE");
                    invitee.setIsLDAPUser(true);
                    userRepository.save(invitee);
                }
                if(!user.equals(invitee)) {
                    invitees.add(new InvitationMember()
                        .invitation(invitation)
                        .user(invitee));
                    if(invitee.getOneSignalId() != null && !invitee.getOneSignalId().isEmpty()) {
                        oneSignalStatuses.add(new NotificationStatus(
                            notification,
                            invitee, false, false
                        ));
                        if(!oneSignalIds.contains(invitee.getOneSignalId()))
                            oneSignalIds.add(invitee.getOneSignalId());
                    } else
                        statuses.add(new NotificationStatus(notification, invitee, false, false));
                }
            }
        }
        if(notification != null && oneSignalIds != null && !oneSignalIds.isEmpty()) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
            Locale locale = new Locale("in", "ID");
            String date;
            if(invitation.getStartDate().equals(invitation.getEndDate())) {
                date = String.format("pada %s", invitation.getStartDate().format(dateFormatter.withLocale(locale)));
            } else {
                date = String.format("dari %s sampai %s", 
                    invitation.getStartDate().format(dateFormatter.withLocale(locale)),
                    invitation.getEndDate().format(dateFormatter.withLocale(locale)));
            }

            String time = String.format("pukul %s sampai %s",
                invitation.getStartTime().format(timeFormat.withLocale(locale)),
                invitation.getEndTime().format(timeFormat.withLocale(locale)));

            Map<String, Object> body = new HashMap<>(Map.ofEntries(
                entry("headings", Map.of("en", "Undangan Agenda")),
                entry("contents", Map.of("en", String.format("Anda diundang untuk mengikuti agenda %s %s %s", 
                    invitation.getTitle(), 
                    date, time))),
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
        invitationMemberRepo.saveAll(invitees);
        notificationStatusRepo.saveAll(statuses);
        
        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.CREATED.value(),
                HttpStatus.CREATED.getReasonPhrase(),
                invitation
            ),
            HttpStatus.CREATED
        );
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(name = "category", required = false, defaultValue = "all") String category,
            @RequestParam(name = "email", required = false) String email) {

        Collection<Invitation> invitations = invitationRepository.findAllNotDeleted();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Map<String, Object> map = null;
        Collection<Map<?, ?>> data = new LinkedList<>();
        for (Invitation invitation : invitations) {
            map = new HashMap<>();
            map.put("id", invitation.getId().toString());
            map.put("text", invitation.getTitle());
            map.put("description", invitation.getDescription());
            map.put("category", invitation.getCategory().toLowerCase());
            map.put("location", invitation.getLocation());
            map.put("editable", false);
            map.put("removable", true);
            if (invitation.getCategory().equalsIgnoreCase("inoffice")) {
                map.put("color", "#4AC8F1");
            } else if (invitation.getCategory().equalsIgnoreCase("outoffice")) {
                map.put("color", "#FB852F");
            } else if (invitation.getCategory().equalsIgnoreCase("online")) {
                map.put("color", "#EAC600");
            }
            if(invitation.getAttachement() != null) {
                map.put("fileName", invitation.getFileName());
                map.put("attachmentUrl", invitation.getAttachmentUrl());
            }
            List<String> members = new LinkedList<>();
            Collection<InvitationMember> invitees = new LinkedList<>(invitation.getInvitees());
            var organizer = invitees.stream().filter((element) -> element.isOrganizer())
                .findFirst().orElse(null);
            if(organizer != null) {
                invitees.remove(organizer);
                if(organizer.getUser().getProfile() != null) {
                    members.add(organizer.getUser().getProfile().getFullName() + " (Organizer)");
                } else {
                    members.add(organizer.getUser().getEmail() + " (Organizer)");
                }
            }
            for (InvitationMember member : invitees) {
                if(member.getUser().getProfile() != null) {
                    members.add(member.getUser().getProfile().getFullName());
                } else {
                    members.add(member.getUser().getEmail());
                }
            }
            map.put("members", String.join(", ", members));
            if (invitation.getStartDate().equals(invitation.getEndDate())) {
                var date = invitation.getStartDate();
                var startTime = invitation.getStartTime();
                var endTime = invitation.getEndTime();
                map.put("start_date", LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                        startTime.getHour(), startTime.getMinute(), startTime.getSecond()).format(formatter));
                map.put("end_date", LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                        endTime.getHour(), endTime.getMinute(), endTime.getSecond()).format(formatter));
                data.add(map);
            } else {
                // var date = invitation.getStartDate();
                var startTime = invitation.getStartTime();
                var endTime = invitation.getEndTime();
                final long days = invitation.getStartDate().until(invitation.getEndDate().plusDays(1), ChronoUnit.DAYS);
                var dateRange = Stream.iterate(invitation.getStartDate(), d -> d.plusDays(1)).limit(days)
                        .collect(Collectors.toList());

                int id = 1;
                for (LocalDate date : dateRange) {
                    Map<String, Object> temp = new HashMap<>(map);
                    temp.put("id", map.get("id") + "_" + id);
                    temp.put("event_id", map.get("id"));
                    temp.put("start_date",
                            LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                                    startTime.getHour(), startTime.getMinute(), startTime.getSecond())
                                    .format(formatter));
                    temp.put("end_date", LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                            endTime.getHour(), endTime.getMinute(), endTime.getSecond()).format(formatter));
                    temp.put("rec_type", String.format("day_1___#%d", id));
                    // System.out.println(temp);
                    data.add(temp);
                    id++;
                }
            }
            // data.add(map);
        }
        return new ResponseEntity<>(new GeneralResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data),
                HttpStatus.OK);
    }
}