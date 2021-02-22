package com.softhaxi.marves.core.controller.common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.softhaxi.marves.core.domain.master.CalendarEvent;
import com.softhaxi.marves.core.repository.master.CalendarEventRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    public @ResponseBody String postEvent(
        @RequestParam(name = "name") String name, 
        @RequestParam(name = "date") @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        CalendarEvent event = new CalendarEvent()
            .date(date)
            .name(name.trim());
        calendarEventRepo.save(event);
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("action", "inserted");

        Gson gson = new Gson();
        return gson.toJson(map);
    }

    @PostMapping("/event/action")
    public @ResponseBody String actionEvent(
        @RequestParam(name = "id") String id,
        @RequestParam(name = "action") String action) {
        CalendarEvent event = calendarEventRepo.findById(UUID.fromString(id)).orElseThrow();
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", event.getId());

        if(action.equalsIgnoreCase("delete")) {
            event.setDeleted(true);
            map.put("action", "deleted");
        } 
        calendarEventRepo.save(event);

        Gson gson = new Gson();
        return gson.toJson(map);
    }

    @GetMapping("/event/search")
    public @ResponseBody String searchEvents(@RequestParam(name = "year", required = false) String paramYear) {
        int year = LocalDate.now().getYear();
        if(paramYear != null) 
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
        Gson gson = new Gson();
        return gson.toJson(data);
    }
}
