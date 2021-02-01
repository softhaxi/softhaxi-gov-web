package com.softhaxi.marves.core.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.attendance.DailyAttendance;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.attendance.DailyAttendanceRepository;
import com.softhaxi.marves.core.repository.attendance.DispensationRepository;
import com.softhaxi.marves.core.repository.employee.EmployeeRepository;
import com.softhaxi.marves.core.repository.logging.ActivityLogRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AbsenceService
 */

@Service
public class AbsenceWebService {
    private static final Logger logger = LoggerFactory.getLogger(AbsenceWebService.class);

    @Autowired
    private DailyAttendanceRepository dailyAttendanceRepository;

    @Autowired
    private SystemParameterRepository systemParameterRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DispensationRepository dispensationRepository;

    public List<DailyAttendance> findUserHistoryByMonthYear(User user, int month, int year) {
        List<DailyAttendance> attendances = dailyAttendanceRepository.findUserHistoryByMonthYear(user, month, year);

        attendances = this.resetAttendanceTimeStatus(attendances);


      /*  Map<String, Object> attendanceMap = new HashMap<>();
        int totalFakeLocator = 0;
        for (DailyAttendance dailyAttendance : attendances) {
            if(dailyAttendance.isMockLocation()){
                ++totalFakeLocator;
            }
        }

        int totalEmployee = employeeRepository.findAll().size();
        int login = activityLogRepository.findUserByActionName("log.in");
        int inPresensi = activityLogRepository.findUserByActionName("clock.in");
        int outPresensi = activityLogRepository.findUserByActionName("clockOut.in");
        int dispensasi = dispensationRepository.findDispensationByRangeDate().size();

        attendanceMap.put("totalEmployee", totalEmployee);
        attendanceMap.put("totalFakeLocator", totalFakeLocator);
        attendanceMap.put("login", login);
        attendanceMap.put("inPresensi", inPresensi);
        attendanceMap.put("outPresensi", outPresensi);
        attendanceMap.put("dispensasi", dispensasi);
        attendanceMap.put("attendances", attendances);*/

        return attendances;
    }

    public Map<String, Object> findAll() {
        Map<String, Object> attendanceMap = new HashMap<>();
        List<DailyAttendance> attendances = dailyAttendanceRepository.findAll();
        int totalFakeLocator = 0;

        attendances = this.resetAttendanceTimeStatus(attendances);

        for (DailyAttendance dailyAttendance : attendances) {
            if(dailyAttendance.isMockLocation()){
                ++totalFakeLocator;
            }
        }

        int totalEmployee = employeeRepository.findAll().size();
        int login = activityLogRepository.findUserByActionName("log.in");
        int inPresensi = activityLogRepository.findUserByActionName("clock.in");
        int outPresensi = activityLogRepository.findUserByActionName("clock.out");
        int dispensasi = dispensationRepository.findDispensationByRangeDate().size();

        attendanceMap.put("totalEmployee", totalEmployee);
        attendanceMap.put("totalFakeLocator", totalFakeLocator);
        attendanceMap.put("login", login);
        attendanceMap.put("inPresensi", inPresensi);
        attendanceMap.put("outPresensi", outPresensi);
        attendanceMap.put("dispensasi", dispensasi);
        attendanceMap.put("attendances", attendances);

        return attendanceMap;
    }

    public List<ZonedDateTime> getWorkingTimeSysParam() {

        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();

        String inWorkingParam = "MAX_CLOCK_IN_DAILY";
        String outWorkingParam = "MAX_CLOCK_OUT_DAILY";

        if (dayOfWeek == 5) {
            inWorkingParam = "MAX_CLOCK_IN_FRIDAY";
            outWorkingParam = "MAX_CLOCK_OUT_FRIDAY";
        }
        List<ZonedDateTime> zonedDateTimes = new ArrayList<>();

        Optional<SystemParameter> startWorkingTimeSysParam = systemParameterRepository.findByCode(inWorkingParam);
        Optional<SystemParameter> endWorkingTimeSysParam = systemParameterRepository.findByCode(outWorkingParam);

        LocalTime localTime = LocalTime.parse(startWorkingTimeSysParam.get().getValue());
        LocalTime localOutTime = LocalTime.parse(endWorkingTimeSysParam.get().getValue());

        LocalDateTime inLdt = LocalDateTime.of(LocalDate.now(), localTime);
        LocalDateTime outLdt = LocalDateTime.of(LocalDate.now(), localOutTime);

        ZonedDateTime startWorkingTime = ZonedDateTime.of(inLdt, ZoneId.systemDefault());
        ZonedDateTime endWorkingTime = ZonedDateTime.of(outLdt, ZoneId.systemDefault());

        zonedDateTimes.add(startWorkingTime);
        zonedDateTimes.add(endWorkingTime);

        return zonedDateTimes;
    }

    private List<LocalTime> getLocalTimeFromSysParam() {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_TIME;

        List<LocalTime> localTimes = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();

        String inWorkingParam = "MAX_CLOCK_IN_DAILY";
        String outWorkingParam = "MAX_CLOCK_OUT_DAILY";

        if (dayOfWeek == 5) {
            inWorkingParam = "MAX_CLOCK_IN_FRIDAY";
            outWorkingParam = "MAX_CLOCK_OUT_FRIDAY";
        }

        Optional<SystemParameter> startWorkingTimeSysParam = systemParameterRepository.findByCode(inWorkingParam);
        Optional<SystemParameter> endWorkingTimeSysParam = systemParameterRepository.findByCode(outWorkingParam);

        LocalTime localTime = LocalTime.parse(startWorkingTimeSysParam.get().getValue(), formatter);
        LocalTime localOutTime = LocalTime.parse(endWorkingTimeSysParam.get().getValue(), formatter);

        localTimes.add(localTime);
        localTimes.add(localOutTime);
        return localTimes;
    }

    private static LocalTime getLocalTime(ZonedDateTime zonedDateTime) {
        LocalDateTime ldt = zonedDateTime.toLocalDateTime();
        LocalTime local = ldt.toLocalTime();
        return local;
    }

    private List<DailyAttendance> resetAttendanceTimeStatus(List<DailyAttendance> attendances) {
        List<LocalTime> localTimesParam = this.getLocalTimeFromSysParam();
        for (DailyAttendance dailyAttendance : attendances) {

            if (null != dailyAttendance.getDateTime()) {
                LocalTime inLocalTime = getLocalTime(dailyAttendance.getDateTime());
                if (inLocalTime.isAfter(localTimesParam.get(0))) {
                    dailyAttendance.setComeLate(true);
                }
            }
            if (null != dailyAttendance.getOutDateTime()) {
                LocalTime outLocalTime = getLocalTime(dailyAttendance.getOutDateTime());
                if (outLocalTime.isBefore(localTimesParam.get(1))) {
                    dailyAttendance.setGoBackEarly(true);
                }
            }

        }
        return attendances;
    }
}