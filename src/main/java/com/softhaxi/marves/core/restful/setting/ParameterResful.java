package com.softhaxi.marves.core.restful.setting;

import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.domain.response.ErrorResponse;
import com.softhaxi.marves.core.domain.response.SuccessResponse;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/param")
public class ParameterResful {

    @Autowired
    private SystemParameterRepository paramRepo;

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(name = "key") String key) {
        SystemParameter parameter = paramRepo.findByCode(key.trim().toUpperCase()).orElse(null);
        if(parameter == null)
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );  

        return new ResponseEntity<>(
            new SuccessResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                parameter
            ),
            HttpStatus.OK
        ); 
    }
}
