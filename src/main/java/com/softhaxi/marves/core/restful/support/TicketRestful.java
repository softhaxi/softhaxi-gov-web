package com.softhaxi.marves.core.restful.support;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softhaxi.marves.core.domain.access.Role;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.logging.ActivityLog;
import com.softhaxi.marves.core.domain.support.Ticket;
import com.softhaxi.marves.core.domain.support.TicketComment;
import com.softhaxi.marves.core.model.request.CommentRequest;
import com.softhaxi.marves.core.model.request.TicketRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.access.RoleRepository;
import com.softhaxi.marves.core.repository.logging.ActivityLogRepository;
import com.softhaxi.marves.core.repository.support.TicketCommentRepository;
import com.softhaxi.marves.core.repository.support.TicketRepository;
import com.softhaxi.marves.core.service.logging.LoggerService;
import com.softhaxi.marves.core.service.storage.FileStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Raja Sihombing
 * @since 1
 */
@RestController()
@RequestMapping("/api/v1/ticket")
public class TicketRestful {

    private final static Logger logger = LoggerFactory.getLogger(TicketRestful.class);

    @Autowired
    private TicketRepository ticketRepo;

    @Autowired
    private TicketCommentRepository commentRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private FileStorageService storageService;

    @Autowired
    private LoggerService loggerService;

    @Autowired
    private ActivityLogRepository activityRepo;

    @GetMapping()
    public ResponseEntity<?> index(@RequestParam(value = "status", required = false) String status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        if (status != null) {
            return new ResponseEntity<>(
                    new GeneralResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(),
                            ticketRepo.findAllByUserAndStatusOrderByCreatedAt(user, status.toLowerCase())),
                    HttpStatus.OK);
        }
        
        List<Ticket> tickets = (List<Ticket>) ticketRepo.findAllNonClosedByUser(user);
        List<Ticket> closedTickets = (List<Ticket>) ticketRepo.findAllClosedByUser(user);
        if(tickets.isEmpty()) tickets = closedTickets;
        else tickets.addAll(closedTickets);

        return new ResponseEntity<>(new GeneralResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(),
                tickets), HttpStatus.OK);

    }

    // consumes = {MediaType.APPLICATION_JSON_VALUE,
    // MediaType.MULTIPART_FORM_DATA_VALUE}
    @PostMapping()
    public ResponseEntity<?> post(@RequestParam(required = true) String payload,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        TicketRequest request = null;
        try {
            request = new ObjectMapper().readValue(payload, TicketRequest.class);
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

        if(request.getContent() == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                    Map.of("content", "field.required")
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        Role admin = roleRepo.findByName("ADMIN").orElse(null);
        
        Ticket ticket = new Ticket()
            .user(user)
            .code(String.format("OT%08d", ticketRepo.count() + 1))
            .content(request.getContent().trim())
            .status("open")
            .pic(admin != null ? admin.getId().toString() : null);
        
        String path = null;
        //String rename =  ticket.getCode() + "_" + new SimpleDateFormat("HHmmss").format(new Date());
        if (file != null) {
            try {
                path = storageService.store("/ticket/" + new SimpleDateFormat("yyyyMMdd").format(new Date()), ticket.getCode(), file);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        if(path != null) {
            path = path.replace('\\', '/');
            ticket.setFilename(path.substring(path.lastIndexOf("/") + 1));
            ticket.setStoragePath(path);
        } 

        ticketRepo.save(ticket);

        loggerService.saveAsyncActivityLog(
            new ActivityLog().user(user)
                .actionTime(ZonedDateTime.now())
                .actionName("open.ticket")
                .description(ticket.getCode())
                .uri("/ticket")
                .deepLink("core://marves.dev/ticket")
                .referenceId(ticket.getId().toString())
        );

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.CREATED.value(), 
                HttpStatus.CREATED.getReasonPhrase(),
                ticket), 
            HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        Ticket ticket = ticketRepo.findOneByUserAndId(user, UUID.fromString(id)).orElse(null);
        if(ticket == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }

        var comments = commentRepo.findAllByTicketOrderByCreatedAtDesc(ticket);
        var activities = activityRepo.findAllByRefIdOrderByActionTimeDesc(ticket.getId().toString());
        ticket.setComments(comments);
        ticket.setActivities(activities);

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                ticket
             ),
            HttpStatus.OK
        );
    }


    @GetMapping("/comment")
    public ResponseEntity<?> comments(@RequestParam(name = "id") String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        
        Ticket ticket = ticketRepo.findOneByUserAndId(user, UUID.fromString(id)).orElse(null);
        if(ticket == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }
        var comments = commentRepo.findAllByTicketOrderByCreatedAtDesc(ticket);

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                comments
             ),
            HttpStatus.OK
        );
    }

    @PostMapping("/status")
    public ResponseEntity<?> status(@RequestBody TicketRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        
        Ticket ticket = ticketRepo.findById(UUID.fromString(request.getId())).orElse(null);

        if(ticket == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(), 
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "item.not.found"), 
                HttpStatus.BAD_REQUEST);
        }

        if(!ticket.getUser().equals(user)) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(), 
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "item.not.found"), 
                HttpStatus.BAD_REQUEST);
        }

        ticket.setStatus(request.getStatus().toUpperCase());
        ticketRepo.save(ticket);

        loggerService.saveAsyncActivityLog(
            new ActivityLog().deepLink("core://marves.dev/openticket")
            .uri("/openticket")
            .referenceId(ticket.getId().toString())
            .actionTime(ZonedDateTime.now())
            .actionName(request.getStatus().toLowerCase() + ".ticket")
            .description(ticket.getStatusDisplay())
            .user(user)
        );

        var comments = commentRepo.findAllByTicketOrderByCreatedAtDesc(ticket);
        var activities = activityRepo.findAllByRefIdOrderByActionTimeDesc(ticket.getId().toString());
        ticket.setComments(comments);
        ticket.setActivities(activities);
        
        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(), 
                HttpStatus.OK.getReasonPhrase(),
                ticket), 
            HttpStatus.OK);
    }

    @PostMapping("/comment")
    public ResponseEntity<?> comment(@RequestBody(required = true) CommentRequest request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        
        Ticket ticket = ticketRepo.findById(UUID.fromString(request.getReference())).orElse(null);

        if(ticket == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(), 
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "item.not.found"), 
                HttpStatus.BAD_REQUEST);
        }

        if(!ticket.getUser().equals(user)) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(), 
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "item.not.found"), 
                HttpStatus.BAD_REQUEST);
        }

        TicketComment comment = new TicketComment()
            .user(user)
            .ticket(ticket)
            .content(request.getContent().trim());
        commentRepo.save(comment);

        loggerService.saveAsyncActivityLog(
            new ActivityLog()
                .user(user)
                .actionTime(ZonedDateTime.now())
                .actionName("comment.ticket")
                .description(ticket.getCode())
                .uri("/ticket")
                .deepLink("core://marves.dev/ticket")
                .referenceId(ticket.getId().toString())
        );  

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(), 
                HttpStatus.OK.getReasonPhrase(),
                ticket), 
            HttpStatus.OK);
    }
}
