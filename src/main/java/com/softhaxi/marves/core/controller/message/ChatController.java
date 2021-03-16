package com.softhaxi.marves.core.controller.message;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.softhaxi.marves.core.domain.account.Profile;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.chatting.Chat;
import com.softhaxi.marves.core.domain.chatting.ChatRoom;
import com.softhaxi.marves.core.domain.chatting.ChatRoomMember;
import com.softhaxi.marves.core.domain.chatting.ChatStatus;
import com.softhaxi.marves.core.domain.exception.BusinessException;
import com.softhaxi.marves.core.domain.messaging.MessageStatus;
import com.softhaxi.marves.core.domain.request.ChatRequest;
import com.softhaxi.marves.core.domain.response.ErrorResponse;
import com.softhaxi.marves.core.domain.response.SuccessResponse;
import com.softhaxi.marves.core.repository.account.ProfileRepository;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.chat.ChatRepository;
import com.softhaxi.marves.core.repository.chat.ChatRoomRepository;
import com.softhaxi.marves.core.repository.chat.ChatStatusRepository;
import com.softhaxi.marves.core.service.message.ChatService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatRoomRepository chatRoomRepo;
    @Autowired
    private ChatRepository chatRepo;
    @Autowired
    private ChatStatusRepository chatStatusRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private ProfileRepository profileRepo;
    @Autowired
    private ChatService chatService;

    @GetMapping()
    public String index(Model model) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // logger.debug("[index] User principal ..." + principal.toString());
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
        User user = userRepo.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));
        
        model.addAttribute("email", user.getEmail());
        return "chat/index";
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@ModelAttribute ChatRequest request) {
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
        User user = userRepo.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));
        if(request.getFile() != null && !request.getFile().isEmpty()) {
            
            request.setContentType(request.getFile().getContentType());
        } else {
            request.setContentType(MediaType.TEXT_PLAIN_VALUE);
        }
        request.setDateTime(new Date());
        logger.debug("[send] Content type..." + request.getContentType());

        Chat chat;
        try {
            chat = chatService.send(user, request);
        } catch (BusinessException e) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    e.getMessage()),
                    HttpStatus.NOT_FOUND
            );
        }
        chat.setMyself(true);
        return new ResponseEntity<>(
            new SuccessResponse(
                HttpStatus.CREATED.value(),
                HttpStatus.CREATED.getReasonPhrase(),
                chat
            ),
            HttpStatus.CREATED
        );
    }

    @GetMapping("/rooms")
    public String rooms(Model model, @RequestParam(name = "id", required = false) String id) {
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
        User user = userRepo.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));
        
        List<ChatRoom> rooms = new LinkedList<>(chatRoomRepo.findAllByUser(user));
        for(ChatRoom room: rooms) {
            Profile profile = null;
            ChatRoomMember member = room.getMembers().stream()
                .filter((item) -> !item.getUser().equals(user))
                .findFirst().orElse(null);
            if(member != null) {
                profile = profileRepo.findByUser(member.getUser()).orElse(null);
            }

            if(member.getUser().getEmployee() != null) {
                room.setProfilePicture(member.getUser().getEmployee().getPictureUrl());
            }
            
            if(profile != null) {
                room.setRecipient(member.getUser().getEmail());
                room.setName(profile.getFullName());
            } else {
                String[] names = room.getName().split("\\|");
                if(names[0].equalsIgnoreCase(user.getEmail())) 
                    room.setName(names[1]);
                else
                    room.setName(names[0]);
            }

            if(room.getChats() != null && !room.getChats().isEmpty()) {
                Chat chat = room.getChats().stream().reduce((a, b) -> b).orElse(null);
                if(chat != null) {
                    room.setLatestChat(chat);
                }
            }
        }
        Collections.sort(rooms, new Comparator<ChatRoom>() {
            @Override
            public int compare(ChatRoom o1, ChatRoom o2) {
                return o2.getLatestChat().getDateTime().compareTo(o1.getLatestChat().getDateTime());
            }
        });

        model.addAttribute("data", rooms);

        return "chat/rooms";
    }

    @GetMapping("/chats")
    public String chats(Model model, 
        @RequestParam(name = "id", required = false) String id,
        @RequestParam(name = "recipient", required = false) String recipient) {
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
        User user = userRepo.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));
        
        ChatRoom room = chatRoomRepo.findOneByIdAndUser(UUID.fromString(id), user).orElse(null);

        if(room == null) {
            return startChat(model, room); 
        }

        Profile profile = null;
        ChatRoomMember member = room.getMembers().stream()
            .filter((item) -> !item.getUser().equals(user))
            .findFirst().orElse(null);
        if(member != null) {
            profile = profileRepo.findByUser(member.getUser()).orElse(null);
        }
        
        if(member.getUser().getEmployee() != null) {
            room.setProfilePicture(member.getUser().getEmployee().getPictureUrl());
        }
        
        if(profile != null) {
            room.setRecipient(member.getUser().getEmail());
            room.setName(profile.getFullName());
        } else {
            String[] names = room.getName().split("\\|");
            if(names[0].equalsIgnoreCase(user.getEmail())) 
                room.setName(names[1]);
            else
                room.setName(names[0]);
        }

        Collection<Chat> chats = chatRepo.findAllByChatRoom(room);
        Collection<ChatStatus> statuses = new LinkedList<>();
        for(Chat chat: chats) {
            if(!chat.getSender().equals(user)) {
                chat.setMyself(false);
                if(chat.getStatuses() != null && !chat.getStatuses().isEmpty()) {
                    for(MessageStatus status: chat.getStatuses()) {
                        if(status.getUser().equals(user)) {
                            if(!status.isDelivered()) {
                                status.setDelivered(true);
                                status.setRead(true);
                                statuses.add((ChatStatus) status);
                            }
                            chat.setDelivered(true);
                            break;
                        }
                    }
                } else {
                    ChatStatus status = chatStatusRepo.findOneChatStatusByUser(chat, user).orElse(null);
                    if(status == null) 
                        statuses.add(new ChatStatus(chat, user, true, false));
                    else if(!status.isDelivered()) {
                        status.setDelivered(true);
                        status.setRead(true);
                        statuses.add(status);
                    }
                    chat.setDelivered(true);
                }
            } else {
                chat.setMyself(true);
            }
        }
        model.addAttribute("room", room);
        model.addAttribute("data", chats);
        return "chat/chats";
    }

    public String startChat(Model model, ChatRoom room) {
        model.addAttribute("room", room);
        return "chat/chats";
    }

    @GetMapping("/room/search")
    public @ResponseBody String search(Model model, 
        @RequestParam(name = "name" , required=false, defaultValue = "") String name) {

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
        User user = userRepo.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));
        
        List<ChatRoom> rooms = new LinkedList<>(chatRoomRepo.findAllByUser(user));
        List<Map<?, ?>> filteredRooms = new LinkedList<>();
        Map<String, String> roomMap = null;
        for(ChatRoom room: rooms) {
            Profile profile = null;
            ChatRoomMember member = room.getMembers().stream()
                .filter((item) -> !item.getUser().equals(user))
                .findFirst().orElse(null);
            if(member != null) {
                profile = profileRepo.findByUser(member.getUser()).orElse(null);
            }
            
            if(profile != null && profile.getFullName().toLowerCase().contains(name)) {
                roomMap = new HashMap<>();
                roomMap.put("value", room.getId().toString());
                roomMap.put("label", profile.getFullName());
                filteredRooms.add(roomMap);
            } 
        }
        Gson gson = new Gson();
        String json = gson.toJson(filteredRooms);
        

        return json;
    }

    @GetMapping("/room/message/{id}")
    public ResponseEntity<?> roomMessage(@PathVariable String id) {
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
        User user = userRepo.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));
        
        Collection<ChatRoom> chatRooms = chatRoomRepo.findAllByUser(user);

        Chat chat = chatRepo.findById(UUID.fromString(id)).orElse(null);
        if(chat == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }

        ChatRoom room = chatRooms.stream()
            .filter(item -> item.equals(chat.getChatRoom()))
            .findFirst().orElse(null);
        if(room == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }

        return new ResponseEntity<>(
            new SuccessResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                chat
            ), 
            HttpStatus.OK
        );
    }
}
