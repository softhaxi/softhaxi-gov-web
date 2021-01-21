package com.softhaxi.marves.core.restful.employement;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.attendance.Attendance;
import com.softhaxi.marves.core.domain.attendance.DailyAttendance;
import com.softhaxi.marves.core.domain.attendance.MeetingAttendance;
import com.softhaxi.marves.core.model.request.AbsenceRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.service.employee.AbsenceService;
import com.softhaxi.marves.core.service.storage.FileStorageService;
import com.softhaxi.marves.core.util.AbsenceUtil;

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
    private AbsenceService absenceService;

    @Autowired
    private FileStorageService storageService;

    @Autowired
    private AbsenceUtil absenceUtil;

    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestParam(required = false, defaultValue = "daily") String type,
        @RequestParam(required = false) String year, 
        @RequestParam(required = false) String month) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        LocalDate from = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        if(year != null && month != null)
            from = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1);

        LocalDate to = from.with(TemporalAdjusters.lastDayOfMonth());
        logger.info("[History] From date..." + from.toString() + " To date..." + to.toString());

        Collection<?> data = absenceService.getHistoryByUser(user, type.trim().toLowerCase(), from.atStartOfDay(ZoneId.systemDefault()), 
            to.atStartOfDay(ZoneId.systemDefault()));

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
                ),
            HttpStatus.OK
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        Attendance attendence = absenceService.getByUserAndId(user, UUID.fromString(id));
        if(attendence == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                attendence instanceof DailyAttendance ? (DailyAttendance) attendence : (MeetingAttendance) attendence
             ),
            HttpStatus.OK
        );
    }
    
    @PostMapping()
    public ResponseEntity<?> post(@RequestParam(required = true) String payload,
        @RequestParam(value = "file", required = false) MultipartFile file,
        HttpServletRequest servlet) {
        AbsenceRequest request = null;
        try {
            request = new ObjectMapper().readValue(payload, AbsenceRequest.class);
        } catch (JsonProcessingException ex) {
            logger.error("[post] Exception..." + ex.getMessage(), ex);
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                    HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                    Map.of("payload", "json.required")
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        String ipAddress = servlet.getHeader("X-Forwarded-For") != null ? servlet.getHeader("X-Forwarded-For") : servlet.getRemoteAddr();
        //logger.info("[post] IP Adress..." + ipAddress);

        String path = null;
        if (file != null) {
            try {
                String folder = String.format("/%s/%s", request.getType().toLowerCase(), new SimpleDateFormat("yyyyMMdd").format(new Date()));
                path = storageService.store(folder, new SimpleDateFormat("HHmmss").format(new Date()), file);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        Attendance attendence = absenceService.getLastAbsence(user, request.getType(), request.getCode());
        if(attendence.getId() != null) {
            if(attendence instanceof DailyAttendance) {
                if(request.getAction().equals("CI")) {
                    if(absenceUtil.isSameDateAction(attendence.getDateTime(), ZonedDateTime.ofInstant(request.getDateTime().toInstant(), ZoneId.systemDefault())))
                        return new ResponseEntity<>(
                            new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(), 
                                HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                                "Can not submit Daily Clock In twice in same day"
                            ), HttpStatus.BAD_REQUEST);
                    else {
                        DailyAttendance daily = new DailyAttendance();
                        daily.setUser(user);
                        daily.setAction(request.getAction().toUpperCase().trim());
                        daily.setDateTime(ZonedDateTime.ofInstant(request.getDateTime().toInstant(), ZoneId.systemDefault()));
                        daily.setLatitude(request.getLatitude());
                        daily.setLongitude(request.getLongitude());
                        daily.setIsMockLocation(request.getIsMockLocation());
                        if(path != null) {
                            daily.setPicturePath(path);
                        }
                        daily.setIpAddress(ipAddress);
                        attendence = absenceService.save(daily);

                        return new ResponseEntity<>(
                            new GeneralResponse(
                                HttpStatus.CREATED.value(),
                                HttpStatus.CREATED.getReasonPhrase(),
                                attendence
                            ),
                            HttpStatus.CREATED
                        );
                    }
                }
                if(request.getAction().equals("CO")) {
                    DailyAttendance daily = (DailyAttendance) attendence;
                    if(daily.getOutWork() != null) {
                        return new ResponseEntity<>(
                            new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(), 
                                HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                                "Can not submit Daily Clock Out before Clock In"
                            ), HttpStatus.BAD_REQUEST);
                    }
                    daily.setOutAction(request.getAction().toUpperCase().trim());
                    daily.setOutDateTime(ZonedDateTime.ofInstant(request.getDateTime().toInstant(), ZoneId.systemDefault()));
                    daily.setOutLatitude(request.getLatitude());
                    daily.setOutLongitude(request.getLongitude());
                    daily.setIsOutMockLocation(request.getIsMockLocation());
                    if(path != null) {
                        daily.setPicturePath(path);
                    }
                    daily.setOutIpAddress(ipAddress);
                    attendence = absenceService.save(daily);
    
                    return new ResponseEntity<>(
                        new GeneralResponse(
                            HttpStatus.CREATED.value(),
                            HttpStatus.CREATED.getReasonPhrase(),
                            attendence
                        ),
                        HttpStatus.CREATED
                    );
                }
            } else if(attendence instanceof MeetingAttendance) {
                if(!attendence.getDateTime().toLocalDate()
                    .equals(LocalDate.ofInstant(request.getDateTime().toInstant(), 
                    ZoneId.systemDefault()))) {
                    MeetingAttendance meeting = new MeetingAttendance();
                    meeting.setUser(user);
                    meeting.setAction(request.getAction().toUpperCase().trim());
                    meeting.setDateTime(ZonedDateTime.ofInstant(request.getDateTime().toInstant(), ZoneId.systemDefault()));
                    meeting.setLatitude(request.getLatitude());
                    meeting.setLongitude(request.getLongitude());
                    meeting.setIsMockLocation(request.getIsMockLocation());
                    meeting.setCode(request.getCode().trim());
                    meeting.setReferenceId(request.getReferenceId());
                    meeting.setLocation(request.getLocation());
                    meeting.setOrganizer(request.getOrganizer());
                    meeting.setTitle(request.getTitle());
                    meeting.setDescription(request.getDescription());
                    meeting.setStartTime(request.getStartTime());
                    meeting.setEndTime(request.getEndTime());
                    if(path != null) {
                        meeting.setPicturePath(path);
                    }
                    meeting.setIpAddress(ipAddress);
                    attendence = absenceService.save(meeting);
                    return new ResponseEntity<>(
                        new GeneralResponse(
                            HttpStatus.CREATED.value(),
                            HttpStatus.CREATED.getReasonPhrase(),
                            attendence
                        ),
                        HttpStatus.CREATED
                    );
                }
                return new ResponseEntity<>(
                        new GeneralResponse(
                            HttpStatus.OK.value(),
                            HttpStatus.OK.getReasonPhrase(),
                            attendence
                        ),
                        HttpStatus.OK
                    );
            } 
        } else {
            if(request.getAction().equals("CI")) {
                if(request.getType().equalsIgnoreCase("DAILY")) {
                    DailyAttendance daily = new DailyAttendance();
                    daily.setUser(user);
                    daily.setAction(request.getAction().toUpperCase().trim());
                    daily.setDateTime(ZonedDateTime.ofInstant(request.getDateTime().toInstant(), ZoneId.systemDefault()));
                    daily.setLatitude(request.getLatitude());
                    daily.setLongitude(request.getLongitude());
                    daily.setIsMockLocation(request.getIsMockLocation());
                    if(path != null) {
                        daily.setPicturePath(path);
                    }
                    daily.setIpAddress(ipAddress);
                    attendence = absenceService.save(daily);
                } else if(request.getType().equalsIgnoreCase("MEETING")) {
                    MeetingAttendance meeting = new MeetingAttendance();
                    meeting.setUser(user);
                    meeting.setAction(request.getAction().toUpperCase().trim());
                    meeting.setDateTime(ZonedDateTime.ofInstant(request.getDateTime().toInstant(), ZoneId.systemDefault()));
                    meeting.setLatitude(request.getLatitude());
                    meeting.setLongitude(request.getLongitude());
                    meeting.setIsMockLocation(request.getIsMockLocation());
                    meeting.setCode(request.getCode().trim());
                    meeting.setReferenceId(request.getReferenceId());
                    meeting.setLocation(request.getLocation());
                    meeting.setOrganizer(request.getOrganizer());
                    meeting.setTitle(request.getTitle());
                    meeting.setDescription(request.getDescription());
                    meeting.setStartTime(request.getStartTime());
                    meeting.setEndTime(request.getEndTime());
                    if(path != null) {
                        meeting.setPicturePath(path);
                    }
                    meeting.setIpAddress(ipAddress);
                    attendence = absenceService.save(meeting);
                }
                return new ResponseEntity<>(
                    new GeneralResponse(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        attendence
                    ),
                    HttpStatus.CREATED
                );
            }
            
            if(attendence instanceof DailyAttendance && request.getAction().equals("CO")) {
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

    @GetMapping("/daily")
    public ResponseEntity<?> daily(
        @RequestParam(name = "date", required = false) 
        @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        Attendance attendence = absenceService.getLastAbsence(user, "DAILY", null);
        
        if(date == null) {
            date = LocalDate.now();
        }
        
        if(attendence != null) {
            if(attendence instanceof DailyAttendance && attendence.getDateTime() != null) {
                if(absenceUtil.isSameDateAction(attendence.getDateTime(), date.atStartOfDay(ZoneId.systemDefault()))) {
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
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                null
            ),
            HttpStatus.OK
        );
    }

    @GetMapping("/meeting")
    public ResponseEntity<?> meeting(
        @RequestParam(value="code", required = true) String code,
        @RequestParam(name = "date", required = false) 
        @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        Attendance attendence = absenceService.getLastAbsence(user, "MEETING", code);

        if(date == null) {
            date = LocalDate.now();
        }

        if(attendence != null) {
            if(attendence instanceof MeetingAttendance && attendence.getDateTime() != null) {
                if(absenceUtil.isSameDateAction(attendence.getDateTime(), date.atStartOfDay(ZoneId.systemDefault()))) {
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
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                null
            ),
            HttpStatus.OK
        );
    }
}
