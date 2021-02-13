package com.softhaxi.marves.core.controller.integration;

import org.springframework.security.access.method.P;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("covid")
public class CovidTrackerController {
    @GetMapping()
    public String index() {
        return "covidtracker/index";
    }
}
