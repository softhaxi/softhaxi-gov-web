package com.softhaxi.marves.core.controller.employee;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.attendance.Dispensation;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.account.UserRepository;
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

    @Autowired
    private UserRepository userRepo;

    @GetMapping()
    public String index(Model model, @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam(name = "division", required = false) String division) {

        LocalDate now = LocalDate.now();
        if (date == null) {
            date = now;
        }
        model.addAttribute("date", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("dateDisplay",
                date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(new Locale("in", "ID"))));
        model.addAttribute("division", division);
        model.addAttribute("divisions", divisionService.findAll());
        model.addAttribute("totalDispensation", dispensationRepo.findStatisticByDate(now));
        model.addAttribute("totalAssignment", dispensationRepo.findStatisticByTypeAndDate("ASSIGNMENT", now));
        model.addAttribute("totalSick", dispensationRepo.findStatisticByTypeAndDate("SICK", now));
        model.addAttribute("totalLeave", dispensationRepo.findStatisticByTypeAndDate("LEAVE", now));
        model.addAttribute("totalOthers", dispensationRepo.findStatisticByTypeAndDate("OTHERS", now));
        // model.addAttribute("data", dispensationRepo.findAllByDate(now));
        int pageSize = Integer.parseInt(
                parameterRepo.findByCode("PAGINATION_PAGE_SIZE").orElse(new SystemParameter().value("10")).getValue());
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Dispensation> pagination = new PageImpl<>(new LinkedList<>());
        List<Dispensation> dispensations = null;
        if (division == null) {
            dispensations = (List<Dispensation>) dispensationRepo.findAllByDate(date);
        } else {
            Collection<Map<?, ?>> employees = divisionService.findEmployeeByDivision(division);
            Collection<String> emails = new LinkedList<>();
            if (employees != null && !employees.isEmpty()) {
                for (Map<?, ?> employee : employees) {
                    emails.add((String) employee.get("email"));
                }
                dispensations = (List<Dispensation>) dispensationRepo.findAllByDateAndEmails(date, emails);
            }
        }
        if (null != dispensations && !dispensations.isEmpty()) {
            int start = (int) pageable.getOffset();
            int end = (start + pageable.getPageSize()) > dispensations.size() ? dispensations.size()
                    : (start + pageable.getPageSize());
            pagination = new PageImpl<Dispensation>((dispensations).subList(start, end), pageable,
                    dispensations.size());
        }
        model.addAttribute("currentPage", page);
        model.addAttribute("startIndex", pageSize * page);
        model.addAttribute("data", pagination);
        int[] pages = PagingUtil.generatePages(pagination.getTotalPages(), pagination.getNumber());

        model.addAttribute("pages", pages);
        return "dispensation/index";
    }

    @GetMapping("/user")
    public String user(Model model, @RequestParam(name = "id") String id,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "year", required = false) String year) {
        User user = userRepo.findById(UUID.fromString(id)).orElse(null);
        if (user == null) {
            return "redirect:/dispensation";
        }

        LocalDate now = LocalDate.now();
        LocalDate from = now.with(TemporalAdjusters.firstDayOfMonth());
        if (year != null && month != null)
            from = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1);
        LocalDate to = null;
        if (now.getYear() == from.getYear() && now.getMonthValue() == from.getMonthValue()) {
            to = now.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        } else {
            to = from.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        }
        List<Dispensation> dispensations = (List<Dispensation>) dispensationRepo.findByUserAndBetweenDates(user, from,
                to);

        AtomicReference<Long> sick = new AtomicReference<>();
        AtomicReference<Long> leave = new AtomicReference<>();
        AtomicReference<Long> assignment = new AtomicReference<>();
        AtomicReference<Long> others = new AtomicReference<>();

        sick.set(0L);
        leave.set(0L);
        assignment.set(0L);
        others.set(0L);
        dispensations.forEach((dispensation) -> {
            switch (dispensation.getType()) {
                case "LEAVE":
                    leave.set(leave.get() + dispensation.getTakingDays());
                    break;
                case "ASSIGNMENT":
                    assignment.set(assignment.get() + dispensation.getTakingDays());
                    break;
                case "OTHERS":
                    others.set(others.get() + dispensation.getTakingDays());
                    break;
                default:
                    sick.set(sick.get() + dispensation.getTakingDays());
            }
        });
        int pageSize = Integer.parseInt(
                parameterRepo.findByCode("PAGINATION_PAGE_SIZE").orElse(new SystemParameter().value("10")).getValue());
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Dispensation> pagination = new PageImpl<>(new LinkedList<>());
        if (null != dispensations && dispensations.size() > 0) {
            int start = (int) pageable.getOffset();
            int end = (start + pageable.getPageSize()) > dispensations.size() ? dispensations.size()
                    : (start + pageable.getPageSize());
            pagination = new PageImpl<Dispensation>((dispensations).subList(start, end), pageable,
                    dispensations.size());
        }
        model.addAttribute("month", from.getMonthValue());
        model.addAttribute("year", from.getYear());
        model.addAttribute("dateDisplay",
                from.format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(new Locale("in", "ID"))));
        model.addAttribute("user", user);
        model.addAttribute("totalAssignment", assignment.get());
        model.addAttribute("totalSick", sick.get());
        model.addAttribute("totalLeave", leave.get());
        model.addAttribute("totalOthers", others.get());
        model.addAttribute("currentPage", page);
        model.addAttribute("startIndex", pageSize * page);
        model.addAttribute("data", pagination);
        int[] pages = PagingUtil.generatePages(pagination.getTotalPages(), pagination.getNumber());

        model.addAttribute("pages", pages);

        return "dispensation/user";
    }
}
