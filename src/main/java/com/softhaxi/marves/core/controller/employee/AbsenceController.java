package com.softhaxi.marves.core.controller.employee;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.attendance.DailyAttendance;
import com.softhaxi.marves.core.domain.exception.BusinessException;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.attendance.DailyAttendanceRepository;
import com.softhaxi.marves.core.repository.attendance.DispensationRepository;
import com.softhaxi.marves.core.repository.logging.SessionRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;
import com.softhaxi.marves.core.service.employee.AbsenceService;
import com.softhaxi.marves.core.service.employee.EmployeeDivisionService;
import com.softhaxi.marves.core.util.PagingUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Raja Sihombing
 * @since 1
 */
@Controller
@RequestMapping("/absence")
public class AbsenceController {

    private static final Logger logger = LoggerFactory.getLogger(AbsenceController.class);

    @Autowired
    private EmployeeDivisionService divisionService;

    @Autowired
    private AbsenceService dailyService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private DispensationRepository dispensationRepo;

    @Autowired
    private DailyAttendanceRepository dailyRepo;

    @Autowired
    private SessionRepository sessionRepo;

    @Autowired
    private SystemParameterRepository parameterRepo;

    @GetMapping()
    public String index(Model model, @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam(name = "division", required = false) String division) {

        LocalDate now = LocalDate.now();
        if (date == null) {
            date = now;
        }

        ZonedDateTime from = now.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime to = now.plusDays(1).atStartOfDay(ZoneId.systemDefault());
        model.addAttribute("division", division);
        model.addAttribute("divisions", divisionService.findAll());
        model.addAttribute("totalEmployee", userRepo.findAllActiveMobileUser().size());
        model.addAttribute("totalFakeLocator", dailyRepo.findStatisticFakeLocatorByDate(from, to));
        model.addAttribute("totalLogin", sessionRepo.findStatisticStatusSession("VALID", from, to));
        model.addAttribute("totalClockIn", dailyRepo.findStatisticClockInByDate(from, to));
        model.addAttribute("totalClockOut", dailyRepo.findStatisticClockOutByDate(from, to));
        model.addAttribute("totalDispensation", dispensationRepo.findStatisticByDate(now));

        from = date.atStartOfDay(ZoneId.systemDefault());
        to = date.plusDays(1).atStartOfDay(ZoneId.systemDefault());

        List<DailyAttendance> attendances = (List<DailyAttendance>) dailyService.getByDivisionAndDateRange(division, from, to);

        int pageSize = Integer.parseInt(
                parameterRepo.findByCode("PAGINATION_PAGE_SIZE").orElse(new SystemParameter().value("10")).getValue());
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<DailyAttendance> pagination = new PageImpl<>(new LinkedList<>());
        if (null != attendances && !attendances.isEmpty()) {
            int start = (int) pageable.getOffset();
            int end = (start + pageable.getPageSize()) > attendances.size() ? attendances.size()
                    : (start + pageable.getPageSize());
            pagination = new PageImpl<DailyAttendance>((attendances).subList(start, end), pageable, attendances.size());
        }
        int[] pages = PagingUtil.generatePages(pagination.getTotalPages(), pagination.getNumber());

        model.addAttribute("date", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("dateDisplay",
                date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(new Locale("in", "ID"))));
        model.addAttribute("currentPage", page);
        model.addAttribute("startIndex", pageSize * page);
        model.addAttribute("data", pagination);
        model.addAttribute("pages", pages);

        return "absence/index";
    }

