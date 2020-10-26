package com.softhaxi.marves.core.restful.employee;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.logging.ActivityLog;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.logging.ActivityLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Raja Sihombing
 * @since 1
 */
@RestController
@RequestMapping("/api/v1/activity")
public class ActivityRestful {
    private static final Logger logger = LoggerFactory.getLogger(ActivityRestful.class);

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @GetMapping()
    public ResponseEntity<?> index(
        @RequestParam(value = "range", defaultValue = "today") String range) {
        List<ActivityLog> activities = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        if(range.equals("today")) {
            logger.debug("[ActivityRestful][index] List of today activity...");
            activities = activityLogRepository.findByUserAndActionDateOrderByActionTimeDesc(user, LocalDate.now());
        }

        return new ResponseEntity<>(
            new GeneralResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), activities), 
            HttpStatus.OK);
    }
}
