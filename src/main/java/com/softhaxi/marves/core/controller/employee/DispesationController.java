package com.softhaxi.marves.core.controller.employee;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dispensation")
public class DispesationController {
    @GetMapping()
    public String index() {
        return "dispensation/index";
    }
}
