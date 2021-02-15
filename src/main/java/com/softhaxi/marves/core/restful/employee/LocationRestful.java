package com.softhaxi.marves.core.restful.employee;

import com.softhaxi.marves.core.model.request.LocationRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Raja Sihombing
 * @since 1
 */
@RestController
@RequestMapping("/api/v1/location")
public class LocationRestful {

    @PostMapping()
    public ResponseEntity<?> post(@RequestBody LocationRequest location) {
        return ResponseEntity.ok("Ok");
    }
}
