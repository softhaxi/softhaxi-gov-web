package com.softhaxi.marves.core.restful.chat;

import static java.util.Map.entry;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.Profile;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.chatting.Chat;
import com.softhaxi.marves.core.domain.chatting.ChatRoom;
import com.softhaxi.marves.core.domain.chatting.ChatRoomMember;
import com.softhaxi.marves.core.domain.chatting.ChatStatus;
import com.softhaxi.marves.core.domain.messaging.MessageStatus;
import com.softhaxi.marves.core.model.request.ChatRequest;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//https://github.com/amrkhaledccd/One-to-One-WebSockets-Chat
@RestController
@RequestMapping("/api/v1/chat")
public class ChatRestful {

    private static final Logger logger = LoggerFactory.getLogger(ChatRestful.class);

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ProfileRepository profileRepo;

    @Autowired
    private ChatRoomRepository chatRoomRepo;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepo;

    @Autowired
    private ChatRepository chatRepo;

    @Autowired
    private ChatStatusRepository chatStatusRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    @PostMapping()
    public ResponseEntity<?> post(@RequestBody ChatRequest payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final User sender = userRepo.findById(UUID.fromString(auth.getPrincipal().toString()))
            .orElse(new User().id(UUID.fromString(auth.getPrincipal().toString())));

        ChatRoom chatRoom = null;
        User recipient = null;
        if(payload.getChatRoom() == null) {
            if(payload.getRecipient() != null) {
                recipient = userRepo.findById(UUID.fromString(payload.getRecipient())).orElse(null);
                if(recipient == null) {
                    return new ResponseEntity<>(
                        new ErrorResponse(
                            HttpStatus.NOT_FOUND.value(),
                            HttpStatus.NOT_FOUND.getReasonPhrase(),
                            "item.not.found"
                        ), HttpStatus.NOT_FOUND
                    );
                }

                chatRoom = chatRoomRepo.findOnePrivateBy2User(sender, recipient).orElse(null);
                if(chatRoom == null) {
                    chatRoom = new ChatRoom().name(String.format("%s|%s", sender.getEmail(), recipient.getEmail()));
                    chatRoomRepo.save(chatRoom);
                    
                    chatRoomMemberRepo.saveAll(List.of(
                        new ChatRoomMember(chatRoom, sender),
                        new ChatRoomMember(chatRoom, recipient)
                    ));
                }
            }
        } else {
            chatRoom = chatRoomRepo.findOneByIdAndUser(UUID.fromString(payload.getChatRoom()), sender).orElse(null);
        }
        if(chatRoom == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(),
                    "item.not.found"
                ), HttpStatus.NOT_FOUND
            );
        }
        if(recipient == null) {
            ChatRoomMember member = chatRoom.getMembers().stream()
                .filter((item) -> !item.getUser().equals(sender))
                .findFirst().orElse(null);
            if(member != null) {
                recipient = member.getUser();
            }
        }
        Chat chat = new Chat()
                        .chatRoom(chatRoom)
                        .sender(sender)
                        .content(payload.getContent().trim())
                        .dateTime(ZonedDateTime.ofInstant(payload.getDateTime().toInstant(), ZoneId.systemDefault()));
        chatRepo.save(chat);
        Collection<ChatStatus> statuses = new ArrayList<>();
        if(payload.getChatRoom() == null)
            statuses.add(new ChatStatus(chat, recipient, false, false));
        else {
            chatRoom.getMembers().forEach((member) -> {
                if(!member.getUser().equals(sender)) {
                    statuses.add(new ChatStatus(chat, member.getUser(), false, false));
                }
            });
            //chatStatusRepo.saveAll(statuses);
        }

        //messagingTemplate.convertAndSendToUser("test", "/message", chat);
        //logger.info(String.format("%s.%s", chatRoom.getId().toString(), recipient.getEmail()));
        messagingTemplate.convertAndSendToUser(
            String.format("%s.%s", chatRoom.getId().toString(), recipient.getEmail()), 
            "/queue/message", chat.getId().toString());

        messagingTemplate.convertAndSendToUser(
            recipient.getEmail(), 
            "/queue/message", chat.getId().toString());

        if(recipient.getOneSignalId() != null && !recipient.getOneSignalId().isEmpty()) {
            Map<String, Object> body = new HashMap<>(Map.ofEntries(
                entry("headings", Map.of("en", sender.getProfile().getFullName())),
                entry("contents", Map.of("en", chat.getContent())),
                entry("data", Map.of("deepLink", "core://marves.dev/chat", 
                    "view", "detail", 
                    "refId", chat.getChatRoom().getId().toString())),
                entry("include_player_ids", Arrays.asList(recipient.getOneSignalId())),
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

    @GetMapping("/room")
    public ResponseEntity<?> rooms() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID id = UUID.fromString(auth.getPrincipal().toString());
        User user = userRepo.findById(id).orElse(new User().id(id));

        List<ChatRoom> rooms = new LinkedList<>(chatRoomRepo.findAllByUser(user));
        for(ChatRoom room: rooms) {
            Profile profile = null;
            ChatRoomMember member = room.getMembers().stream()
                .filter((item) -> !item.getUser().equals(user))
                .findFirst().orElse(null);
            if(member != null) {
                profile = profileRepo.findByUser(member.getUser()).orElse(null);
            }
            // 
            //room.setName(recipient.getProfile().getFullName());
            if(profile != null) {
                room.setRecipient(member.getUser().getEmail());
                // logger.info(recipient.toString());
                room.setName(profile.getFullName());
            } else {
                String[] names = room.getName().split("\\|");
                if(names[0].equalsIgnoreCase(user.getEmail())) 
                    room.setName(names[1]);
                else
                    room.setName(names[0]);
            }

            // long unread = room.getChats().stream().filter((item) -> {
            //     //System.out.println(item.isRead());
            //     return !item.isRead() && !item.getUser().equals(user);
            // }).count();
            // room.setUnreadChat(unread);

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

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                rooms
            ), 
            HttpStatus.OK
        );
    }

    @GetMapping("/room/{id}/messages")
    public ResponseEntity<?> messages(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        ChatRoom chatRoom = chatRoomRepo.findOneByIdAndUser(UUID.fromString(id), user).orElse(null);
        if(chatRoom == null) {
            return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), 
                    HttpStatus.NOT_FOUND.getReasonPhrase(), 
                    "item.not.found"
                ),
                HttpStatus.NOT_FOUND
            );
        }

        Collection<Chat> chats = chatRepo.findAllByChatRoom(chatRoom);
        Collection<ChatStatus> statuses = new ArrayList<>();
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
        if(!statuses.isEmpty())
            chatStatusRepo.saveAll(statuses);

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                chats
            ), 
            HttpStatus.OK
        );
    }

    @GetMapping("/undelivered")
    public ResponseEntity<?> undeliveredList() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        Collection<Chat> chats = chatRepo.findAllUndeliveredByUser(user);
        logger.info("[undeliveredList] Count..." + chats.size());
        
        Collection<ChatStatus> statuses = new ArrayList<>();
        for(Chat chat: chats) {
            ChatStatus status = chatStatusRepo.findOneUndeliveredChatByUser(chat, user).orElse(null);
            if(status == null) 
                status = new ChatStatus(chat,
                    user, true, false
                );
            else 
                status.setDelivered(true);
            
            statuses.add(status);
        }
        if(!statuses.isEmpty())
            chatStatusRepo.saveAll(statuses);
        
        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                chats
                // Map.of(
                //     "id", "1", 
                //     "content", "Testing notification from server", 
                //     "dateTime", LocalDateTime.now()
                // )
            ),
            HttpStatus.OK
        );
    }
}
