package com.softhaxi.marves.core.controller.employee;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.attendance.DailyAttendance;
import com.softhaxi.marves.core.domain.attendance.Dispensation;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.model.employee.Absence;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.attendance.DailyAttendanceRepository;
import com.softhaxi.marves.core.repository.attendance.DispensationRepository;
import com.softhaxi.marves.core.repository.logging.SessionRepository;
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

        List<User> users = null;
        Collection<String> emails = new LinkedList<>();
        if (division != null) {
            Collection<Map<?, ?>> employees = divisionService.findEmployeeByDivision(division);

            if (employees != null && !employees.isEmpty()) {
                for (Map<?, ?> employee : employees) {
                    emails.add((String) employee.get("email"));
                }
            }
            users = (List<User>) userRepo.findAllByEmails(emails);
        } else {
            users = (List<User>) userRepo.findAllActiveMobileUser();
        }

        emails = new LinkedList<>();
        for (User user : users) {
            emails.add(user.getEmail());
        }

        from = date.atStartOfDay(ZoneId.systemDefault());
        to = date.plusDays(1).atStartOfDay(ZoneId.systemDefault());
        Collection<DailyAttendance> attendances = dailyRepo.findAllByEmailsAndDate(emails, from, to);
        Collection<Dispensation> dispensations = dispensationRepo.findAllByDateAndEmails(date, emails);

        AtomicReference<List<Absence>> attends = new AtomicReference<>();
        AtomicReference<List<Absence>> dispens = new AtomicReference<>();
        AtomicReference<List<Absence>> nonAttends = new AtomicReference<>();

        attends.set(new LinkedList<>());
        dispens.set(new LinkedList<>());
        nonAttends.set(new LinkedList<>());
        users.forEach((user) -> {
            DailyAttendance attendance = attendances.stream().filter(item -> item.getUser().equals(user)).findFirst()
                    .orElse(null);
            Dispensation dispensation = dispensations.stream().filter(item -> item.getUser().equals(user)).findFirst()
                    .orElse(null);

            if (attendance == null) {
                if (dispensation != null) {
                    var temp = dispens.get();
                    temp.add(new Absence().userId(user.getId()).email(user.getEmail())
                            .fullName(user.getProfile() != null ? user.getProfile().getFullName() : null)
                            .divisionName(user.getEmployee() != null ? user.getEmployee().getDivisionName() : null)
                            .dispensationId(dispensation.getId()).dispensation(dispensation.getType())
                            .dispensationReason(dispensation.getDescription()));
                    dispens.set(temp);
                } else {
                    var temp = nonAttends.get();
                    temp.add(new Absence().userId(user.getId()).email(user.getEmail())
                            .fullName(user.getProfile() != null ? user.getProfile().getFullName() : null)
                            .divisionName(user.getEmployee() != null ? user.getEmployee().getDivisionName() : null));
                }
            } else {
                Absence absence = new Absence().userId(user.getId()).email(user.getEmail())
                        .fullName(user.getProfile() != null ? user.getProfile().getFullName() : null)
                        .divisionName(user.getEmployee() != null ? user.getEmployee().getDivisionName() : null)
                        .workFrom(attendance.getWorkFrom()).clockInTime(attendance.getDateTime())
                        .clockInIpAddress(attendance.getIpAddress()).clockInMockLocation(attendance.isMockLocation())
                        .clockOutTime(attendance.getOutDateTime()).clockOutIpAddress(attendance.getOutIpAddress())
                        .clockOutMockLocation(attendance.isOutMockLocation());
                if(dispensation != null) {
                    absence.setDispensationId(dispensation.getId());
                    absence.setDispensation(dispensation.getType());
                    absence.setDispensationReason(dispensation.getDescription());
                }
                var temp = attends.get();
                temp.add(absence);
                attends.set(temp);
            }
        });
        List<Absence> absences = attends.get();
        absences.addAll(dispens.get());
        absences.addAll(nonAttends.get());

        int pageSize = Integer.parseInt(
                parameterRepo.findByCode("PAGINATION_PAGE_SIZE").orElse(new SystemParameter().value("10")).getValue());
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Absence> pagination = new PageImpl<>(new LinkedList<>());
        if (null != absences && !absences.isEmpty()) {
            int start = (int) pageable.getOffset();
            int end = (start + pageable.getPageSize()) > absences.size() ? absences.size()
                    : (start + pageable.getPageSize());
            pagination = new PageImpl<Absence>((absences).subList(start, end), pageable, absences.size());
        }
        int[] pages = PagingUtil.generatePages(pagination.getTotalPages(), pagination.getNumber());

        model.addAttribute("date", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("dateDisplay",
                date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(new Locale("in", "ID"))));
        model.addAttribute("division", division);
        model.addAttribute("divisions", divisionService.findAll());
        model.addAttribute("totalEmployee", userRepo.findAllActiveMobileUser().size());
        model.addAttribute("totalFakeLocator", dailyRepo.findStatisticFakeLocatorByDate(from, to));
        model.addAttribute("totalLogin", sessionRepo.findStatisticStatusSession("VALID", from, to));
        model.addAttribute("totalClockIn", dailyRepo.findStatisticClockInByDate(from, to));
        model.addAttribute("totalClockOut", dailyRepo.findStatisticClockOutByDate(from, to));
        model.addAttribute("totalDispensation", dispensationRepo.findStatisticByDate(now));
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
        if (year != null && month != null)
            from = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1);
        LocalDate to = null;
        if (now.getYear() == from.getYear() && now.getMonthValue() == from.getMonthValue()) {
            to = now.plusDays(1);
        } else {
            to = from.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        }
        final long days = from.until(to, ChronoUnit.DAYS);
        List<LocalDate> dateRange = Stream.iterate(from, d -> d.plusDays(1)).limit(days).collect(Collectors.toList());

        List<Dispensation> dispensations = (List<Dispensation>) dispensationRepo.findByUserAndBetweenDates(user, from,
                to);
        List<DailyAttendance> attendances = (List<DailyAttendance>) dailyRepo.findAllByEmailsAndDate(
                Arrays.asList(user.getEmail()), from.atStartOfDay(ZoneId.systemDefault()),
                to.atStartOfDay(ZoneId.systemDefault()));

        AtomicReference<List<Absence>> data = new AtomicReference<>();
        AtomicReference<Integer> wfo = new AtomicReference<>();
        AtomicReference<Integer> wfh = new AtomicReference<>();
        AtomicReference<Integer> fake = new AtomicReference<>();

        wfo.set(0);
        wfh.set(0);
        fake.set(0);
        dateRange.forEach((date) -> {
            List<Absence> list = data.get();
            if (list == null)
                list = new LinkedList<>();
            DailyAttendance attendance = attendances.stream()
                    .filter(item -> item.getDateTime().toLocalDate().equals(date)).findFirst().orElse(null);
            if (attendance == null) {
                Dispensation dispensation = dispensations.stream()
                        .filter(item -> item.getStartDate().equals(date)
                                || (item.getStartDate().isBefore(date) && item.getEndDate().isAfter(date))
                                || item.getEndDate().equals(date))
                        .findFirst().orElse(null);
                if (dispensation != null) {
                    list.add(new Absence().date(date)
                            .weekend(date.getDayOfWeek() == DayOfWeek.SUNDAY
                                    || date.getDayOfWeek() == DayOfWeek.SATURDAY)
                            .dispensationId(dispensation.getId()).dispensation(dispensation.getType())
                            .dispensationReason(dispensation.getDescription()));
                } else {
                    list.add(new Absence().date(date).weekend(
                            date.getDayOfWeek() == DayOfWeek.SUNDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY));
                }
            } else {
                if (attendance.getWorkFrom().equalsIgnoreCase("wfo")) {
                    wfo.set(wfo.get() + 1);
                } else if (attendance.getWorkFrom().equalsIgnoreCase("wfh")) {
                    wfh.set(wfh.get() + 1);
                }

                if (attendance.isMockLocation() || attendance.isOutMockLocation()) {
                    fake.set(fake.get() + 1);
                }
                list.add(new Absence().date(date)
                        .weekend(date.getDayOfWeek() == DayOfWeek.SUNDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY)
                        .workFrom(attendance.getWorkFrom()).clockInTime(attendance.getDateTime())
                        .clockInIpAddress(attendance.getIpAddress()).clockInMockLocation(attendance.isMockLocation())
                        .clockOutTime(attendance.getOutDateTime()).clockOutIpAddress(attendance.getOutIpAddress())
                        .clockOutMockLocation(attendance.isOutMockLocation()));
            }
            data.set(list);
        });

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
        Page<Absence> pagination = new PageImpl<>(new LinkedList<>());
        List<Absence> absences = data.get();
        if (null != absences && !absences.isEmpty()) {
            int start = (int) pageable.getOffset();
            int end = (start + pageable.getPageSize()) > absences.size() ? absences.size()
                    : (start + pageable.getPageSize());
            pagination = new PageImpl<Absence>((absences).subList(start, end), pageable, absences.size());
        }
        int[] pages = PagingUtil.generatePages(pagination.getTotalPages(), pagination.getNumber());

        model.addAttribute("month", from.getMonthValue());
        model.addAttribute("year", from.getYear());
        model.addAttribute("dateDisplay",
                from.format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(new Locale("in", "ID"))));
        model.addAttribute("user", user);
        model.addAttribute("totalLate", 0);
        model.addAttribute("totalEarly", 0);
        model.addAttribute("totalWFO", wfo.get());
        model.addAttribute("totalWFH", wfh.get());
        model.addAttribute("totalFake", fake.get());
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
}
