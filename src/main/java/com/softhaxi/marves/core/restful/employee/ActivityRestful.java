package com.softhaxi.marves.core.restful.employee;

import java.time.LocalDate;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.response.SuccessResponse;
import com.softhaxi.marves.core.repository.logging.ActivityLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
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
    private ActivityLogRepository activityRepo;

    @GetMapping()
    public ResponseEntity<?> index(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        if (date == null)
            date = LocalDate.now();

        var data =  activityRepo.findAllUserActivity(user, date);
        
        //logger.debug("[index] Data...." + data);
        return new ResponseEntity<>(new SuccessResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(),
                data), HttpStatus.OK);
    }
}
