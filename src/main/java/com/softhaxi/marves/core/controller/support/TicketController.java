package com.softhaxi.marves.core.controller.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.Gson;
import com.softhaxi.marves.core.domain.employee.Employee;
import com.softhaxi.marves.core.domain.support.Ticket;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.employee.EmployeeRepository;
import com.softhaxi.marves.core.service.support.TicketService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequestMapping("/ticket")
public class TicketController {

    Logger logger = LoggerFactory.getLogger(TicketController.class);

    @Autowired
    private TicketService ticketService;

    @Autowired 
    UserRepository userRepository;

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

    @GetMapping("/open-ticket")
    public String action(Model model){
        Collection<Ticket> ticketList = ticketService.findAllOrderByDateTimeDesc();

    
        Ticket ticket = ticketList.iterator().next();
        model.addAttribute("ticketNo", ticket.getCode());
        if(null!=ticket.getUser().getProfile()){
            model.addAttribute("userName", ticket.getUser().getProfile().getFullName());
        }else{
            model.addAttribute("userName", ticket.getUser().getUsername());
        }
        model.addAttribute("ticketContent", ticket.getContent());
        model.addAttribute("ticketStatus", ticket.getStatus());

        model.addAttribute("tickets", ticketList);

        return "ticket/list2";
    }

    @PostMapping(value="/ticket-filter")
    public String findUserByName(Model model, @RequestParam("ticketcode") Optional<String> ticketCode) {
        String strTticketCode = ticketCode.orElse("");
        Optional<Ticket> optTicket = ticketService.findTicketByCode(strTticketCode);    
        if(optTicket.isPresent()){
            Ticket ticket = optTicket.orElse(new Ticket());
            model.addAttribute("ticketNo", ticket.getCode());
            if(null!=ticket.getUser().getProfile()){
                model.addAttribute("userName", ticket.getUser().getProfile().getFullName());
            }else{
                model.addAttribute("userName", ticket.getUser().getUsername());
            }
            model.addAttribute("ticketContent", ticket.getContent());
            model.addAttribute("ticketStatus", ticket.getStatus());
        }
        
        return "ticket/list2";
    }
    
    @PostMapping(value = "/update-status")
    public String updateTicketStatus(Model model, @RequestParam("ticketcode") Optional<String> ticketCode, @RequestParam("status") Optional<String> status) {
        Map<String, String> statusMap = new HashMap<>();
        String strTticketCode = ticketCode.orElse("");
        logger.debug("strTticketCode: " + strTticketCode);
        if(ticketCode.isPresent() && status.isPresent()){
            try {
                ticketService.updateTicketStatus(ticketCode.get(), status.get());
            } catch (Exception e) {
                statusMap.put("status", "error");
                e.printStackTrace();
            }
        }

        Optional<Ticket> optTicket = ticketService.findTicketByCode(strTticketCode);    
        if(optTicket.isPresent()){
            Ticket ticket = optTicket.orElse(new Ticket());
            model.addAttribute("ticketNo", ticket.getCode());
            if(null!=ticket.getUser().getProfile()){
                model.addAttribute("userName", ticket.getUser().getProfile().getFullName());
            }else{
                model.addAttribute("userName", ticket.getUser().getUsername());
            }
            model.addAttribute("ticketContent", ticket.getContent());
            model.addAttribute("ticketStatus", ticket.getStatus());
        }
        return "ticket/list2";
    }

    @PostMapping(value="/search-ticket")
    public String searchTicketByCode(Model model, @RequestParam("ticketcode") Optional<String> ticketCode) {


        String strTicketCode = ticketCode.orElse("");

        Collection<Ticket> ticketList = ticketService.findTicketLikeCode(strTicketCode);

        Ticket ticket = ticketList.iterator().next();
        model.addAttribute("ticketNo", ticket.getCode());
        if(null!=ticket.getUser().getProfile()){
            model.addAttribute("userName", ticket.getUser().getProfile().getFullName());
        }else{
            model.addAttribute("userName", ticket.getUser().getUsername());
        }
        model.addAttribute("ticketContent", ticket.getContent());
        model.addAttribute("ticketStatus", ticket.getStatus());

        model.addAttribute("tickets", ticketList);

        return "ticket/list2";
    }
    
}
