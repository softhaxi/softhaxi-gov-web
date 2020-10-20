package com.softhaxi.marves.core.restful;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Raja Sihombing
 * @since 1
 */
@RestController
@RequestMapping("/api/v1/activity")
public class ActivityRestful {
    private static final Logger logger = LoggerFactory.getLogger(ActivityRestful.class);

    @GetMapping()
    public ResponseEntity<String> index(
        @RequestParam(value = "range", defaultValue = "today") String range) {
        logger.debug("List of activity for " + range);
        return new ResponseEntity<>("List of today activity", HttpStatus.OK);
    }
}
