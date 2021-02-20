package com.softhaxi.marves.core.controller.common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Invitation;
import com.softhaxi.marves.core.repository.employee.InvitationRepository;
import com.softhaxi.marves.core.service.AgendaService;
import com.softhaxi.marves.core.service.account.UserService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * AgendaController
 */
@Controller
public class AgendaController {

    Logger logger = LoggerFactory.getLogger(AgendaController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private AgendaService agendaService;

    @GetMapping("/agenda")
    public String getAgenda(Model model, @RequestParam("date") Optional<Date> date) {
        Date startDate = date.orElse(new Date());
        logger.debug("startDate: " + startDate);
        LocalDate localDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Collection<User> userList = userService.findAll();

        Collection<Invitation> invitations = new ArrayList<>();
        List<Invitation> invitationList = new ArrayList<>();

        JSONArray jsonArray = new JSONArray();

        for (User user : userList) {
            invitations = invitationRepository.findAllUserDailyInvitationByCreated(user.getId().toString(),
                    localDate);
                    
                    agendaService.setMember(invitations);              
                    
                    for (Iterator<Invitation> iterator = invitations.iterator(); iterator.hasNext();) {
                        jsonArray.put(iterator.next());
                    }

            invitationList.addAll(invitations);
        }
        
        logger.debug("jsonArray.toList(): " +jsonArray.toList());
        model.addAttribute("invitationObject", jsonArray.toList());
        
        model.addAttribute("invitations", invitationList);

        return "agenda/index";
    }

    @PostMapping("/agenda/search-employee")
    public String searchEmployee(Model model, @RequestParam("name") Optional<String> name) {
        String strName = name.orElse("");
        try {
            List<User> userList = userService.findUserByUsernameLike(strName);
    
            Collection<Invitation> invitations = new ArrayList<>();
            Collection<Invitation> invitationList = new ArrayList<>();
    
            for (User user : userList) {
                invitations = invitationRepository.findAllUserDailyInvitationByCreated(user.getId().toString(), LocalDate.now().minusDays(1));
                invitationList.addAll(invitations);
            }
    
            model.addAttribute("invitations", invitationList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "common/agenda/agenda-bak";
    }
    
}