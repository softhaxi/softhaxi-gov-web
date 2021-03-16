package com.softhaxi.marves.core.restful.account;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.logging.Session;
import com.softhaxi.marves.core.domain.request.UserRequest;
import com.softhaxi.marves.core.domain.response.ErrorResponse;
import com.softhaxi.marves.core.domain.response.SuccessResponse;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.logging.SessionRepository;
import com.softhaxi.marves.core.service.account.UserService;
import com.softhaxi.marves.core.service.employee.EmployeeVitaeService;

import org.apache.groovy.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserRestful {

    private static final Logger logger = LoggerFactory.getLogger(UserRestful.class);

    @Value("${marves.hr.url}")
    private String marvesHrUrl;

    @Autowired
    private EmployeeVitaeService employeeVitaeService;
    
    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private SessionRepository sessionRepo;

    @GetMapping()
    public ResponseEntity<?> index(@RequestParam(name = "type", defaultValue = "mobile") String type) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        List<User> users = new LinkedList<>(userRepo.findAllActiveMobileUser());
        Collection<Map<?, ?>> data = new LinkedList<>();
        users.forEach((item) -> {
            if(!item.equals(user)) {
            data.add(Maps.of("id", item.getId(), "email", item.getEmail(), 
                "fullName", item.getProfile().getFullName()));
            }
        });
 
        return new ResponseEntity<>(
                new SuccessResponse(
                    HttpStatus.OK.value(),
                    HttpStatus.OK.getReasonPhrase(),
                    data
                ),
                HttpStatus.OK   
            );
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        //logger.info("Calling....");
        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getPersonalInfo(user.getEmail().toLowerCase().trim());
        
        if(data != null) {
            return new ResponseEntity<>(
                new SuccessResponse(
                    HttpStatus.OK.value(),
                    HttpStatus.OK.getReasonPhrase(),
                    // "Success"
                    Map.of("id", user.getId(), "fullName", data.get("name"), "photoUrl", 
                        data.get("thumbnail"), "email", data.get("email"))
                ),
                HttpStatus.OK   
            );
            
        }

        data = (Map<?, ?>) userService.retrieveUserLdapDetail(user.getEmail().trim().toLowerCase());
        //logger.info("[ME] === " + data);
        if(data != null) {
            return new ResponseEntity<>(
                new SuccessResponse(
                    HttpStatus.OK.value(),
                    HttpStatus.OK.getReasonPhrase(),
                    Map.of("id", user.getId(), "fullName", data.get("fullName").toString(), "photoUrl", 
                        "", "email", data.get("email").toString())
                ),
                HttpStatus.OK   
            );
        }

        return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
    }

    @PostMapping("/update/notification")
    public ResponseEntity<?> updateNotification(@RequestBody UserRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        if(user == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }

        // logger.info("[updateNotification] OneSignal Id: " + request.getOneSignalId());
        if(user.getOneSignalId() == null || !user.getOneSignalId().equals(request.getOneSignalId())) {
            user.setOneSignalId(request.getOneSignalId().trim());
            userRepo.save(user);
        }
        // logger.info("[updateNotification] User Id: " + user.getId().toString() + " OneSignal Id: " + user.getOneSignalId());

        List<Session> sessions = (List<Session>) sessionRepo.findAllValidByUser(user);
        sessions.forEach((session) -> {
            session.setOneSignalId(request.getOneSignalId().trim());
        });
        sessionRepo.saveAll(sessions);

        return new ResponseEntity<>(
            new SuccessResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                "item.updated"
            ),
            HttpStatus.OK   
        );
    }
}