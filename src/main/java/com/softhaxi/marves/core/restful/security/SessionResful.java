package com.softhaxi.marves.core.restful.security;

import com.softhaxi.marves.core.domain.response.SuccessResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/session")
public class SessionResful {
    @PostMapping("/validate")
    public ResponseEntity<?> validate() {
        return new ResponseEntity<>(new SuccessResponse(
            HttpStatus.OK.value(),
            HttpStatus.OK.getReasonPhrase(),
            "valid"
        ), HttpStatus.OK);
    }
}
