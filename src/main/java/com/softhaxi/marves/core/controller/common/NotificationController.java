package com.softhaxi.marves.core.controller.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.messaging.Message;
import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Notification
 */
@Controller
public class NotificationController {

    Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/notification")
    public String getAllNotification(Model model){
        List<Notification> notifications = notificationRepository.findAll();
        logger.debug("notifications: "+notifications);
        model.addAttribute("notifications", notifications);
        return "common/notification-list.html";
    }

    @PostMapping("/save-notification")
    public String saveNotification(Model model, @ModelAttribute("notification") Notification notification){
        
        Optional<User> user = userRepository.findUserByEmail(notification.getAssignee());
        if(user.isPresent()){
            notification.setUser(user.get());
            notificationRepository.save(notification);    
        }
        else{
            model.addAttribute("errorMessage", "Email "+notification.getAssignee()+" is not attached to any user");
        }
        List<Notification> notifications = notificationRepository.findAll();
        model.addAttribute("notifications", notifications);
        return "common/notification-list.html";
    }

    @GetMapping("/find-user-email")
    public @ResponseBody String findUserByName(Model model, @RequestParam("email") Optional<String> email) {
        String strEmail = email.orElse("");
        
        List<User> users = userRepository.findUserByUsernameLike(strEmail);
        List<Map<String, String>> userList = new ArrayList<>();
        
        String json = "";
        
        try {
            Map<String, String> userMap = new HashMap<>();
            for (User user : users) {
                userMap = new HashMap<>();
                userMap.put("email", user.getEmail());
                userList.add(userMap);
            }
            Gson gson = new Gson();
            json = gson.toJson(userList);
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;
    }
}