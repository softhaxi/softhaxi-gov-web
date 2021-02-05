package com.softhaxi.marves.core.controller.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
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
    private EmployeeRepository employeeRepository;

    @Autowired UserRepository userRepository;

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
        List<Employee> employeeList = new ArrayList<>();
        for (Ticket ticket : ticketList) {
            Optional<Employee> employee = employeeRepository.findEmployeeByUserName(ticket.getUser());
            if(employee.isPresent()){
                employeeList.add(employee.get());
            }
        }
        model.addAttribute("tickets", ticketList);
        model.addAttribute("employees", employeeList);
        return "ticket/list2";
    }

    @PostMapping(value="/ticket-filter")
    public String findUserByName(Model model, @RequestParam("userid") Optional<String> userId) {
        String strUserId = userId.orElse("");
        logger.debug("strUserId: " +strUserId);
        User user = userRepository.findById(UUID.fromString(strUserId)).get();
        Employee employee = employeeRepository.findEmployeeByUserName(user).get();
        Collection<Ticket> ticketList = ticketService.findByUserId(user);
        model.addAttribute("employeeNo", employee.getEmployeeNo());
        if(null!=employee.getUser().getProfile()){
            model.addAttribute("employeeName", employee.getUser().getProfile().getFullName());
        }else{
            model.addAttribute("employeeName", employee.getUser().getUsername());
        }
        for (Ticket ticket : ticketList) {
            model.addAttribute("ticketContent", ticket.getContent());
        }
        return "ticket/list2";
    }
    
}
