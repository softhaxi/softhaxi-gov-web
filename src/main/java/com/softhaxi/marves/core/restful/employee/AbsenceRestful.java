package com.softhaxi.marves.core.restful.employee;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.attendance.DailyAttendance;
import com.softhaxi.marves.core.domain.exception.BusinessException;
import com.softhaxi.marves.core.domain.request.AbsenceRequest;
import com.softhaxi.marves.core.domain.response.ErrorResponse;
import com.softhaxi.marves.core.domain.response.SuccessResponse;
import com.softhaxi.marves.core.service.employee.AbsenceService;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Raja Sihombing
 * @since 1
 */
@RestController
@RequestMapping("/api/v1/absence")
public class AbsenceRestful {

    private final static Logger logger = LoggerFactory.getLogger(AbsenceRestful.class);

    @Autowired
    private AbsenceService dailyService;

    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestParam(required = false, defaultValue = "daily") String type,
            @RequestParam(required = false) String year, @RequestParam(required = false) String month) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        List<DailyAttendance> data = (List<DailyAttendance>) dailyService.getHistoryByUser(user, year, month, false);
        Collections.sort(data, new Comparator<DailyAttendance>() {
            @Override
            public int compare(DailyAttendance o1, DailyAttendance o2) {
                return o2.getDateTime().compareTo(o1.getDateTime());
            }
        });

        return new ResponseEntity<>(new SuccessResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data),
                HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        DailyAttendance daily = null;
        try {
            daily = dailyService.getByUserAndId(user, UUID.fromString(id));
        } catch (BusinessException e) {
            return new ResponseEntity<>(new ErrorResponse(HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(), e.getMessage()), HttpStatus.NOT_FOUND);
        } catch(Exception e) {
            return new ResponseEntity<>(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(new SuccessResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(),
            daily),
                HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<?> post(@RequestParam(required = true) String payload,
            @RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest servlet) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        AbsenceRequest request = null;
        try {
            logger.debug("[post] payload from mobile...." + payload);
            request = new ObjectMapper().readValue(payload, AbsenceRequest.class);
        } catch (JsonProcessingException ex) {
            logger.error("[post] Exception..." + ex.getMessage(), ex);
            return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), Map.of("payload", "json.required")),
                    HttpStatus.BAD_REQUEST);
        }
        request.setIpAddress(servlet.getHeader("X-Forwarded-For") != null ? servlet.getHeader("X-Forwarded-For")
                : servlet.getRemoteAddr());
        request.setPhoto(file);

        try {
            var daily = dailyService.save(user, request);

            return new ResponseEntity<>(new SuccessResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), daily),
                HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/daily")
    public ResponseEntity<?> daily(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        if (date == null) {
            date = LocalDate.now();
        }

        var daily = dailyService.getDailyByUserAndDate(user, date);

        return new ResponseEntity<>(new SuccessResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), daily),
                HttpStatus.OK);
    }

    @GetMapping("/notes")
    public ResponseEntity<?> info() {
        var data = dailyService.getDailyNotes();

        return new ResponseEntity<>(new SuccessResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data),
                HttpStatus.OK);
    }


    @GetMapping("/history/year")
    public ResponseEntity<?> year(@RequestParam(required = false, defaultValue = "daily") String type) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        var data = dailyService.getHistoryYearListByUser(user);

        return new ResponseEntity<>(new SuccessResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data),
                HttpStatus.OK);
    }
}
