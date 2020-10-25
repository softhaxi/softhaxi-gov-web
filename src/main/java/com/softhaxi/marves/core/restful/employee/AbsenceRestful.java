package com.softhaxi.marves.core.restful.employee;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import com.softhaxi.marves.core.domain.attendence.Attendence;
import com.softhaxi.marves.core.domain.attendence.DailyAttendence;
import com.softhaxi.marves.core.model.request.AbsenceRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.service.employee.AbsenceService;
import com.softhaxi.marves.core.util.AbsenceUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Raja Sihombing
 * @since 1
 */
@RestController
@RequestMapping("/api/v1/absence")
public class AbsenceRestful {

    @Autowired
    private AbsenceService absenceService;

    @Autowired
    private AbsenceUtil absenceUtil;
    
    @GetMapping()
    public ResponseEntity<String> getLast(
        @RequestParam(value = "type", defaultValue = "daily") String type) {
        return new ResponseEntity<>("Last attendence", HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<?> post(@RequestBody() AbsenceRequest absence) {
        Attendence attendence = absenceService.getLastAbsence(absence.getType());
        if(attendence.getId() != null) {
            if(absence.getAction().equals("CI")) {
                if(absenceUtil.isSameDateAction(Date.from(attendence.getDateTime().toInstant()), absence.getDateTime()))
                    return new ResponseEntity<>(
                        new ErrorResponse(
                            HttpStatus.BAD_REQUEST.value(), 
                            HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                            "Can not submit Daily Clock In twice in same day"
                        ), HttpStatus.BAD_REQUEST);
            }
            if(attendence instanceof DailyAttendence && absence.getAction().equals("CO")) {
                DailyAttendence daily = (DailyAttendence) attendence;
                if(daily.getOutWork() != null) {
                    return new ResponseEntity<>(
                        new ErrorResponse(
                            HttpStatus.BAD_REQUEST.value(), 
                            HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                            "Can not submit Daily Clock Out before Clock In"
                        ), HttpStatus.BAD_REQUEST);
                }
                daily.setOutAction(absence.getAction().toUpperCase().trim());
                daily.setOutDateTime(ZonedDateTime.ofInstant(absence.getDateTime().toInstant(), ZoneId.systemDefault()));
                daily.setOutLatitude(absence.getLatitude());
                daily.setOutLongitude(absence.getLatitude());
                daily.setIsOutMockLocation(absence.getIsMockLocation());
                attendence = absenceService.save(daily);

                return new ResponseEntity<>(
                    new GeneralResponse(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        Map.of("id", attendence.getId())
                    ),
                    HttpStatus.OK
                );
            }
        } else {
            if(absence.getType().equals("DAILY") && absence.getAction().equals("CI")) {
                DailyAttendence daily = new DailyAttendence();
                daily.setAction(absence.getAction().toUpperCase().trim());
                daily.setDateTime(ZonedDateTime.ofInstant(absence.getDateTime().toInstant(), ZoneId.systemDefault()));
                daily.setLatitude(absence.getLatitude());
                daily.setLongitude(absence.getLatitude());
                daily.setIsMockLocation(absence.getIsMockLocation());
                attendence = absenceService.save(daily);

                return new ResponseEntity<>(
                    new GeneralResponse(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        Map.of("id", attendence.getId())
                    ),
                    HttpStatus.CREATED
                );
            }
            if(attendence instanceof DailyAttendence && absence.getAction().equals("CO")) {
                return new ResponseEntity<>(
                        new ErrorResponse(
                            HttpStatus.BAD_REQUEST.value(), 
                            HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                            "Can not submit Daily Clock Out before Clock In"
                        ), HttpStatus.BAD_REQUEST);
            }
        }

        return new ResponseEntity<>("Success", HttpStatus.CREATED);
    }
}
