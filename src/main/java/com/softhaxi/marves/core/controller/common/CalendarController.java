package com.softhaxi.marves.core.controller.common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import com.softhaxi.marves.core.domain.master.CalendarEvent;
import com.softhaxi.marves.core.model.request.CalendarEventRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.master.CalendarEventRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/calendar")
public class CalendarController {

    @Autowired
    private CalendarEventRepository calendarEventRepo;

    @GetMapping
    public String index() {
        return "settings/calendar/index";
    }

    @PostMapping("/event")
    public ResponseEntity<?> actionEvent(@RequestBody CalendarEventRequest request) {
        Map<String, Object> map = new HashMap<>();
        CalendarEvent event = null;
        if(request.getAction().equalsIgnoreCase("insert"))
            event = new CalendarEvent().date(request.getDate()).name(request.getName().trim());
        else if(request.getAction().equalsIgnoreCase("delete")) {
            UUID uuid;
            try {
                uuid = UUID.fromString(request.getId());
            } catch (IllegalArgumentException ex) {
                map.put("id", request.getId());
                map.put("action", "deleted");
                return new ResponseEntity<>(
                        new ErrorResponse(
                            HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), map), 
                            HttpStatus.NOT_FOUND);
            }
            event = calendarEventRepo.findById(uuid).orElse(null);
            if(event == null) {
                return new ResponseEntity<>(
                        new ErrorResponse(
                            HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), "item.not.found"), 
                            HttpStatus.NOT_FOUND);
            }
            event.setDeleted(true);
        }
        calendarEventRepo.save(event);

        map.put("id", event.getId());
        if(request.getAction().equalsIgnoreCase("insert"))
            map.put("action", "inserted");
        else if(request.getAction().equalsIgnoreCase("delete"))
            map.put("action", "deleted");

        return new ResponseEntity<>(new GeneralResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), map),
                HttpStatus.OK);
    }

    @GetMapping("/event/search")
    public ResponseEntity<?> searchEvents(@RequestParam(name = "year", required = false) String paramYear) {
        int year = LocalDate.now().getYear();
        if (paramYear != null)
            year = Integer.parseInt(paramYear);

        Collection<CalendarEvent> events = calendarEventRepo.findAllByYear(year);
        Collection<Map<?, ?>> data = new LinkedList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Map<String, Object> map = null;
        for (CalendarEvent event : events) {
            map = new HashMap<>();
            map.put("id", event.getId().toString());
            map.put("text", event.getName());
            map.put("start_date", event.getDate().atStartOfDay(ZoneId.systemDefault()).format(formatter));
            map.put("end_date", event.getDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).format(formatter));
            data.add(map);
        }
        return new ResponseEntity<>(new GeneralResponse(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data),
                HttpStatus.OK);
    }
}
