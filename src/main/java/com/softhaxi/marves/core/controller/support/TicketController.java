package com.softhaxi.marves.core.controller.support;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Map.entry;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.logging.ActivityLog;
import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.domain.messaging.NotificationStatus;
import com.softhaxi.marves.core.domain.support.Ticket;
import com.softhaxi.marves.core.domain.support.TicketComment;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.logging.ActivityLogRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationStatusRepository;
import com.softhaxi.marves.core.repository.support.TicketCommentRepository;
import com.softhaxi.marves.core.repository.support.TicketRepository;
import com.softhaxi.marves.core.service.logging.LoggerService;
import com.softhaxi.marves.core.service.message.MessageService;
import com.softhaxi.marves.core.service.support.TicketService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/openticket")
public class TicketController {

    Logger logger = LoggerFactory.getLogger(TicketController.class);

    @Autowired
    private TicketService ticketService;

    @Autowired
    private LoggerService loggerService;

    @Autowired
    private TicketRepository ticketRepo;

    @Autowired
    private TicketCommentRepository commentRepo;

    @Autowired
    private ActivityLogRepository activityRepo;

    @Autowired 
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private NotificationStatusRepository notifStatusRepo;

    @Autowired
    private MessageService messageService;

    @GetMapping()
    public String index(Model model,
        @RequestParam(value="page", required = false, defaultValue = "1") int page,
        @RequestParam(value="status", required = false, defaultValue = "open") String status) {

        return "ticket/index";
    }

    @GetMapping("/list")
    public String list(Model model,
        @RequestParam(value="id", required = false) String id) {
        List<Ticket> tickets = (List<Ticket>) ticketRepo.findAllNonClosed();
        List<Ticket> closedTickets = (List<Ticket>) ticketRepo.findAllClosed();
        if(tickets.isEmpty()) tickets = closedTickets;
        else tickets.addAll(closedTickets);
        
        if(id != null)
            model.addAttribute("initialId", id);
        else
            model.addAttribute("initialId", tickets.get(0).getId());
        model.addAttribute("data", tickets);

        return "ticket/list";
    }

    @GetMapping("/{id}")
    public String action(Model model, @PathVariable String id,
        @RequestParam(value="action", defaultValue="detail") String action) {
        
        Ticket ticket = ticketRepo.findById(UUID.fromString(id)).orElse(null);
        if(ticket == null) {
            model.addAttribute("error", "Tidak ada tiket");
            return "ticket/index";
        }

        model.addAttribute("data", ticket);
        model.addAttribute("action", action);
        model.addAttribute("activities", activityRepo.findAllByRefIdOrderByActionTimeDesc(ticket.getId().toString()));
        model.addAttribute("comments", commentRepo.findAllByTicketOrderByCreatedAtDesc(ticket));
        // ticketService.performAction(ticket, action);
        
        // model.addAttribute("data", ticketService.findAllOrderByDateTimeDesc());
        return "ticket/action";
    }

