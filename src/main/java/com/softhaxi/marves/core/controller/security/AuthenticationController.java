package com.softhaxi.marves.core.controller.security;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.softhaxi.marves.core.domain.logging.Session;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.logging.SessionRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;
import com.softhaxi.marves.core.util.PagingUtil;

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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private SessionRepository sessionRepo;

    @Autowired
    private SystemParameterRepository parameterRepo;

    private Model getSessionPagination(Model model, int page, String filter) {
        LocalDate now = LocalDate.now();

        List<Session> sessions = null;
        if (filter.equalsIgnoreCase("latest")) {
            ZonedDateTime from = now.atStartOfDay(ZoneId.systemDefault());
            ZonedDateTime to = now.plusDays(1).atStartOfDay(ZoneId.systemDefault());
            sessions = (List<Session>) sessionRepo.findAllByStatusAndDateRange("VALID", from, to);
        } else {
            sessions = (List<Session>) sessionRepo.findAllByStatus("VALID");
        }
        int pageSize = Integer.parseInt(
                parameterRepo.findByCode("PAGINATION_PAGE_SIZE").orElse(new SystemParameter().value("10")).getValue());
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Session> pagination = new PageImpl<>(new LinkedList<>());
        if (null != sessions && sessions.size() > 0) {
            int start = (int) pageable.getOffset();
            int end = (start + pageable.getPageSize()) > sessions.size() ? sessions.size()
                    : (start + pageable.getPageSize());
            pagination = new PageImpl<Session>((sessions).subList(start, end), pageable, sessions.size());
        }

        int[] pages = PagingUtil.generatePages(pagination.getTotalPages(), pagination.getNumber());

        model.addAttribute("currentPage", page);
        model.addAttribute("startIndex", pageSize * (page - 1));
        model.addAttribute("data", pagination);
        model.addAttribute("pages", pages);

        return model;
    }

    @GetMapping("/session")
    public String sessionIndex(Model model, @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam(name = "filter", required = false, defaultValue = "all") String filter) {

        model.addAttribute("filter", filter);
        // model.addAttribute("date", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        // model.addAttribute("dateDisplay",
        //         date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(new Locale("in", "ID"))));

        model = getSessionPagination(model, page, filter);

        return "auth/session";
    }

    @PostMapping("/session")
    public String sessionDelete(Model model, @RequestParam(name = "id") String id, 
        @RequestParam(name = "filter", required = false, defaultValue = "all") String filter) {
        Session session = sessionRepo.findById(UUID.fromString(id)).orElseThrow();

        session.setStatus("INVALID");
        sessionRepo.save(session);
        
        model.addAttribute("filter", filter);
        model = getSessionPagination(model, 1, filter);

        return "auth/session-table";
    }
}
