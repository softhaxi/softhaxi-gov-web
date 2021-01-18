package com.softhaxi.marves.core.restful.employement;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Invitation;
import com.softhaxi.marves.core.domain.employee.InvitationMember;
import com.softhaxi.marves.core.domain.messaging.Message;
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
import com.softhaxi.marves.core.service.storage.FileStorageService;

import org.apache.catalina.connector.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping()
    public ResponseEntity<?> index(@RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        if (date == null)
            date = LocalDate.now();

        Collection<Invitation> invitations = invitationRepo.findAllUserDailyInvitation(user, date);

        invitations.forEach((invitation) -> {
            // logger.info("[index] Invitation member ==" + invitation.getInvitees());
            Set<Map<String, Object>> members = new HashSet<>();
            invitation.getInvitees().forEach((member) -> {
                Map<String, Object> temp = new HashMap<>();
                temp.put("email", member.getUser().getEmail());
                temp.put("id", member.getUser().getId());
                temp.put("fullName", member.getUser().getProfile() != null ? member.getUser().getProfile().getFullName() : "");
                temp.put("response", member.getResponse());
                temp.put("organizer", member.getOrganizer());
                members.add(temp);
                if(member.getUser().equals(user)) {
                    invitation.setCompleted(member.getStatus() != null && member.getStatus().equalsIgnoreCase("ATTENDED"));
                }
            });
            invitation.setMembers(members);
        });
        //logger.info("[Index] number of invitation = " + invitations.size());

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                invitations
            ),
            HttpStatus.OK   
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
                path = storageService.store(folder, new SimpleDateFormat("HHmmss").format(new Date()), file);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        String[] times = request.getStartTime().split(":");
        LocalDate startDate = request.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        ZonedDateTime startTime = ZonedDateTime.of(startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth(),
            Integer.parseInt(times[0]), Integer.parseInt(times[1]), 0, 0, ZoneId.systemDefault());

        Invitation invitation = new Invitation()
            .code(request.getCode().trim())
            .title(request.getTitle().trim())
            .description(request.getDescription())
            .location(request.getLocation())
            .startTime(startTime)
            .category(request.getCategory().toUpperCase())
            .user(user);
        if(path != null) {
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
        notification.setContent(String.format("%s|%s", invitation.getTitle(), invitation.getStartTime().toString()));
        notification.setDateTime(ZonedDateTime.now());
        notificationRepo.save(notification);
        
        String[] inviteeEmails = request.getInvitee().split(";");
        List<InvitationMember> invitees = new ArrayList<>();
        invitees.add(new InvitationMember()
            .invitation(invitation)
            .user(user)
            .organizer(true)
            .response("ACCEPT"));
        List<NotificationStatus> statuses = new ArrayList<>();
        for(String email: inviteeEmails) {
            User invitee = userRepo.findByUsernameOrEmailIgnoreCase(email).orElse(null);
            if(invitee == null) {
                invitee = new User()
                    .email(email)
                    .username(email.substring(0, email.indexOf("@")).toUpperCase());
                invitee.setIsLDAPUser(true);
                userRepo.save(invitee);
            }
            if(!user.equals(invitee)) {
                invitees.add(new InvitationMember()
                    .invitation(invitation)
                    .user(invitee));
                statuses.add(new NotificationStatus(
                    (Message) notification,
                    invitee, false, false
                ));
            }
        }
        invitationMemberRepo.saveAll(invitees);
        notificationStatusRepo.saveAll(statuses);
        
        return new ResponseEntity<>(
            new GeneralResponse(
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
        Invitation invitation = invitationRepo.getByUserAndId(user, UUID.fromString(id)).orElse(null);
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
            temp.put("fullName", member.getUser().getProfile() != null ? member.getUser().getProfile().getFullName() : "");
            temp.put("response", member.getResponse());
            temp.put("organizer", member.getOrganizer());
            members.add(temp);

            if(member.getUser().equals(user)) {
                invitation.setCompleted(member.getStatus() == "ATTENDED");
            }
        });
        invitation.setMembers(members);

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                invitation
             ),
            HttpStatus.OK
        );
    }

    @GetMapping("/complete/{id}")
    public ResponseEntity<?> complete(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        logger.info("[complete] complete invitation ");
        InvitationMember member = invitationMemberRepo.getByUserAndInvitationId(user, UUID.fromString(id)).orElse(null);
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

        Invitation invitation = invitationRepo.getByUserAndId(user, UUID.fromString(id)).orElse(null);
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
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                "invitation.completed"
             ),
            HttpStatus.OK
        );
    }
}
