package com.softhaxi.marves.core.restful.employee;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.attendence.Attendence;
import com.softhaxi.marves.core.domain.attendence.DailyAttendence;
import com.softhaxi.marves.core.domain.attendence.MeetingAttendence;
import com.softhaxi.marves.core.model.request.AbsenceRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.service.employee.AbsenceService;
import com.softhaxi.marves.core.util.AbsenceUtil;

import org.springframework.beans.factory.annotation.Autowired;
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
    
    // @GetMapping()
    // public ResponseEntity<String> getLast(
    //     @RequestParam(value = "type", defaultValue = "daily") String type) {
    //     return new ResponseEntity<>("Last attendence", HttpStatus.OK);
    // }

    @PostMapping()
    public ResponseEntity<?> post(@RequestBody() AbsenceRequest absence) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        Attendence attendence = absenceService.getLastAbsence(user, absence.getType(), absence.getCode());
        if(attendence.getId() != null) {
            if(attendence instanceof DailyAttendence) {
                if(absence.getAction().equals("CI")) {
                    if(absenceUtil.isSameDateAction(attendence.getDateTime(), ZonedDateTime.ofInstant(absence.getDateTime().toInstant(), ZoneId.systemDefault())))
                        return new ResponseEntity<>(
                            new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(), 
                                HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                                "Can not submit Daily Clock In twice in same day"
                            ), HttpStatus.BAD_REQUEST);
                    
                    if(absence.getAction().equals("CO")) {
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
                        daily.setOutLongitude(absence.getLongitude());
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
                }
            } else if(attendence instanceof MeetingAttendence) {
                return new ResponseEntity<>(
                    new GeneralResponse(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        Map.of("id", attendence.getId(), "code", ((MeetingAttendence)attendence).getCode())
                    ),
                    HttpStatus.OK
                );
            } 
        } else {
            if(absence.getAction().equals("CI")) {
                if(absence.getType().equalsIgnoreCase("DAILY")) {
                    DailyAttendence daily = new DailyAttendence();
                    daily.setUser(user);
                    daily.setAction(absence.getAction().toUpperCase().trim());
                    daily.setDateTime(ZonedDateTime.ofInstant(absence.getDateTime().toInstant(), ZoneId.systemDefault()));
                    daily.setLatitude(absence.getLatitude());
                    daily.setLongitude(absence.getLongitude());
                    daily.setIsMockLocation(absence.getIsMockLocation());
                    attendence = absenceService.save(daily);
                } else if(absence.getType().equalsIgnoreCase("MEETING")) {
                    MeetingAttendence meeting = new MeetingAttendence();
                    meeting.setUser(user);
                    meeting.setAction(absence.getAction().toUpperCase().trim());
                    meeting.setDateTime(ZonedDateTime.ofInstant(absence.getDateTime().toInstant(), ZoneId.systemDefault()));
                    meeting.setLatitude(absence.getLatitude());
                    meeting.setLongitude(absence.getLongitude());
                    meeting.setIsMockLocation(absence.getIsMockLocation());
                    meeting.setCode(absence.getCode().trim());
                    attendence = absenceService.save(meeting);
                }
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

    @GetMapping("/meeting")
    public ResponseEntity<?> meetingAttendence(@RequestParam(value="code", required = true) String code) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        Attendence attendence = absenceService.getLastAbsence(user, "MEETING", code);

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                attendence
            ),
            HttpStatus.OK
        );
    }
}
