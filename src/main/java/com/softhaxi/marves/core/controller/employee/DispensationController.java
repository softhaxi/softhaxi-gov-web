package com.softhaxi.marves.core.controller.employee;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.softhaxi.marves.core.domain.attendance.Dispensation;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.attendance.DispensationRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;
import com.softhaxi.marves.core.service.employee.EmployeeDivisionService;
import com.softhaxi.marves.core.util.PagingUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
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

    @Autowired
    private SystemParameterRepository parameterRepo;

    @GetMapping()
    public String index(Model model, @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam(name = "division", required = false) String division) {

        int pageSize = Integer.parseInt(parameterRepo.findByCode("PAGINATION_PAGE_SIZE").orElse(
            new SystemParameter().value("10")
        ).getValue());

        LocalDate now = LocalDate.now();
        if(date == null) {
            date = now;
        }
        model.addAttribute("date", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("dateDisplay",
                date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(new Locale("in", "ID"))));
        model.addAttribute("totalDispensation", dispensationRepo.findStatisticByDate(now));
        model.addAttribute("totalAssignment", dispensationRepo.findStatisticByTypeAndDate("ASSIGNMENT", now));
        model.addAttribute("totalSick", dispensationRepo.findStatisticByTypeAndDate("SICK", now));
        model.addAttribute("totalLeave", dispensationRepo.findStatisticByTypeAndDate("LEAVE", now));
        model.addAttribute("totalOthers", dispensationRepo.findStatisticByTypeAndDate("OTHERS", now));
        // model.addAttribute("data", dispensationRepo.findAllByDate(now));
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Dispensation> pagination = new PageImpl<>(new LinkedList<>());
        List<Dispensation> dispensations = (List<Dispensation>) dispensationRepo.findAllByDate(date);
        if (null != dispensations && dispensations.size() > 0) {
            int start = (int) pageable.getOffset();
            int end = (start + pageable.getPageSize()) > dispensations.size() ? dispensations.size()
                    : (start + pageable.getPageSize());
            pagination = new PageImpl<Dispensation>((dispensations).subList(start, end),
                    pageable, dispensations.size());
        }
        model.addAttribute("currentPage", page);
        model.addAttribute("startIndex", pageSize * page);
        model.addAttribute("data", pagination);
        int[] pages = PagingUtil.generatePages(pagination.getTotalPages(), pagination.getNumber());

        model.addAttribute("pages", pages);

        model.addAttribute("division", division);
        model.addAttribute("divisions", divisionService.findAll());
        return "dispensation/index";
    }
}
