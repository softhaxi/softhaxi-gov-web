package com.softhaxi.marves.core.controller.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.softhaxi.marves.core.util.JSONUtil;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

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
    
    @Value("${app.upload.path}")
	private String pathLocation;

    @GetMapping("/agenda")
    public String getAgenda(Model model, @RequestParam("date") Optional<Date> date,
    @RequestParam("id") Optional<String> id, @RequestParam("search") Optional<String> name, @RequestParam("category") Optional<List<String>> category) {
        Date startDate = date.orElse(new Date());
        String strId = id.orElse("");
        String strName = name.orElse("");
        logger.debug("strId: " + strId);
        List<String> categoryList = category.isEmpty()?new ArrayList<>():category.get();
        
        LocalDate localDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Collection<User> userList = new ArrayList<>();
        if(!strId.isEmpty()){
            User user = userRepository.getOne(UUID.fromString(strId));
            userList.add(user);
        }else{
            userList = userService.findAllIncludeAdmin();
        }

        Collection<Invitation> invitations = new ArrayList<>();
        List<Invitation> invitationList = new ArrayList<>();

        JSONArray jsonArray = new JSONArray();

        for (User user : userList) {
            if(categoryList.isEmpty())
                invitations = invitationRepository.findAllUserDailyInvitationByCreated(!strId.equals("")?strId:user.getId().toString(), localDate);
            else
                invitations = invitationRepository.findAllUserDailyInvitationByCategory(!strId.equals("")?strId:user.getId().toString(), categoryList, localDate);

            
            agendaService.setMember(invitations);

            for (Iterator<Invitation> iterator = invitations.iterator(); iterator.hasNext();) {
                Invitation invitation = iterator.next();
                invitation.setDescription(JSONUtil.escape(invitation.getDescription()));
                invitation.setTitle(JSONUtil.escape(invitation.getTitle()));
                jsonArray.put(invitation);
            }
            
            invitationList.addAll(invitations);
        }
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        model.addAttribute("startTime", hour + ":" +minute);
        model.addAttribute("endTime", hour+1+":" +minute);
        model.addAttribute("category", categoryList);
        model.addAttribute("name", strName);
        model.addAttribute("id", strId);
        
        model.addAttribute("invitationObject", jsonArray.toList());

        return "agenda/index";
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
            @ModelAttribute("category") Optional<String> category,
            @RequestParam("file") MultipartFile file) {
        
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
            LocalDateTime startDateTime = LocalDateTime.parse(startDate.get()+" " + startTime.get() + ":00", formatter);
            LocalDateTime endDateTime = LocalDateTime.parse(endDate.get()+" " + endTime.get() + ":00", formatter);
            

            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String userId = null;
            if(principal != null) {
                if(principal instanceof LdapUserDetailsImpl) {
                    LdapUserDetails ldapUser = (LdapUserDetailsImpl) principal;
                    userId = ldapUser.getUsername();
                } else {
                    userId = principal.toString();
                }
            } else {
                userId = principal.toString();
            }
            User user = userRepository.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));
        

            try {
              

                String fileFormat = FilenameUtils.getExtension(file.getOriginalFilename());
                String newFileName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

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
                .user(user)
                .fileName(newFileName+"."+fileFormat)
                .attachement(pathLocation+"/"+newFileName+"."+fileFormat);
                invitationRepository.save(invitation);
                

                Path path = Paths.get("/asset"+pathLocation);
                
                if (!file.isEmpty()) {
                    Files.copy(file.getInputStream(), path.resolve(newFileName+"."+fileFormat));
                }
                
                
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
            } catch (IOException ioe){
                ioe.printStackTrace();
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