    @PostMapping("/status")
    public String status(Model model,
        @RequestParam(name = "id") String id, 
        @RequestParam(name = "status") String newStatus) {
        
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = null;
        if(principal != null) {
            if(principal instanceof LdapUserDetailsImpl) {
                LdapUserDetails ldapUser = (LdapUserDetailsImpl) principal;
                userId = ldapUser.getUsername();
            }
        } else {
            userId = principal.toString();
        }
        
        Ticket ticket = ticketRepo.findById(UUID.fromString(id)).orElse(null);
        if(ticket == null) {
            model.addAttribute("error", "Tidak ada tiket");
            return "ticket/index";
        }
        ticket.setStatus(newStatus.toUpperCase());
        ticketRepo.save(ticket);

        loggerService.saveAsyncActivityLog(
            new ActivityLog().deepLink("core://marves.dev/openticket")
            .uri("/openticket")
            .referenceId(ticket.getId().toString())
            .actionTime(ZonedDateTime.now())
            .actionName(newStatus.toLowerCase() + ".ticket")
            .description(ticket.getStatusDisplay())
            .user(new User().id(UUID.fromString(userId)))
        );

        if(newStatus.equalsIgnoreCase("start") ||
            newStatus.equalsIgnoreCase("finish")) {
            Notification notification = new Notification()
                .level("PRIVATE")
                .assignee("SPECIFIC")
                .category("TASK")
                .deepLink("core://marves.dev/openticket")
                .referenceId(ticket.getId().toString())
                .uri("/openticket");
            notification.setUser(ticket.getUser());
            notification.setDateTime(ZonedDateTime.now());
            if(newStatus.equalsIgnoreCase("start")) {
                notification.setContent("Isu/error yang anda kirim sudah kami terima. Kami akan segera memperbaikinya.");
            } else {
                notification.setContent("Perbaikan sudah selesai. Jika ada kendala, sampaikan di kolom feedback.");
            }
            notificationRepo.save(notification);

            NotificationStatus notifStatus = new NotificationStatus(notification, ticket.getUser(), false, false);

            if(ticket.getUser().getOneSignalId() != null && 
                !ticket.getUser().getOneSignalId().isEmpty()) {
                Map<String, Object> body = new HashMap<>(Map.ofEntries(
                    entry("headings", Map.of("en", "Open Ticket")),
                    entry("contents", Map.of("en", notification.getContent())),
                    entry("data", Map.of("deepLink", notification.getDeepLink(), 
                        "view", "detail", 
                        "refId", notification.getReferenceId())),
                    entry("include_player_ids", Arrays.asList(ticket.getUser().getOneSignalId())),
                    entry("small_icon", "ic_stat_marves"),
                    entry("android_channel_id", "066ee9a7-090b-4a42-b084-0dcbbeb7f158"),
                    entry("android_accent_color", "FF19A472"),
                    entry("android_group", "openticket")
                ));
                messageService.sendPushNotification(notification, Arrays.asList(notifStatus), body);
            } else {
                notifStatusRepo.save(notifStatus);
            }
        }
        
        model.addAttribute("data", ticket);
        model.addAttribute("action", newStatus);
        model.addAttribute("activities", activityRepo.findAllByRefIdOrderByActionTimeDesc(ticket.getId().toString()));
        model.addAttribute("comments", commentRepo.findAllByTicketOrderByCreatedAtDesc(ticket));
        
        return "ticket/action";
    }

