package com.softhaxi.marves.core.restful.chat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.chatting.Chat;
import com.softhaxi.marves.core.domain.chatting.ChatRoom;
import com.softhaxi.marves.core.domain.chatting.ChatRoomMember;
import com.softhaxi.marves.core.domain.chatting.ChatStatus;
import com.softhaxi.marves.core.domain.messaging.MessageStatus;
import com.softhaxi.marves.core.model.request.ChatRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.chat.ChatRepository;
import com.softhaxi.marves.core.repository.chat.ChatRoomMemberRepository;
import com.softhaxi.marves.core.repository.chat.ChatRoomRepository;
import com.softhaxi.marves.core.repository.chat.ChatStatusRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatRestful {

    private static final Logger logger = LoggerFactory.getLogger(ChatRestful.class);

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ChatRoomRepository chatRoomRepo;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepo;

    @Autowired
    private ChatRepository chatRepo;

    @Autowired
    private ChatStatusRepository chatStatusRepo;

    @PostMapping()
    public ResponseEntity<?> post(@RequestBody ChatRequest payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User sender = new User().id(UUID.fromString(auth.getPrincipal().toString()));

        ChatRoom chatRoom = null;
        if(payload.getChatRoom() == null) {
            if(payload.getRecipient() != null) {
                sender = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(sender);
                User recipient = userRepo.findById(UUID.fromString(payload.getRecipient())).orElse(null);
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
        Chat chat = new Chat()
                        .chatRoom(chatRoom)
                        .sender(sender)
                        .content(payload.getContent().trim())
                        .dateTime(ZonedDateTime.ofInstant(payload.getDateTime().toInstant(), ZoneId.systemDefault()));
        chatRepo.save(chat);
        if(payload.getChatRoom() == null)
            chatStatusRepo.save(new ChatStatus(chat, new User().id(UUID.fromString(payload.getRecipient())), false, false));
        else {
            Collection<ChatStatus> statuses = new ArrayList<>();
            for(ChatRoomMember member : chatRoom.getMembers()) {
                if(!member.getUser().equals(sender)) {
                    statuses.add(new ChatStatus(chat, member.getUser(), false, false));
                }
            }
            chatStatusRepo.saveAll(statuses);
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

        Collection<ChatRoom> rooms = chatRoomRepo.findAllByUser(user);
        for(ChatRoom room: rooms) {
            String[] names = room.getName().split("\\|");
            if(names[0].equalsIgnoreCase(user.getEmail())) 
                room.setName(names[1]);
            else
                room.setName(names[0]);

            if(room.getChats() != null && !room.getChats().isEmpty()) {
                Chat chat = room.getChats().stream().reduce((a, b) -> b).orElse(null);
                if(chat != null)
                    room.setLastMessage(chat.getContent());
            }
        }

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
                                //status.setRead(true);
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
                        //status.setRead(true);
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
