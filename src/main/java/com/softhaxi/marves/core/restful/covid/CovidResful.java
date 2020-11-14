package com.softhaxi.marves.core.restful.covid;

import java.util.Map;

import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/covid")
public class CovidResful {

    @Autowired
    private SystemParameterRepository sysParamRepo;

    @GetMapping
    public ResponseEntity<?> index() {
        RestTemplate restTemplate = new RestTemplate();

        SystemParameter param = sysParamRepo.findByCode("COVIDTRACKER_API_URL").orElse(null);
        if(param != null) {
            return restTemplate.getForEntity(param.getValue() + "/statistic", Map.class);
        }

        return ResponseEntity.ok("Ok");
    }
}
