package com.softhaxi.marves.core.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/calendar")
public class CalendarController {
    
    @GetMapping
    public String index() {
        return "settings/calendar/index";
    }
}
