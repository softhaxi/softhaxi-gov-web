package com.softhaxi.marves.core.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Raja Sihombing
 * @since 1
 */
@RestController
@RequestMapping("/api/v1/attendence")
public class AttendenceRestful {
    
    @GetMapping()
    public ResponseEntity<String> getLast(
        @RequestParam(value = "type", defaultValue = "daily") String type) {
        return new ResponseEntity<>("Last attendence", HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<String> post(
        @RequestParam(value = "type", defaultValue = "daily") String type,
        @RequestParam(value = "action", defaultValue = "in") String action) {
        return new ResponseEntity<>("Success", HttpStatus.CREATED);
    }
}
