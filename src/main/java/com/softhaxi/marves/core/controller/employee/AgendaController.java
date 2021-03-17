package com.softhaxi.marves.core.controller.employee;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Invitation;
import com.softhaxi.marves.core.domain.employee.InvitationMember;
import com.softhaxi.marves.core.domain.request.InvitationRequest;
import com.softhaxi.marves.core.domain.response.ErrorResponse;
import com.softhaxi.marves.core.domain.response.SuccessResponse;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.employee.InvitationRepository;
import com.softhaxi.marves.core.service.employee.InvitationService;

import org.apache.catalina.connector.Response;
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
import org.springframework.web.bind.annotation.PathVariable;
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
    private UserRepository userRepo;

    @Autowired
    private InvitationRepository invitationRepo;

    @Autowired
    private InvitationService invitationService;

    @GetMapping()
    public String index() {
        return "agenda/index";
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
        User user = userRepo.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));

        Invitation invitation = null;
        try {
            invitation = invitationService.save(user, request, true);
        } catch(Exception ex) {
            logger.error("[action]Exception...", ex);
            if(ex.getMessage().equalsIgnoreCase("item.not.found")) {
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

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
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
        var user = userRepo.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));

        var invitation = invitationRepo.findById(UUID.fromString(id)).orElse(null);
        if(invitation == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(),
                    "item.not.found"),
                    HttpStatus.NOT_FOUND
            );
        }
        Set<Map<String, Object>> members = new HashSet<>();
        invitation.getInvitees().forEach((member) -> {
            if(!member.isDeleted() && !member.getUser().equals(user)) {
                Map<String, Object> temp = new HashMap<>();
                temp.put("email", member.getUser().getEmail());
                temp.put("id", member.getUser().getId());
                temp.put("name", member.getUser().getProfile() != null ? member.getUser().getProfile().getFullName() : "");
                temp.put("response", member.getResponse());
                temp.put("organizer", member.getOrganizer());
                members.add(temp);
            }
        });
        invitation.setMembers(members);

        return new ResponseEntity<>(
                new SuccessResponse(HttpStatus.OK.value(), 
                    HttpStatus.OK.getReasonPhrase(), invitation),
                HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(name = "category", required = false, defaultValue = "all") String category,
            @RequestParam(name = "email", required = false) String email) {

        Collection<Invitation> invitations = invitationRepo.findAllNotDeleted();

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
            map.put("editable", true);
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
        return new ResponseEntity<>(new SuccessResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data),
                HttpStatus.OK);
    }
}