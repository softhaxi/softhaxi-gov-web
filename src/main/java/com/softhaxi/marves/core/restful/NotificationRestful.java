package com.softhaxi.marves.core.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Raja Sihombing
 * @since 1
 */
@RestController
@RequestMapping("/api/v1/notification")
public class NotificationRestful {
    
    @GetMapping()
    public ResponseEntity<String> index(
        @RequestParam(value="page", defaultValue="1") int page,
        @RequestParam(value="q", required=false) String q) {
        return new ResponseEntity<String>("List of notification page " + page, HttpStatus.OK);
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> count(
        @RequestParam(value="status", defaultValue = "unread") String status) {
        return new ResponseEntity<Integer>(0, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PostMapping("/{id}")
    public ResponseEntity<String> action(@PathVariable String id,
        @RequestParam(value="action", defaultValue="detail") String action) {
        return new ResponseEntity<String>("Detail of notification", HttpStatus.OK);
    }
}
