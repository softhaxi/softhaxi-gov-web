package com.softhaxi.marves.core.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.attendance.DailyAttendance;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.attendance.DailyAttendanceRepository;
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

    public List<DailyAttendance> findUserHistoryByMonthYear(User user, int month, int year) {
        List<DailyAttendance> attendances = dailyAttendanceRepository.findUserHistoryByMonthYear(user, month, year);

        attendances = this.resetAttendanceTimeStatus(attendances);

        return attendances;
    }

    public List<DailyAttendance> findAll() {
        List<DailyAttendance> attendances = dailyAttendanceRepository.findAll();

        attendances = this.resetAttendanceTimeStatus(attendances);

        return attendances;
    }

    public List<ZonedDateTime> getWorkingTimeSysParam() {

        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();
        logger.debug("dayOfWeek: " + dayOfWeek);

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
        logger.debug("dayOfWeek: " + dayOfWeek);

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
                logger.debug("inLocalTime: " + inLocalTime);
                logger.debug("localTimesParam.get(0): " + localTimesParam.get(0));
                if (inLocalTime.isAfter(localTimesParam.get(0))) {
                    dailyAttendance.setComeLate(true);
                }
            }
            if (null != dailyAttendance.getOutDateTime()) {
                LocalTime outLocalTime = getLocalTime(dailyAttendance.getOutDateTime());
                logger.debug("outLocalTime: " + outLocalTime);
                logger.debug("localTimesParam.get(1): " + localTimesParam.get(1));
                if (outLocalTime.isBefore(localTimesParam.get(1))) {
                    dailyAttendance.setGoBackEarly(true);
                }
            }

        }
        return attendances;
    }
}