package com.softhaxi.marves.core.controller.employee;

import com.softhaxi.marves.core.service.employee.EmployeeDivisionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dispensation")
public class DispensationController {

    private static final Logger logger = LoggerFactory.getLogger(DispensationController.class);

    @Autowired
    private EmployeeDivisionService divisionService;

    @GetMapping()
    public String index(Model model, 
        @RequestParam(name = "division", required = false) String division) {
        model.addAttribute("division", division);
        model.addAttribute("divisions", divisionService.findAll());
        return "dispensation/index";
    }
}
