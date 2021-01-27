package com.softhaxi.marves.core.controller.employee;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.attendance.Attendance;
import com.softhaxi.marves.core.domain.attendance.DailyAttendance;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.attendance.DailyAttendanceRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;
import com.unboundid.util.json.JSONField;
import com.unboundid.util.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Raja Sihombing
 * @since 1
 */
@Controller
public class AbsenceController {

    private static final Logger logger = LoggerFactory.getLogger(AbsenceController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyAttendanceRepository dailyAttendanceRepository;

    @Autowired
    private static SystemParameterRepository systemParameterRepository;

    @GetMapping("/absence")
    public String getAllAbsences(Model model) {

        List<DailyAttendance> attendanceList = dailyAttendanceRepository.findAll();


        LocalDate today = LocalDate.now();
        int defaultMonth = today.getMonthValue();
        int defaultYear = today.getYear();
        
        model.addAttribute("month", defaultMonth);
        model.addAttribute("year", defaultYear);

        ZonedDateTime startWorkingTime = ZonedDateTime.of(2020, 12, 12, 9, 0, 0, 0, ZoneId.of("UTC"));// todo add from
                                                                                                      // System
                                                                                                      // Parameter
        ZonedDateTime endWorkingTime = ZonedDateTime.of(2020, 12, 12, 18, 0, 0, 0, ZoneId.of("UTC"));

        model.addAttribute("startWorkingTime", startWorkingTime);
        model.addAttribute("endWorkingTime", endWorkingTime);
        model.addAttribute("attendanceList", attendanceList);
        return "common/absence/absence-list";
    }

    @GetMapping("/absence/find-user-byname")
    public @ResponseBody String findUserByName(Model model, @RequestParam("username") Optional<String> name) {
        String strName = name.orElse("");
        
        
        List<User> users = userRepository.findUserByUsernameLike(strName.toUpperCase());
        
        
        List<Map<String, String>> userList = new ArrayList<>();
        
        String json = "";
        
        try {
            Map<String, String> userMap = new HashMap<>();
            for (User user : users) {
                userMap = new HashMap<>();
                userMap.put("username", user.getUsername());
                userMap.put("name", user.getProfile().getFullName());
                userList.add(userMap);
            }
            Gson gson = new Gson();
            json = gson.toJson(userList);
            logger.debug("Json: " + json);
        } catch (Exception e) {
            
        }

        return json;
    }

    @PostMapping("/absence-filter")
    public String getAbsenceByQuery(Model model, @RequestParam("username") Optional<String> name,
            @RequestParam("month") Optional<Integer> month, @RequestParam("year") Optional<Integer> year) {
        String strName = name.orElse("");


        LocalDate today = LocalDate.now();
        int defaultMonth = today.getMonthValue();
        int defaultYear = today.getYear();

        int intMonth = month.orElse(defaultMonth);
        int intYear = year.orElse(defaultYear);
        logger.debug(strName +" "+intMonth + " " +intYear);
        List<User> users = userRepository.findUserByUsernameLike(strName);
        List<DailyAttendance> dailyAttendances = new ArrayList<>();
        List<DailyAttendance> dailyAttendancesPerUser = new ArrayList<>();
        
        for (User user : users) {
            
            dailyAttendancesPerUser = dailyAttendanceRepository.findUserHistoryByMonthYear(user, intMonth, intYear);
            dailyAttendances.addAll(dailyAttendancesPerUser);
        }
        logger.debug("dailyAttendances: "+dailyAttendances);
        ZonedDateTime startWorkingTime = ZonedDateTime.of(2020, 12, 12, 9, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime endWorkingTime = ZonedDateTime.of(2020, 12, 12, 18, 0, 0, 0, ZoneId.of("UTC"));

        model.addAttribute("startWorkingTime", startWorkingTime);
        model.addAttribute("endWorkingTime", endWorkingTime);
        model.addAttribute("attendanceList", dailyAttendances);

        return "common/absence/absence-list";
    }

    private static List<ZonedDateTime> setWorkingTime(){
        List<ZonedDateTime> zonedDateTime = new ArrayList<>();

        Optional<SystemParameter> inWorkingTime = systemParameterRepository.findByCode("MAX_CLOCK_IN_DAILY");
        Optional<SystemParameter> outWorkingTime = systemParameterRepository.findByCode("MAX_CLOCK_OUT_DAILY");
        if(inWorkingTime.isPresent()){
           // ZonedDateTime d=inWorkingTime.get().getValue();
        }
        if(outWorkingTime.isPresent()){
            
        }


        return zonedDateTime;
    }

}
