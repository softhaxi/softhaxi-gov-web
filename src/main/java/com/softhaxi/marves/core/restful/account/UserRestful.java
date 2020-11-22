package com.softhaxi.marves.core.restful.account;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.service.employee.MarvesHRService;

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
    private MarvesHRService marvesHrService;
    
    @Autowired
    private UserRepository userRepo;

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) marvesHrService.getPersonalInfo(user.getEmail().toLowerCase().trim());
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }
        //logger.info("[me] Data..." + data.toString());
        Map<?, ?> result = (Map<?, ?>) ((List<?>) data.get("result")).get(0);
        //logger.info("[me] Result..." + result.toString());

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                // "Success"
                Map.of("fullName", result.get("NAMA_ONLY"), "photoUrl", 
                    String.format("%s%s", marvesHrUrl, result.get("FOTO_THUMB")))
            ),
            HttpStatus.OK   
        );
    }
}