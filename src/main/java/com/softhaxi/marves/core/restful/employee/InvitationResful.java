package com.softhaxi.marves.core.restful.employee;

import static java.util.Map.entry;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Invitation;
import com.softhaxi.marves.core.domain.employee.InvitationMember;
import com.softhaxi.marves.core.domain.exception.BusinessException;
import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.domain.messaging.NotificationStatus;
import com.softhaxi.marves.core.domain.request.InvitationRequest;
import com.softhaxi.marves.core.domain.response.ErrorResponse;
import com.softhaxi.marves.core.domain.response.SuccessResponse;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.employee.InvitationMemberRepository;
import com.softhaxi.marves.core.repository.employee.InvitationRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationStatusRepository;
import com.softhaxi.marves.core.service.employee.InvitationService;
import com.softhaxi.marves.core.service.message.MessageService;
import com.softhaxi.marves.core.service.storage.FileStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/invitation")
public class InvitationResful {
    private static final Logger logger = LoggerFactory.getLogger(InvitationResful.class);

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private InvitationRepository invitationRepo;

    @Autowired
    private InvitationMemberRepository invitationMemberRepo;

    @Autowired
    private FileStorageService storageService;

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private NotificationStatusRepository notificationStatusRepo;

    @Autowired
    private MessageService messageService;

    @Autowired
    private InvitationService invitationService;

