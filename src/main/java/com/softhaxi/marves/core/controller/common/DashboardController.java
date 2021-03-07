package com.softhaxi.marves.core.controller.common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.attendance.DailyAttendanceRepository;
import com.softhaxi.marves.core.repository.attendance.DispensationRepository;
import com.softhaxi.marves.core.repository.logging.ActivityLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private ActivityLogRepository activityLogRepo;

    @Autowired
    private DailyAttendanceRepository dailyAttendanceRepo;

    @Autowired
    private DispensationRepository dispensationRepo;

    @GetMapping()
    public String index(Model model) {
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Collection<GrantedAuthority> roles = (Collection<GrantedAuthority>)
        // auth.getAuthorities();
        // AtomicBoolean sadmin = new AtomicBoolean(false);
        // roles.stream().forEach((role) -> {
        // if(role.getAuthority().equalsIgnoreCase("SADMIN")) {
        // sadmin.set(true);
        // }
        // });

        // if(sadmin.get()) {
        // return "dashboard/super-index";
        // }
        model.addAttribute("dateDisplay",
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(new Locale("in", "ID"))));
        model.addAttribute("latestUpdated",
                activityLogRepo.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "actionTime"))));

        return "dashboard/index";
    }

    @GetMapping("/dailyStatistic")
    public ResponseEntity<?> dailyStatistic(@RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "year", required = false) String year) {
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
        logger.debug("[dailyStatistic] From...." + from.toString());
        logger.debug("[dailyStatistic] To...." + to.toString());

        final long days = from.until(from.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1), ChronoUnit.DAYS);
        var dateRange = Stream.iterate(from, d -> d.plusDays(1)).limit(days).collect(Collectors.toList());
        List<Integer> wfo = new LinkedList<>(Collections.nCopies(dateRange.size(), 0));
        List<Integer> wfh = new LinkedList<>(Collections.nCopies(dateRange.size(), 0));
        List<Integer> dispensation = new LinkedList<>(Collections.nCopies(dateRange.size(), 0));

        Collection<Object[]> absences = dailyAttendanceRepo.findStatisticWorkFromRangeDate(
                from.atStartOfDay(ZoneId.systemDefault()), to.atStartOfDay(ZoneId.systemDefault()));
        Collection<Object[]> dispensations = dispensationRepo.findStatisticFromRangeDate(from,
                from.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1));
        logger.debug("[dailyStatistic] Dispensation...." + dispensations.size());

        absences.stream().forEach((item) -> {
            LocalDate date = LocalDate.parse(item[0].toString());
            var index = dateRange.indexOf(date);
            if (item[1].toString().equalsIgnoreCase("wfo")) {
                wfo.set(index, wfo.get(index) + Integer.parseInt(item[2].toString()));
            } else {
                wfh.set(index, wfh.get(index) + Integer.parseInt(item[2].toString()));
            }
        });
        AtomicReference<LocalDate> toReference = new AtomicReference<>();
        toReference.set(to);
        dispensations.stream().forEach((item) -> {
            LocalDate startDate = LocalDate.parse(item[0].toString());
            LocalDate endDate = LocalDate.parse(item[1].toString()); // LocalDate.ofInstant(record[0].toInstant(),
                                          
                int[] indexes = IntStream.range(0, dateRange.size()).filter((i) -> {
                    LocalDate date = dateRange.get(i);

                    return date.equals(startDate) || date.equals(endDate)
                            || (date.isAfter(startDate) && date.isBefore(endDate));
                }).toArray();
                for (int index : indexes) {
                    if(dateRange.get(index).isBefore(toReference.get()))
                        dispensation.set(index, dispensation.get(index) + Integer.parseInt(item[2].toString()));
                }
        });
        var formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        var locale = new Locale("in", "ID");
        var dates = new LinkedList<>();
        dateRange.forEach((date) -> {
            dates.add(date.format(formatter.withLocale(locale)));
        });

        return new ResponseEntity<>(new GeneralResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(),
                Map.of("dates", dates, "wfo", wfo, "wfh", wfh, "dispensation", dispensation)), HttpStatus.OK);
    }
}