    @PostMapping("/comment")
    public String comment(Model model,
        @RequestParam(name = "id") String id, 
        @RequestParam(name = "comment") String content) {
        
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = null;
        if(principal != null) {
            if(principal instanceof LdapUserDetailsImpl) {
                LdapUserDetails ldapUser = (LdapUserDetailsImpl) principal;
                userId = ldapUser.getUsername();
            }
        } else {
            userId = principal.toString();
        }
        User user = userRepository.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));
        
        Ticket ticket = ticketRepo.findById(UUID.fromString(id)).orElse(null);
        if(ticket == null) {
            model.addAttribute("error", "Tidak ada tiket");
            return "ticket/index";
        }
        TicketComment comment = new TicketComment()
            .ticket(ticket)
            .content(content);
        comment.setUser(user);
        comment.dateTime(ZonedDateTime.now());
        commentRepo.save(comment);

        // loggerService.saveAsyncActivityLog(
        //     new ActivityLog().deepLink("core://marves.dev/openticket")
        //     .uri("/openticket")
        //     .referenceId(ticket.getId().toString())
        //     .actionTime(ZonedDateTime.now())
        //     .actionName(newStatus.toLowerCase() + ".ticket")
        //     .description(ticket.getStatusDisplay())
        //     .user(new User().id(UUID.fromString(userId)))
        // );
        
        model.addAttribute("data", ticket);
        model.addAttribute("action", "comment");
        model.addAttribute("activities", activityRepo.findAllByRefIdOrderByActionTimeDesc(ticket.getId().toString()));
        model.addAttribute("comments", commentRepo.findAllByTicketOrderByCreatedAtDesc(ticket));
        
        return "ticket/action";
    }

    @GetMapping("/open-ticket")
    public String action(Model model){
        Collection<Ticket> ticketList = ticketService.findAllOrderByDateTimeDesc();

    
        Ticket ticket = ticketList.iterator().next();
        model.addAttribute("ticketNo", ticket.getCode());
        if(null!=ticket.getUser().getProfile()){
            model.addAttribute("userName", ticket.getUser().getProfile().getFullName());
        }else{
            model.addAttribute("userName", ticket.getUser().getUsername());
        }
        model.addAttribute("ticketContent", ticket.getContent());
        model.addAttribute("ticketStatus", ticket.getStatus());

        model.addAttribute("tickets", ticketList);

        return "ticket/list2";
    }

    @PostMapping(value="/ticket-filter")
    public String findUserByName(Model model, @RequestParam("ticketcode") Optional<String> ticketCode) {
        String strTticketCode = ticketCode.orElse("");
        Optional<Ticket> optTicket = ticketService.findTicketByCode(strTticketCode);    
        if(optTicket.isPresent()){
            Ticket ticket = optTicket.orElse(new Ticket());
            model.addAttribute("ticketNo", ticket.getCode());
            if(null!=ticket.getUser().getProfile()){
                model.addAttribute("userName", ticket.getUser().getProfile().getFullName());
            }else{
                model.addAttribute("userName", ticket.getUser().getUsername());
            }
            model.addAttribute("ticketContent", ticket.getContent());
            model.addAttribute("ticketStatus", ticket.getStatus());
        }
        
        return "ticket/list2";
    }
    
    @PostMapping(value = "/update-status")
    public String updateTicketStatus(Model model, @RequestParam("ticketcode") Optional<String> ticketCode, @RequestParam("status") Optional<String> status) {
        Map<String, String> statusMap = new HashMap<>();
        String strTticketCode = ticketCode.orElse("");
        logger.debug("strTticketCode: " + strTticketCode);
        if(ticketCode.isPresent() && status.isPresent()){
            try {
                ticketService.updateTicketStatus(ticketCode.get(), status.get());
            } catch (Exception e) {
                statusMap.put("status", "error");
                e.printStackTrace();
            }
        }

        Optional<Ticket> optTicket = ticketService.findTicketByCode(strTticketCode);    
        if(optTicket.isPresent()){
            Ticket ticket = optTicket.orElse(new Ticket());
            model.addAttribute("ticketNo", ticket.getCode());
            if(null!=ticket.getUser().getProfile()){
                model.addAttribute("userName", ticket.getUser().getProfile().getFullName());
            }else{
                model.addAttribute("userName", ticket.getUser().getUsername());
            }
            model.addAttribute("ticketContent", ticket.getContent());
            model.addAttribute("ticketStatus", ticket.getStatus());
        }
        return "ticket/list2";
    }

    @PostMapping(value="/search-ticket")
    public String searchTicketByCode(Model model, @RequestParam("ticketcode") Optional<String> ticketCode) {


        String strTicketCode = ticketCode.orElse("");

        Collection<Ticket> ticketList = ticketService.findTicketLikeCode(strTicketCode);

        Ticket ticket = ticketList.iterator().next();
        model.addAttribute("ticketNo", ticket.getCode());
        if(null!=ticket.getUser().getProfile()){
            model.addAttribute("userName", ticket.getUser().getProfile().getFullName());
        }else{
            model.addAttribute("userName", ticket.getUser().getUsername());
        }
        model.addAttribute("ticketContent", ticket.getContent());
        model.addAttribute("ticketStatus", ticket.getStatus());

        model.addAttribute("tickets", ticketList);

        return "ticket/list2";
    }
    
}
