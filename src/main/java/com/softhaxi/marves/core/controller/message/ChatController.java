package com.softhaxi.marves.core.controller.message;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;

import com.google.gson.Gson;
import com.softhaxi.marves.core.domain.account.Profile;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.chatting.Chat;
import com.softhaxi.marves.core.domain.chatting.ChatRoom;
import com.softhaxi.marves.core.domain.chatting.ChatRoomMember;
import com.softhaxi.marves.core.domain.chatting.ChatStatus;
import com.softhaxi.marves.core.domain.messaging.MessageStatus;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.account.ProfileRepository;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.chat.ChatRepository;
import com.softhaxi.marves.core.repository.chat.ChatRoomMemberRepository;
import com.softhaxi.marves.core.repository.chat.ChatRoomRepository;
import com.softhaxi.marves.core.repository.chat.ChatStatusRepository;
import com.softhaxi.marves.core.service.message.MessageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

    private ChatRoomMemberRepository chatRoomMemberRepo;

    @Autowired
    private ChatRepository chatRepo;

    @Autowired
    private ChatStatusRepository chatStatusRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ProfileRepository profileRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

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

    @PostMapping()
    public ResponseEntity<?> post(Model model, @RequestParam(name = "id", required = false) String id,
        @RequestParam(name = "recipient", required = false) String recipient,
        @RequestParam(name = "message") String message) {
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
        User senderUser = userRepo.findById(UUID.fromString(userId)).orElse(new User().id(UUID.fromString(userId)));
        
        ChatRoom chatRoom = null;
        User recipientUser = null;
        if(id == null) {
            if(recipient != null) {
                recipientUser = userRepo.findById(UUID.fromString(recipient.trim())).orElseThrow();

                chatRoom = chatRoomRepo.findOnePrivateBy2User(senderUser, recipientUser).orElse(null);
                if(chatRoom == null) {
                    chatRoom = new ChatRoom().name(String.format("%s|%s", senderUser.getEmail(), recipientUser.getEmail()));
                    chatRoomRepo.save(chatRoom);
                    
                    chatRoomMemberRepo.saveAll(List.of(
                        new ChatRoomMember(chatRoom, senderUser),
                        new ChatRoomMember(chatRoom, recipientUser)
                    ));
                }
            }
        } else {
            chatRoom = chatRoomRepo.findOneByIdAndUser(UUID.fromString(id), senderUser).orElseThrow();
        }
        if(recipientUser == null) {
            ChatRoomMember member = chatRoom.getMembers().stream()
                .filter((item) -> !item.getUser().equals(senderUser))
                .findFirst().orElse(null);
            if(member != null) {
                recipientUser = member.getUser();
            }
        }
        Chat chat = new Chat()
                        .chatRoom(chatRoom)
                        .sender(senderUser)
                        .content(message.trim())
                        .dateTime(ZonedDateTime.now());
        chatRepo.save(chat);
        Collection<ChatStatus> statuses = new LinkedList<>();
        if(id == null)
            statuses.add(new ChatStatus(chat, recipientUser, false, false));
        else {
            chatRoom.getMembers().forEach((member) -> {
                if(!member.getUser().equals(senderUser)) {
                    statuses.add(new ChatStatus(chat, member.getUser(), false, false));
                }
            });
        }
        messagingTemplate.convertAndSendToUser(
            String.format("%s.%s", chatRoom.getId().toString(), recipientUser.getEmail()), 
            "/queue/message", chat.getId().toString());

        messagingTemplate.convertAndSendToUser(
            recipientUser.getEmail(), 
            "/queue/message", String.format("%s.%s", chat.getChatRoom().getId().toString(), chat.getId().toString()));

        if(recipientUser.getOneSignalId() != null && !recipientUser.getOneSignalId().isEmpty()) {
            Map<String, Object> body = new HashMap<>(Map.ofEntries(
                entry("headings", Map.of("en", "Administrator")),
                entry("contents", Map.of("en", chat.getContent())),
                entry("data", Map.of("deepLink", "core://marves.dev/chat", 
                    "view", "detail", 
                    "refId", chat.getChatRoom().getId().toString())),
                entry("include_player_ids", Arrays.asList(recipientUser.getOneSignalId())),
                entry("small_icon", "ic_stat_marves"),
                entry("android_channel_id", "066ee9a7-090b-4a42-b084-0dcbbeb7f158"),
                entry("android_accent_color", "FF19A472"),
                entry("android_group", chat.getChatRoom().getId().toString())
            ));
            messageService.sendPushNotification(chat, statuses, body);
        }

        chat.setMyself(true);
        return new ResponseEntity<>(
            new GeneralResponse(
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
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                chat
            ), 
            HttpStatus.OK
        );
    }
}
