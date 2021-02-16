package com.softhaxi.marves.core.controller.common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.attendance.DailyAttendanceRepository;
import com.softhaxi.marves.core.repository.attendance.DispensationRepository;
import com.softhaxi.marves.core.repository.logging.ActivityLogRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("dashboard")
public class DashboardController {

    @Autowired
	private ActivityLogRepository activityLogRepo;

    @Autowired
    private DailyAttendanceRepository dailyAttendanceRepo;

    @Autowired
    private DispensationRepository dispensationRepo;
    
    @GetMapping()
    public String index(Model model) {
        model.addAttribute("latestUpdated", 
			activityLogRepo.findAll(PageRequest.of(0, 5, 
				Sort.by(Sort.Direction.DESC, "actionTime"))));
        return "dashboard/index";
    }

    @GetMapping("/monthlyAbsence")
    public ResponseEntity<?> getMonthlyAbsence() {
        LocalDate now = LocalDate.now();
        LocalDate from = now.with(TemporalAdjusters.firstDayOfMonth());

        LocalDate to = null;
        if(now.getYear() == from.getYear() && now.getMonthValue() == from.getMonthValue()) {
            to = now.plusDays(1);
        } else {
            to = from.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        }

        final long days = from.until(to, ChronoUnit.DAYS);
        var dateRange = Stream.iterate(from, 
            d -> d.plusDays(1))
            .limit(days)
            .collect(Collectors.toList());
        List<Integer> wfo = new LinkedList<>(Collections.nCopies(dateRange.size(), 0));
        List<Integer> wfh = new LinkedList<>(Collections.nCopies(dateRange.size(), 0));
        List<Integer> dispensation = new LinkedList<>(Collections.nCopies(dateRange.size(), 0));

        Collection<Object[]> absences = dailyAttendanceRepo.findStatisticWorkFromRangeDate(from.atStartOfDay(ZoneId.systemDefault()), 
            to.atStartOfDay(ZoneId.systemDefault()));
        Collection<Object[]> dispensations = dispensationRepo.findStatisticFromRangeDate(from, to);

        absences.stream().forEach((item) -> {
            LocalDate date = LocalDate.parse(item[0].toString()); //LocalDate.ofInstant(record[0].toInstant(), ZoneId.systemDefault());
            var index = dateRange.indexOf(date);
            if(item[1].toString().equalsIgnoreCase("wfo")) {
                wfo.set(index, wfo.get(index) + Integer.parseInt(item[2].toString()));
            } else {
                wfh.set(index, wfh.get(index) + Integer.parseInt(item[2].toString()));
            }
        });

        dispensations.stream().forEach((item) -> {
            LocalDate startDate = LocalDate.parse(item[0].toString()); 
            LocalDate endDate = LocalDate.parse(item[1].toString()); //LocalDate.ofInstant(record[0].toInstant(), ZoneId.systemDefault());
            int[] indexes = IntStream.range(0, dateRange.size())
                .filter((i) -> {
                    LocalDate date = dateRange.get(i);

                    return date.equals(startDate) || date.equals(endDate) 
                        || (date.isAfter(startDate) && date.isBefore(endDate));
                }).toArray();
            for(int index : indexes) {
                dispensation.set(index, dispensation.get(index) + Integer.parseInt(item[2].toString()));
            }
        });
        var formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        var locale = new Locale("in", "ID");
        var dates = new LinkedList<>();
        dateRange.forEach((date) -> {
            dates.add(date.format(formatter.withLocale(locale)));
        });
        
        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                Map.of("dates", dates, "wfo", wfo, "wfh", wfh, "dispensation", dispensation)
            ),
            HttpStatus.OK
        );
    }
}
