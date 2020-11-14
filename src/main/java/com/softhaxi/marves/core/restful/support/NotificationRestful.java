package com.softhaxi.marves.core.restful.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.messaging.Message;
import com.softhaxi.marves.core.domain.messaging.MessageStatus;
import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.domain.messaging.NotificationStatus;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationStatusRepository;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Raja Sihombing
 * @since 1
 */
@RestController
@RequestMapping("/api/v1/notification")
public class NotificationRestful {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRestful.class);

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private NotificationStatusRepository notificationStatusRepo;
    
    @GetMapping()
    public ResponseEntity<?> index(
        @RequestParam(value="page", defaultValue = "1", required = false) int page,
        @RequestParam(value="q", required=false) String q) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        Collection<Notification> notifications = notificationRepo.findAllByUser(user);
        Collection<NotificationStatus> statuses = new ArrayList<>();
        for(Notification notification: notifications) {
            if(notification.getStatuses() != null && !notification.getStatuses().isEmpty()) {
                for(MessageStatus status: notification.getStatuses()) {
                    if(status.getUser().equals(user)) {
                        notification.setRead(status.isRead());
                        break;
                    }
                }
            } else {
                NotificationStatus status = notificationStatusRepo.findOneNotificationStatusByUser(notification, user).orElse(null);
                if(status == null) 
                    status = new NotificationStatus(
                        (Message) notification,
                        user, true, false
                    );
                else 
                    status.setDelivered(true);
                
                statuses.add(status);
            }
        }
        if(!statuses.isEmpty())
            notificationStatusRepo.saveAll(statuses);
        
        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                notifications
            ), 
            HttpStatus.OK
        );
    }

    // @PostMapping
    // public ResponseEntity<?> post() {
    //     Notification notification = new Notification();
    //     notification.setDateTime(ZonedDateTime.now());
    //     notification.setContent("Testing general notification saved to database");

    //     notificationRepo.save(notification);

    //     return new ResponseEntity<>(
    //         new GeneralResponse(
    //             HttpStatus.CREATED.value(),
    //             HttpStatus.CREATED.getReasonPhrase(),
    //             notification
    //         ),
    //         HttpStatus.CREATED
    //     );
    // }

    @GetMapping("/count")
    public ResponseEntity<Integer> count(
        @RequestParam(value="status", defaultValue = "unread") String status) {
        return new ResponseEntity<Integer>(0, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PostMapping("/{id}")
    public ResponseEntity<?> action(@PathVariable String id,
        @RequestParam(value="action", defaultValue="detail") String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        Notification notification = notificationRepo.findOneByIdAndUser(UUID.fromString(id), user).orElse(null);
        if(notification == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        } 
        NotificationStatus status = notificationStatusRepo.findOneNotificationStatusByUser(notification, user).orElse(null);
        if(status == null) {
            status = new NotificationStatus(
                (Message) notification,
                user, true, true
            );
        } else {
            status.setDelivered(true);
            status.setRead(true);
        }
        notificationStatusRepo.save(status);
        notification.setRead(true);

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                notification
            ), 
            HttpStatus.OK
        );
    }

    @GetMapping("/undelivered")
    public ResponseEntity<?> undeliveredList() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        Collection<Notification> notifications = notificationRepo.findAllUndeliveredByUser(user);

        logger.info("[undeliveredList] Count..." + notifications.size());

        Collection<NotificationStatus> statuses = new ArrayList<>();
        for(Notification notification: notifications) {
            NotificationStatus status = notificationStatusRepo.findOneUndeliveredNotificationByUser(notification, user).orElse(null);
            if(status == null) 
                status = new NotificationStatus(
                    (Message) notification,
                    user, true, false
                );
            else 
                status.setDelivered(true);
            
            statuses.add(status);
        }
        if(!statuses.isEmpty())
            notificationStatusRepo.saveAll(statuses);
        
        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                notifications
                // Map.of(
                //     "id", "1", 
                //     "content", "Testing notification from server", 
                //     "dateTime", LocalDateTime.now()
                // )
            ),
            HttpStatus.OK
        );
    }
}
