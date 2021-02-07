package com.softhaxi.marves.core.controller.common;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.messaging.Message;
import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;

import org.apache.groovy.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

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

    @Autowired
    @Qualifier("oneSignalRestTemplate")
    private RestTemplate restTemplate;

    @Value("${onesignal.app.id}")
    private String appId;

    @Value("${onesignal.notification.endpoint}")
    private String notificationEndPoint;

    @GetMapping("/notification")
    public String getAllNotification(Model model){
        List<Notification> notifications = notificationRepository.findAll();
        logger.debug("notifications: "+notifications);
        model.addAttribute("notifications", notifications);
        return "common/notification-list.html";
    }

    @PostMapping("/save-notification")
    public String saveNotification(Model model, @ModelAttribute("notification") Notification notification){
        String subscriber = "";
        logger.debug("Assignee: " + notification.getAssignee());
        if(null==notification.getAssignee() || notification.getAssignee().equals("")){
            subscriber = "Subscribed Users";
        }else{
            Optional<User> user = userRepository.findUserByEmail(notification.getAssignee());
            if(user.isPresent()){
                notification.setUser(user.get());
                notification.setDateTime(ZonedDateTime.now(ZoneId.systemDefault()));
                notificationRepository.save(notification);    
            }
            else{
                model.addAttribute("errorMessage", "Email "+notification.getAssignee()+" is not attached to any user");
            }
        }
        
       
        HttpHeaders headers = new HttpHeaders();
        
        headers.add("Content-Type", "application/json; charset=utf-8");
        
        Map<?, ?> body = new HashMap<>();
        if(!subscriber.equals("")){
            body = Maps.of(
                "app_id", appId,
                "included_segments", Arrays.asList(subscriber),
                "headings", ImmutableMap.of("en", notification.getCategory()),
                "contents", ImmutableMap.of("en", notification.getContent()),
                "template_id", "8d98a080-04d2-4873-bf29-3c463ce6866a"
            );
        }else{
            body = Maps.of(
                "app_id", appId,
                "include_player_ids", Arrays.asList(notification.getUser().getOneSignalId()),
                "headings", ImmutableMap.of("en", notification.getCategory()),
                "contents", ImmutableMap.of("en", notification.getContent()),
                "template_id", "8d98a080-04d2-4873-bf29-3c463ce6866a"
            );
        }
        
        HttpEntity<Map<?, ?>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<?> response = restTemplate.postForEntity(notificationEndPoint, entity, Map.class);
    
        logger.info("[sendNotification] Result....{}", response.getBody());

        List<Notification> notifications = notificationRepository.findAll();
        
        model.addAttribute("notifications", notifications);
        return "common/notification-list.html";
    }

    @GetMapping("/find-user-email")
    public @ResponseBody String findUserByName(Model model, @RequestParam("email") Optional<String> email) {
        String strEmail = email.orElse("");
        
        List<User> users = userRepository.findUserByUsernameLike(strEmail);
        
        List<Map<String, String>> userList = new ArrayList<>();
        users = users.stream().limit(10).collect(Collectors.toList());
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