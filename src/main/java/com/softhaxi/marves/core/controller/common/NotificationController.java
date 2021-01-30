package com.softhaxi.marves.core.controller.common;

import java.util.List;

import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Notification
 */
@Controller
public class NotificationController {

    Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/notification")
    public String getAllNotification(Model model){
        List<Notification> notifications = notificationRepository.findAll();
        for (Notification notification : notifications) {
            logger.debug("notification: " + notification.toString());
        }
        
        model.addAttribute("notifications", notifications);
        return "common/notification-list.html";
    }
    
}