package com.softhaxi.marves.core.restful.employee;

import java.util.Map;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.messaging.Letter;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.model.response.RestfulResponse;
import com.softhaxi.marves.core.repository.messaging.LetterRepository;

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
@RequestMapping("/api/v1/letter")
public class LetterResful {
    
    @Autowired()
    private LetterRepository letterRepo;

    @GetMapping()
    public ResponseEntity<?> index(@RequestParam(value = "id", required = false) String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        
        RestfulResponse entity = null;
        ResponseEntity<?> response = new ResponseEntity<>(HttpStatus.OK);
        if(id == null) {
            entity = new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                letterRepo.findAllByUserOrderByReceivedAtDesc(user)
            );
            response = new ResponseEntity<>(entity, HttpStatus.OK);
        } else {
            try {
                UUID uuid = UUID.fromString(id);
                Letter letter = letterRepo.findById(uuid).orElse(null);
                if(letter != null && letter.getUser().equals(user)) {
                    entity = new GeneralResponse(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        letter
                    );
                    response = new ResponseEntity<>(entity, HttpStatus.OK);
                } else {
                    response = new ResponseEntity<>(
                        new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                            HttpStatus.NOT_FOUND.getReasonPhrase(), 
                            "Identifier not found"
                        ), 
                        HttpStatus.NOT_FOUND);
                }
            } catch (IllegalArgumentException iaex) {
                response = new ResponseEntity<>(
                    new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                        HttpStatus.BAD_REQUEST.getReasonPhrase(), 
                        "Invalid identifier"
                    ), 
                    HttpStatus.NOT_FOUND);
            }
            
        }
        return response;
    }

    @GetMapping("/count")
    public ResponseEntity<?> count(@RequestParam(value = "status", defaultValue = "unread") String status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
        
        RestfulResponse response = new GeneralResponse(
            HttpStatus.OK.value(),
            HttpStatus.OK.getReasonPhrase(),
            Map.of("status", status, "count", 0)
        );
        Object[] countObject = letterRepo.countUnreadByUser(user).orElse(new Object[] { 0 });
        if(countObject != null) {
            response = new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                Map.of("status", status, "count", Integer.parseInt(countObject[0].toString()))
            );
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