    @GetMapping("/user")
    public String user(Model model, @RequestParam(name = "id") String id,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "year", required = false) String year) {
        User user = userRepo.findById(UUID.fromString(id)).orElse(null);
        if (user == null) {
            return "redirect:/absence";
        }
        
        LocalDate now = LocalDate.now();
        LocalDate from = now.with(TemporalAdjusters.firstDayOfMonth());
        
        AtomicReference<Integer> wfo = new AtomicReference<>();
        AtomicReference<Integer> wfh = new AtomicReference<>();
        AtomicReference<Integer> fake = new AtomicReference<>();
        AtomicReference<Integer> late = new AtomicReference<>();
        AtomicReference<Integer> early = new AtomicReference<>();
        AtomicReference<Integer> absent = new AtomicReference<>();
        AtomicReference<Long> sick = new AtomicReference<>();
        AtomicReference<Long> leave = new AtomicReference<>();
        AtomicReference<Long> assignment = new AtomicReference<>();
        AtomicReference<Long> others = new AtomicReference<>();

        wfo.set(0);
        wfh.set(0);
        fake.set(0);
        late.set(0);
        early.set(0);
        absent.set(0);
        sick.set(0L);
        leave.set(0L);
        assignment.set(0L);
        others.set(0L);
        
        List<DailyAttendance> attendances = (List<DailyAttendance>) dailyService.getHistoryByUser(user, year, month, true);
        attendances.forEach((daily) -> {
            if(daily.getId() != null) {
                if (daily.getWorkFrom().equalsIgnoreCase("wfo")) {
                    wfo.set(wfo.get() + 1);
                } else if (daily.getWorkFrom().equalsIgnoreCase("wfh")) {
                    wfh.set(wfh.get() + 1);
                }
                if(daily.isFakeLocator()) {
                    fake.set(fake.get() + 1);
                }
                if(daily.getLate() > 0) {
                    late.set(late.get() + 1);
                }

                if(daily.getEarly() > 0) {
                    early.set(early.get() + 1);
                }
                if(daily.isNotAbsence() && daily.getDispensation() == null) {
                    absent.set(absent.get() + 1);
                }
            } else {
                if(!daily.isWeekend() && daily.getDispensation() == null) {
                    absent.set(absent.get() + 1);
                }
            }
            if(daily.getDispensation() != null) {
                switch (daily.getDispensation().getType()) {
                    case "LEAVE":
                        leave.set(leave.get() + 1);
                        break;
                    case "ASSIGNMENT":
                        assignment.set(assignment.get() + 1);
                        break;
                    case "OTHERS":
                        others.set(others.get() + 1);
                        break;
                    default:
                        sick.set(sick.get() + 1);
                }  
            }
        });
    
        int pageSize = Integer.parseInt(
                parameterRepo.findByCode("PAGINATION_PAGE_SIZE").orElse(new SystemParameter().value("10")).getValue());
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<DailyAttendance> pagination = new PageImpl<>(new LinkedList<>());
        if (null != attendances && !attendances.isEmpty()) {
            int start = (int) pageable.getOffset();
            int end = (start + pageable.getPageSize()) > attendances.size() ? attendances.size()
                    : (start + pageable.getPageSize());
            pagination = new PageImpl<DailyAttendance>((attendances).subList(start, end), pageable, attendances.size());
        }
        int[] pages = PagingUtil.generatePages(pagination.getTotalPages(), pagination.getNumber());

        model.addAttribute("month", from.getMonthValue());
        model.addAttribute("year", from.getYear());
        model.addAttribute("dateDisplay",
                from.format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(new Locale("in", "ID"))));
        model.addAttribute("user", user);
        model.addAttribute("totalLate", late.get());
        model.addAttribute("totalEarly", early.get());
        model.addAttribute("totalWFO", wfo.get());
        model.addAttribute("totalWFH", wfh.get());
        model.addAttribute("totalFake", fake.get());
        model.addAttribute("totalAbsent", absent.get());
        model.addAttribute("totalAssignment", assignment.get());
        model.addAttribute("totalSick", sick.get());
        model.addAttribute("totalLeave", leave.get());
        model.addAttribute("totalOthers", others.get());
        model.addAttribute("currentPage", page);
        model.addAttribute("startIndex", pageSize * page);
        model.addAttribute("data", pagination);
        model.addAttribute("pages", pages);

        return "absence/user";
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download() {
        String filename = "download.xlsx";

        InputStreamResource file;
        try {
            file = new InputStreamResource(dailyService.generateExcelFormat());
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);
        } catch (BusinessException e) {
           return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
