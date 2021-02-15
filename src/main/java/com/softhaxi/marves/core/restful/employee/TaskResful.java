package com.softhaxi.marves.core.restful.employee;

import java.time.LocalDate;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.employee.ScheduleRepository;

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
@RequestMapping("/api/v1/schedule")
public class TaskResful {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @GetMapping()
    public ResponseEntity<?> list(
        @RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        if(date == null) 
            date = LocalDate.now();

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(), 
                HttpStatus.OK.getReasonPhrase(), 
                scheduleRepository.findAllByUserAndDate(user, date)
            ), 
            HttpStatus.OK);
    }
}
