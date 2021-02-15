package com.softhaxi.marves.core.controller.employee;

import java.time.LocalDate;
import java.util.Collection;

import com.softhaxi.marves.core.repository.attendance.DispensationRepository;
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

    @Autowired
    private DispensationRepository dispensationRepo;

    @GetMapping()
    public String index(Model model, 
        @RequestParam(name = "day", required = false) String day,
        @RequestParam(name = "month", required = false) String month,
        @RequestParam(name = "year", required = false) String year,
        @RequestParam(name = "division", required = false) String division) {

        LocalDate now = LocalDate.now();
        model.addAttribute("totalDispensation", dispensationRepo.findStatisticByDate(now));
        model.addAttribute("totalAssignment", dispensationRepo.findStatisticByTypeAndDate("ASSIGNMENT", now));
        model.addAttribute("totalSick", dispensationRepo.findStatisticByTypeAndDate("SIGCK", now));
        model.addAttribute("totalLeave", dispensationRepo.findStatisticByTypeAndDate("LEAVE", now));
        model.addAttribute("totalOthers", dispensationRepo.findStatisticByTypeAndDate("OTHERS", now));

        model.addAttribute("division", division);
        model.addAttribute("divisions", divisionService.findAll());
        return "dispensation/index";
    }
}
