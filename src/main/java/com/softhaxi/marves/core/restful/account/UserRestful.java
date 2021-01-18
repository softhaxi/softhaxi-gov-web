package com.softhaxi.marves.core.restful.account;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.service.account.UserService;
import com.softhaxi.marves.core.service.employee.EmployeeVitaeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getPersonalInfo(user.getEmail().toLowerCase().trim());
        if(data != null) {
            return new ResponseEntity<>(
                new GeneralResponse(
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
        logger.info("[ME] === " + data);
        if(data != null) {
            return new ResponseEntity<>(
                new GeneralResponse(
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
}