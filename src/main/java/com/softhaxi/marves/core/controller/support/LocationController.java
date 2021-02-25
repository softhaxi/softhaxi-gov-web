package com.softhaxi.marves.core.controller.support;

import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.logging.LocationLog;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.model.support.Location;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.logging.LocationLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/location")
public class LocationController {
    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    @Autowired
    private LocationLogRepository locationRepo;

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/latest")
    public ResponseEntity<?> latest(@RequestParam(name = "user", required = false) String userId) {
        Collection<LocationLog> locations = null;
        if(userId != null) {
            User user = userRepo.findById(UUID.fromString(userId)).orElse(null);
            if(user == null) {
                return new ResponseEntity<>(new ErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(),
                    "item.not.found"
                ), HttpStatus.NOT_FOUND);
            }

            locations = locationRepo.findAllLatestUpdatedByUser(user);            
        } else
            locations = locationRepo.findAllLatestUpdated();

        logger.debug("[latest] Number latest update...." + locations.size());

        Collection<Location> data = new LinkedList<>();
        for (LocationLog locationLog : locations) {
            data.add(new Location()
                .id(locationLog.getId())
                .userId(locationLog.getUser().getId())
                .email(locationLog.getUser().getEmail())
                .fullName(locationLog.getUser().getProfile() != null ? locationLog.getUser().getProfile().getFullName() : null)
                .profilePicture(locationLog.getUser().getEmployee() != null ? locationLog.getUser().getEmployee().getPictureUrl() : null)
                .dateTime(locationLog.getDateTime())
                .latitude(locationLog.getLatitude())
                .longitude(locationLog.getLongitude()));
        }

        return new ResponseEntity<>(new GeneralResponse(
            HttpStatus.OK.value(),
            HttpStatus.OK.getReasonPhrase(),
            data
        ), HttpStatus.OK);
    }
}
