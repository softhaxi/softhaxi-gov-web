package com.softhaxi.marves.core.controller.support;

import java.util.UUID;

import com.softhaxi.marves.core.domain.support.Ticket;
import com.softhaxi.marves.core.service.support.TicketService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/ticket")
public class TicketController {
    @Autowired
    private TicketService ticketService;

    @GetMapping()
    public String index(
        @RequestParam(value="page", required = false, defaultValue = "1") int page,
        @RequestParam(value="status", required = false, defaultValue = "open") String status,
        @RequestParam(value="id", required = false) String id,
        Model model) {
        if(id != null) {
            return "ticket/detail";
        }

        //Page<Ticket> pagination = ticketService.findAllPaginated(page, 10);
        //Collection<Ticket> data = pagination.getContent();

        // model.addAttribute("currentPage", page);
        // model.addAttribute("totalPages", pagination.getTotalPages());
        // model.addAttribute("totalItems", pagination.getTotalElements());
        // model.addAttribute("status", status);
        model.addAttribute("data", ticketService.findAllOrderByDateTimeDesc());

        return "ticket/list";
    }

    @GetMapping("/{id}")
    public String action(@PathVariable String id,
        @RequestParam(value="action", defaultValue="detail") String action,
        Model model) {
        
        Ticket ticket = new Ticket().id(UUID.fromString(id));

        ticketService.performAction(ticket, action);
        
        model.addAttribute("data", ticketService.findAllOrderByDateTimeDesc());
        return "ticket/list";
    }
}
