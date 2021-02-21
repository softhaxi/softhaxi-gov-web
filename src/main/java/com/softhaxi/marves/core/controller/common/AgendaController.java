package com.softhaxi.marves.core.controller.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Invitation;
import com.softhaxi.marves.core.domain.employee.InvitationMember;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.employee.InvitationMemberRepository;
import com.softhaxi.marves.core.repository.employee.InvitationRepository;
import com.softhaxi.marves.core.service.AgendaService;
import com.softhaxi.marves.core.service.account.UserService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * AgendaController
 */
@Controller
public class AgendaController {

    Logger logger = LoggerFactory.getLogger(AgendaController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private InvitationMemberRepository invitationMemberRepo;

    @Autowired
    private AgendaService agendaService;

    @GetMapping("/agenda")
    public String getAgenda(Model model, @RequestParam("date") Optional<Date> date) {
        Date startDate = date.orElse(new Date());
        
        LocalDate localDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Collection<User> userList = userService.findAllIncludeAdmin();

        Collection<Invitation> invitations = new ArrayList<>();
        List<Invitation> invitationList = new ArrayList<>();

        JSONArray jsonArray = new JSONArray();

        for (User user : userList) {
            invitations = invitationRepository.findAllUserDailyInvitationByCreated(user.getId().toString(), localDate);
            
            agendaService.setMember(invitations);

            for (Iterator<Invitation> iterator = invitations.iterator(); iterator.hasNext();) {
                jsonArray.put(iterator.next());
            }
            invitationList.addAll(invitations);
        }

        model.addAttribute("invitationObject", jsonArray.toList());

        // model.addAttribute("invitations", invitationList);

        return "agenda/index";
    }

    @PostMapping("/agenda/search-employee")
    public String searchEmployee(Model model, @ModelAttribute("name") Optional<String> name) {
        String strName = name.orElse("");
        try {
            List<User> userList = userService.findUserByUsernameLike(strName);

            Collection<Invitation> invitations = new ArrayList<>();
            Collection<Invitation> invitationList = new ArrayList<>();

            for (User user : userList) {
                invitations = invitationRepository.findAllUserDailyInvitationByCreated(user.getId().toString(),
                        LocalDate.now().minusDays(1));
                invitationList.addAll(invitations);
            }

            model.addAttribute("invitations", invitationList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "common/agenda/agenda-bak";
    }

    @PostMapping("/agenda/save")
    public String saveAgenda(Model model, @ModelAttribute("code") Optional<String> code, 
            @ModelAttribute("title") Optional<String> title,
            @ModelAttribute("description") Optional<String> description,
            @ModelAttribute("startDate") Optional<String> startDate,
            @ModelAttribute("endDate") Optional<String> endDate,
            @ModelAttribute("startTime") Optional<String> startTime,
            @ModelAttribute("endTime") Optional<String> endTime,
            @ModelAttribute("location") Optional<String> location,
            @ModelAttribute("email") Optional<String> email,
            @ModelAttribute("category") Optional<String> category) {
        
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
            LocalDateTime startDateTime = LocalDateTime.parse(startDate.get()+" " + startTime.get() + ":00", formatter);
            LocalDateTime endDateTime = LocalDateTime.parse(endDate.get()+" " + endTime.get() + ":00", formatter);
                
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));
            try {
              
                Invitation invitation = new Invitation()
                .code(code.orElse(""))
                .title(title.orElse(""))
                .description(title.get() != null ? description.get() : title.get())
                .location(location.get())
                .startDate(new SimpleDateFormat("MM/dd/yyyy").parse(startDate.get()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .endDate(new SimpleDateFormat("MM/dd/yyyy").parse(endDate.get()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .startTime(startDateTime.atZone(ZoneId.systemDefault()))
                .endTime(endDateTime.atZone(ZoneId.systemDefault()))
                .category(category.orElse(""))
                .user(user);
                invitationRepository.save(invitation);
                
                List<InvitationMember> invitees = new ArrayList<>();
                
                List<String> inviteeEmails =  new LinkedList<>(Arrays.asList(email.orElse("").split(",")));
                logger.debug("inviteeEmails: " + inviteeEmails);
                //List<String> inviteeEmails =  new LinkedList<>(Arrays.asList(email.orElse("")));
                invitees.add(new InvitationMember()
                    .invitation(invitation)
                    .user(user)
                    .organizer(true)
                    .response("ACCEPT"));
                if(inviteeEmails != null && !inviteeEmails.isEmpty()) {

                    for(String strEmail: inviteeEmails) {
                        logger.debug("strEmail: " + strEmail);
                        User invitee = userRepository.findByUsernameOrEmailIgnoreCase(strEmail).orElse(null);
                        if(invitee == null) {
                            invitee = new User()
                                .email(strEmail)
                                .username(strEmail.substring(0, strEmail.indexOf("@")).toUpperCase());
                            invitee.setIsLDAPUser(true);
                            userRepository.save(invitee);
                        }
                        if(!user.equals(invitee)) {
                            invitees.add(new InvitationMember()
                                .invitation(invitation)
                                .user(invitee));
                        }
                    }
                }
                invitationMemberRepo.saveAll(invitees);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        
        return "redirect:/agenda";
    }

    @GetMapping("/agenda/getuserbyemail")
    public @ResponseBody String getUserByEmail(Model model, @RequestParam("email") Optional<String> email) {
            String strEmail = email.orElse("");
            logger.debug(strEmail);
            List<User> users = userService.findUserByEmailLike(strEmail);
            List<Map<?, ?>> userList = new LinkedList<>();
            
            String json = "";
            
            try {
                Map<String, String> userMap = new HashMap<>();
                for (User user : users) {
                    userMap = new HashMap<>();
                    userMap.put("value", user.getId().toString());
                    userMap.put("email", user.getEmail());
                    userList.add(userMap);
                }
                Gson gson = new Gson();
                json = gson.toJson(userList);
                
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
    
            return json;
        }
    
}