    @GetMapping()
    public ResponseEntity<?> index(@RequestParam(name="email", required = false) String email,
        @RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        
        if (date == null)
            date = LocalDate.now();
        
        final User specificUser = email != null ? userRepo.findByUsernameOrEmailIgnoreCase(email).orElse(null)
            : null;
        Collection<Invitation> invitations = null;
        if(email != null) {
            if(specificUser != null)
                invitations = invitationRepo.findAllUserDailyInvitation(specificUser, date);
        } else {
            invitations = invitationRepo.findAllUserDailyInvitation(user, date);
        }

        if(invitations != null) {
            invitations.forEach((invitation) -> {
                Set<Map<String, Object>> members = new HashSet<>();
                invitation.getInvitees().forEach((member) -> {
                    if(!member.isDeleted()) {
                        Map<String, Object> temp = new HashMap<>();
                        temp.put("email", member.getUser().getEmail());
                        temp.put("id", member.getUser().getId());
                        temp.put("name", member.getUser().getProfile() != null ? member.getUser().getProfile().getFullName() : "");
                        temp.put("response", member.getResponse());
                        temp.put("organizer", member.getOrganizer());
                        members.add(temp);
                        if(specificUser != null) {
                            if(member.getUser().equals(specificUser)) {
                                invitation.setCompleted(member.getStatus() != null && member.getStatus().equalsIgnoreCase("ATTENDED"));
                            }
                        } else if(member.getUser().equals(user)) {
                            invitation.setCompleted(member.getStatus() != null && member.getStatus().equalsIgnoreCase("ATTENDED"));
                        }
                    }
                });
                invitation.setMembers(members);
            });
        }

        return new ResponseEntity<>(
            new SuccessResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                invitations
            ),
            HttpStatus.OK   
        );
    }

    @PostMapping("/action")
    public ResponseEntity<?> action(@RequestParam(required = true) String payload,
        @RequestParam(value = "file", required = false) MultipartFile file) {
        InvitationRequest request;
        try {
            request = new ObjectMapper().readValue(payload, InvitationRequest.class);
        } catch (JsonProcessingException ex) {
            logger.error("[action] Exception..." + ex.getMessage(), ex);
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                    Map.of("payload", "json.required")
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        if (file != null) {
            request.setAttachment(file);
        }

        Invitation invitation = null;
        try {
            invitation = invitationService.save(user, request, false);
        } catch(Exception ex) {
            logger.error("[action]... ", ex);
            if(ex instanceof BusinessException) {
                return new ResponseEntity<>(
                    new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), ex.getMessage()),
                    HttpStatus.NOT_FOUND
                );
            }
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                HttpStatus.BAD_REQUEST.getReasonPhrase(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
            );
        }
        
        if(request.getAction().equals("edit")) {
            return new ResponseEntity<>(
                new SuccessResponse(
                    HttpStatus.OK.value(),
                    HttpStatus.OK.getReasonPhrase(),
                    invitation
                ),
                HttpStatus.OK
            );
        } else if(request.getAction().equals("delete")) {
            return new ResponseEntity<>(
                new SuccessResponse(
                    HttpStatus.OK.value(),
                    HttpStatus.OK.getReasonPhrase(),
                    Map.of("status", "deleted", "id", invitation.getId())
                ),
                HttpStatus.OK
            );
        }
        return new ResponseEntity<>(
            new SuccessResponse(
                HttpStatus.CREATED.value(),
                HttpStatus.CREATED.getReasonPhrase(),
                invitation
            ),
            HttpStatus.CREATED
        );
    }

    @PostMapping()
    public ResponseEntity<?> post(@RequestParam(required = true) String payload,
        @RequestParam(value = "file", required = false) MultipartFile file) {
        InvitationRequest request;
        try {
            request = new ObjectMapper().readValue(payload, InvitationRequest.class);
        } catch (JsonProcessingException ex) {
            logger.error("[post] Exception..." + ex.getMessage(), ex);
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                    Map.of("payload", "json.required")
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        String path = null;
        if (file != null) {
            try {
                String folder = String.format("/%s/%s", "invitation", new SimpleDateFormat("yyyyMMdd").format(new Date()));
                path = storageService.store(folder, null, file);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        LocalDate startDate = request.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = request.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        ZonedDateTime startTimeMobile = ZonedDateTime.parse(request.getStartTime(),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        ZonedDateTime endTimeMobile = ZonedDateTime.parse(request.getEndTime(),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        
        Invitation invitation = new Invitation()
            .code(request.getCode())
            .title(request.getTitle().trim())
            .description(request.getDescription() != null ? request.getDescription() : request.getTitle())
            .location(request.getLocation())
            .startDate(startDate)
            .endDate(endDate)
            .startTime(startTimeMobile.toInstant().atZone(ZoneId.systemDefault()))
            .endTime(endTimeMobile.toInstant().atZone(ZoneId.systemDefault()))
            .category(request.getCategory().toUpperCase())
            .user(user);
        if(path != null) {
            invitation.setFileName(file.getOriginalFilename());
            invitation.setAttachement(path);
        }
        invitationRepo.save(invitation);

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
                User invitee = userRepo.findByUsernameOrEmailIgnoreCase(email).orElse(null);
                if(invitee == null) {
                    invitee = new User()
                        .email(email)
                        .username(email.substring(0, email.indexOf("@")).toUpperCase())
                        .status("INACTIVE");
                    invitee.setIsLDAPUser(true);
                    userRepo.save(invitee);
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
            new SuccessResponse(
                HttpStatus.CREATED.value(),
                HttpStatus.CREATED.getReasonPhrase(),
                invitation
            ),
            HttpStatus.CREATED   
        );
    }

    @PostMapping("/edit")
    public ResponseEntity<?> edit(@RequestParam(required = true) String payload,
        @RequestParam(value = "file", required = false) MultipartFile file) {
        logger.info("[edit] Start....");
        InvitationRequest request;
        try {
            request = new ObjectMapper().readValue(payload, InvitationRequest.class);
        } catch (JsonProcessingException ex) {
            logger.error("[post] Exception..." + ex.getMessage(), ex);
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                    Map.of("payload", "json.required")
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        Invitation invitation = invitationRepo.findByUserAndId(user, UUID.fromString(request.getId())).orElse(null);
        if(invitation == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }

        String path = null;
        if (file != null) {
            try {
                String folder = String.format("/%s/%s", "invitation", new SimpleDateFormat("yyyyMMdd").format(new Date()));
                path = storageService.store(folder, null, file);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.info("[edit] File Path...." + path);

        // LocalDate startDate = invitation.getStartDate();

        // String[] startTimes = request.getStartTime().split(":");
        // String[] endTimes = request.getEndTime().split(":");

        ZonedDateTime startTimeMobile = ZonedDateTime.parse(request.getStartTime(),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        ZonedDateTime endTimeMobile = ZonedDateTime.parse(request.getEndTime(),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        
        // ZonedDateTime startTime = ZonedDateTime.of(startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth(),
        //     Integer.parseInt(startTimes[0]), Integer.parseInt(startTimes[1]), 0, 0, ZoneId.systemDefault());
        // ZonedDateTime endTime = ZonedDateTime.of(startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth(),
        //     Integer.parseInt(endTimes[0]), Integer.parseInt(endTimes[1]), 0, 0, ZoneId.systemDefault());

        //invitation.location(request.getLocation());
        invitation.setDescription(request.getDescription());
        // invitation.startTimeMobile(startTimeMobile);
        // invitation.endTimeMobile(endTimeMobile);
        invitation.startTime(startTimeMobile.toInstant().atZone(ZoneId.systemDefault()));
        invitation.endTime(endTimeMobile.toInstant().atZone(ZoneId.systemDefault()));
        if(path != null) {
            invitation.setFileName(file.getOriginalFilename());
            invitation.setAttachement(path);
        }
        //invitationRepo.save(invitation);
        
        List<InvitationMember> invitees = new LinkedList<>();
        List<String> oneSignalIds = new LinkedList<>();
        List<NotificationStatus> oneSignalStatuses = new LinkedList<>();
        List<NotificationStatus> statuses = new LinkedList<>();
        Notification notification = null;

        if(request.getInvitee() != null && !request.getInvitee().isBlank()) {
            logger.info("[edit] invitees..." + request.getInvitee());
            List<String> inviteeEmails = new LinkedList<>(Arrays.asList(request.getInvitee().split(";")));
            invitation.getInvitees().forEach((member) -> {
                if(!member.isOrganizer() && !inviteeEmails.contains(member.getUser().getEmail())) {
                    member.deleted(true);
                }
            });

            invitation.getInvitees().forEach((member) -> inviteeEmails.remove(member.getUser().getEmail()));

            if(!inviteeEmails.isEmpty()) {
                notification = notificationRepo.findOneByUserAndReferenceId(user, invitation.getId().toString())
                    .orElse(new Notification()
                        .level("PRIVATE")
                        .assignee("SPECIFIC")
                        .category("TASK")
                        .deepLink("core://marves.dev/invitation")
                        .referenceId(invitation.getId().toString())
                        .uri("/invitation"));
                if(notification.getId() == null) { 
                    notification.setUser(user);
                    notification.setContent(String.format("%s|%s", invitation.getTitle(), invitation.getStartTime().toString()));
                    notification.setDateTime(ZonedDateTime.now());
                    notificationRepo.save(notification);
                }

                for(String email: inviteeEmails) {
                    User invitee = userRepo.findByUsernameOrEmailIgnoreCase(email).orElse(null);
                    if(invitee == null) {
                        invitee = new User()
                            .email(email)
                            .username(email.substring(0, email.indexOf("@")).toUpperCase())
                            .status("INACTIVE");
                        invitee.setIsLDAPUser(true);
                        userRepo.save(invitee);
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
            new SuccessResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                invitation
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        Invitation invitation = invitationRepo.findByUserAndId(user, UUID.fromString(id)).orElse(null);
        if(invitation == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }

        Set<Map<String, Object>> members = new HashSet<>();
        invitation.getInvitees().forEach((member) -> {
            Map<String, Object> temp = new HashMap<>();
            temp.put("email", member.getUser().getEmail());
            temp.put("id", member.getUser().getId());
            temp.put("name", member.getUser().getProfile() != null ? member.getUser().getProfile().getFullName() : "");
            temp.put("response", member.getResponse());
            temp.put("organizer", member.getOrganizer());
            members.add(temp);

            // if(member.getUser().equals(user)) {
            //     invitation.setCompleted(member.getStatus().equalsIgnoreCase("ATTENDED"));
            // }
        });
        invitation.setMembers(members);

        return new ResponseEntity<>(
            new SuccessResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                invitation
             ),
            HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        
        Invitation invitation = invitationRepo.findByUserAndId(user, UUID.fromString(id)).orElse(null);
        if(invitation == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }
        
        if(invitation.getUser().equals(user)) {
            invitation.setDeleted(true);
            invitationRepo.save(invitation);
        } else {
            InvitationMember member = invitationMemberRepo.findByUserAndInvitationId(user, invitation.getId()).orElse(null);
            if(member == null) {
                return new ResponseEntity<>(
                    new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                        HttpStatus.NOT_FOUND.getReasonPhrase(), 
                        "item.not.found"
                    ),
                    HttpStatus.NOT_FOUND
                );
            }

            member.setDeleted(true);
            invitationMemberRepo.save(member);
        }

        return new ResponseEntity<>(
            new SuccessResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                "invitation.deleted"
             ),
            HttpStatus.OK
        );
    }

    @GetMapping("/complete/{id}")
    public ResponseEntity<?> complete(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        logger.info("[complete] complete invitation ");
        InvitationMember member = invitationMemberRepo.findByUserAndInvitationId(user, UUID.fromString(id)).orElse(null);
        if(member == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }
        
        member.setStatus("ATTENDED");
        invitationMemberRepo.save(member);

        Invitation invitation = invitationRepo.findByUserAndId(user, UUID.fromString(id)).orElse(null);
        if(invitation == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }

        return new ResponseEntity<>(
            new SuccessResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                "invitation.completed"
             ),
            HttpStatus.OK
        );
    }
}
