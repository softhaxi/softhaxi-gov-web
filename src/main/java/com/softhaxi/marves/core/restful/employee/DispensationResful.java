package com.softhaxi.marves.core.restful.employee;

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
import com.softhaxi.marves.core.domain.attendance.Dispensation;
import com.softhaxi.marves.core.domain.logging.ActivityLog;
import com.softhaxi.marves.core.model.request.DispensationRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.attendance.DispensationRepository;
import com.softhaxi.marves.core.service.logging.LoggerService;
import com.softhaxi.marves.core.service.storage.FileStorageService;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/dispensation")
public class DispensationResful {

    private static final Logger logger = LoggerFactory.getLogger(DispensationResful.class);

    @Autowired
    private DispensationRepository dispensationRepo;

    @Autowired
    private FileStorageService storageService;

    @Autowired
    private LoggerService loggerService;

    @GetMapping()
    public ResponseEntity<?> index(@RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        Collection<Dispensation> dispensations;
        if (date == null) {
            dispensations = dispensationRepo.findAllByUserOrderByStartDateDesc(user);
        } else {
            dispensations = dispensationRepo.findByUserAndDate(user, date);
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                dispensations
            ),
            HttpStatus.OK   
        );
    }
    
    @PostMapping()
    public ResponseEntity<?> post(@RequestParam(required = true) String payload,
        @RequestParam(value = "file", required = false) MultipartFile file,
        HttpServletRequest servlet) {
        DispensationRequest request;
        try {
            request = new ObjectMapper().readValue(payload, DispensationRequest.class);
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        String ipAddress = servlet.getHeader("X-Forwarded-For") != null ? servlet.getHeader("X-Forwarded-For") : servlet.getRemoteAddr();

        String path = null;
        if (file != null) {
            try {
                String folder = String.format("/%s/%s", "dispensation", new SimpleDateFormat("yyyyMMdd").format(new Date()));
                path = storageService.store(folder, null, file);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        LocalDate startDate = request.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = request.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        Dispensation dispensation = new Dispensation()
            .type(request.getType().toUpperCase())
            .desciption(request.getDescription().trim())
            .startDate(startDate)
            .endDate(endDate)
            .ipAddress(ipAddress)
            .user(user);
        if(path != null) {
            dispensation.setFileName(file.getOriginalFilename());
            dispensation.setAttachment(path);
        }
        dispensationRepo.save(dispensation);

        ActivityLog activityLog = new ActivityLog()
            .user(user)
            .actionTime(ZonedDateTime.now())
            .actionName("dispensation.submit")
            .description(dispensation.getType())
            .ipAddress(ipAddress)
            .uri("/dispensation")
            .deepLink("core://marves.dev/dispensasion")
            .referenceId(dispensation.getId().toString());
        loggerService.saveAsyncActivityLog(activityLog);

        
        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.CREATED.value(),
                HttpStatus.CREATED.getReasonPhrase(),
                dispensation
            ),
            HttpStatus.CREATED   
        );
    }

    @GetMapping("/history")
    public ResponseEntity<?> history() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        LocalDate date = LocalDate.now().plusDays(1);
        Collection<?> dispensations = dispensationRepo.findAllByUserAndBeforeDate(user, date);
        logger.info("[History] Dispensation number..." + dispensations.size());

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                dispensations
                ),
            HttpStatus.OK
        );
    }
}
