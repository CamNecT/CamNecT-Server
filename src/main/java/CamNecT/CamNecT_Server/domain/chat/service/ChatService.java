package CamNecT.CamNecT_Server.domain.chat.service;


import CamNecT.CamNecT_Server.domain.chat.dto.message.ChatMessageResponseDto;
import CamNecT.CamNecT_Server.domain.chat.dto.message.ChatMessageSendRequestDto;
import CamNecT.CamNecT_Server.domain.chat.dto.message.ChatReadEvent;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomListDetailDto;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomListUpdateDto;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomWithDetailDto;
import CamNecT.CamNecT_Server.domain.chat.model.Chat;
import CamNecT.CamNecT_Server.domain.chat.model.ChatRequest;
import CamNecT.CamNecT_Server.domain.chat.model.ChatRoom;
import CamNecT.CamNecT_Server.domain.chat.repository.ChatRepository;
import CamNecT.CamNecT_Server.domain.chat.repository.ChatRequestRepository;
import CamNecT.CamNecT_Server.domain.chat.repository.ChatRoomRepository;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.domain.profile.components.majors.model.Majors;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import CamNecT.CamNecT_Server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.CamNecT_Server.global.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRequestRepository chatRequestRepository;
    private final TagRepository tagRepository;
    private final UserProfileRepository userProfileRepository;
    private final MajorRepository majorRepository;

    /*
      1. 커피챗 요청 보내기
      Requester -> Receiver임.
     */
    @Transactional
    public Long sendCoffeeChatRequest(Long requesterId, Long receiverId, List<Long> tagIds, String content) {
        Users requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("요청자 정보를 찾을 수 없습니다."));
        Users receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("수신자 정보를 찾을 수 없습니다."));

        List<Tag> tags = tagRepository.findAllById(tagIds);

        if (tags.size() != tagIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 태그가 포함되어 있습니다.");
        }

        ChatRequest request = ChatRequest.builder()
                .requester(requester)
                .receiver(receiver)
                .requestInterest(tags)
                .content(content)
                .build();

        return chatRequestRepository.save(request).getId();
    }


    /*
      2. 요청 수락/거절 처리
      Receiver가 수락(ACCEPTED)하면 -> 채팅방(ChatRoom)이 생성
     */
    @Transactional
    public void respondToRequest(Long requestId, Long userId, boolean isAccepted) {
        ChatRequest request = chatRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("해당 요청이 존재하지 않습니다."));

        // 본인 요청인지 검증
        if (!request.getReceiver().getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 요청만 처리할 수 있습니다.");
        }

        if (isAccepted) {
            request.accept();
            createChatRoom(request);
        } else {
            request.reject();
        }
    }

    /*
     2-1. 채팅방 생성 (수락 시 자동 호출)
     */
    private void createChatRoom(ChatRequest request) {
        ChatRoom chatRoom = ChatRoom.createRoom(request, request.getRequester(), request.getReceiver());
        chatRoomRepository.save(chatRoom);
    }

    /*
      3. 채팅 메시지 보내기
      채팅방 Id랑 Sender(보내는 사람) ID만 있으면 됨. 시간 갱신도 해줌.
     */
/*    @Transactional
    public Chat sendMessage(Long roomId, Long senderId, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));
        Users sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        Users receiver = room.getRequester().getUserId().equals(senderId) ? room.getReceiver() : room.getRequester();

        Chat chat = Chat.createChat(room, sender, receiver, content);
        Chat savedChat = chatRepository.save(chat);

        room.updateLastMessageTime();

        return savedChat;
    }*/

    /*
      4. 채팅 내역 불러오기 (+ 읽음 처리도 같이)
      방에 입장하는 순간 실행됨
     */
/*    @Transactional
    public List<Chat> getChatHistory(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        List<Chat> chatHistory = chatRepository.findAllByRoomId(roomId);

        chatRepository.bulkReadMessages(roomId, userId);

        return chatHistory;
    }*/

    @Transactional
    public List<ChatMessageResponseDto> getChatHistory(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        Users opponent = (room.getRequester().getUserId().equals(userId)) ? room.getReceiver() : room.getRequester();

//        chatRepository.bulkReadMessages(roomId, userId);
//        System.out.println("bulkRead 함");
        markAllAsRead(roomId, opponent);

        List<Chat> chatHistory = chatRepository.findAllByRoomId(roomId);

        return chatHistory.stream()
                .map(ChatMessageResponseDto::toDto)
                .toList();
    }

/*
    @Transactional
    public ChatRoom getRoomWithDetails(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 없어요"));

        room.getRequester().getName();
        room.getReceiver().getName();

        room.getRequest().getRequestInterests().forEach(Tag::getName);

        this.getChatHistory(roomId, userId);

        return room;
    }
*/

    @Transactional
    public ChatRoomWithDetailDto getRoomWithDetails(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findByUserIdWithDetails(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 없어요"));

        Users me = (room.getRequester().getUserId().equals(userId)) ? room.getRequester() : room.getReceiver();
        Users opponent = (room.getRequester().getUserId().equals(userId)) ? room.getReceiver() : room.getRequester();

        UserProfile opProfile = userProfileRepository.findByUserId(opponent.getUserId())
                .orElse(null);

        String majorName = "전공 미입력";
        if (opProfile != null && opProfile.getMajorId() != null) {
            majorName = majorRepository.findById(opProfile.getMajorId())
                    .map(Majors::getMajorNameKor)
                    .orElse("알 수 없는 전공");
        }

        List<ChatMessageResponseDto> chatHistory = this.getChatHistory(roomId, userId);

        return ChatRoomWithDetailDto.from(room, me, opponent, opProfile, majorName, chatHistory);
    }


    public List<ChatRoomListDetailDto> getChatRoomList(Long userId) {
        Users me = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        List<ChatRoom> myRooms = chatRoomRepository.findAllByUserIdWithBasicInfo(userId);

        if (myRooms.isEmpty()) {
            return List.of();
        }

        List<Long> opponentIds = myRooms.stream()
                .map(room -> {
                    Users opponent = room.getRequester().getUserId().equals(userId)
                            ? room.getReceiver() : room.getRequester();
                    return opponent.getUserId();
                })
                .distinct()
                .toList();

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllByUserIdIn(opponentIds)
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p)); // Key: userId, Value: Profile

        // 안 읽은 개수 조회
        Map<Long, Long> unreadCounts = chatRepository.countUnreadMessagesByRooms(myRooms, userId)
                .stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        Map<Long, String> lastMessageMap = chatRepository.findLastMessagesByRooms(myRooms)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],  // RoomId
                        row -> (String) row[1] // Message
                ));

        return myRooms.stream()
                .map(room -> {
                    Users opponent = room.getRequester().getUserId().equals(userId)
                            ? room.getReceiver() : room.getRequester();

                    UserProfile opProfile = profileMap.get(opponent.getUserId());

                    String majorName = "전공 미입력";
                    String studentYear = "";

                    if (opProfile != null) {
                        studentYear = opProfile.getYearLevel().toString();
                        if (opProfile.getMajorId() != null) {
                            majorName = majorRepository.findById(opProfile.getMajorId())
                                    .map(Majors::getMajorNameKor)
                                    .orElse("알 수 없는 전공");
                        }
                    }

                    Long count = unreadCounts.getOrDefault(room.getId(), 0L);
                    String lastMessage = lastMessageMap.getOrDefault(room.getId(), "대화 내용이 없습니다.");

                    return ChatRoomListDetailDto.of(room, me, count, majorName, studentYear, lastMessage);
                })
                .toList();
    }

    private final ChatPresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    //   기존 unread 전부 읽음 처리
    @Transactional
    public void markAllAsRead(Long roomId, Users reader) {

        List<Chat> unreadMessages = chatRepository.findUnreadMessages(roomId, reader.getUserId());

        if (unreadMessages.isEmpty()) {
            return;
        }

        unreadMessages.forEach(Chat::markAsRead);
        chatRepository.saveAll(unreadMessages);

        System.out.println("📚 읽음 처리 완료: " + unreadMessages.size() + "건 저장됨.");


        // 마지막으로 읽은 메시지 ID 추출 (없으면 0L임)
        Long lastMessageId = unreadMessages.isEmpty() ? 0L : unreadMessages.get(unreadMessages.size() - 1).getId();

        ChatReadEvent chatReadEvent = ChatReadEvent.of(
                roomId,
                lastMessageId,
                LocalDateTime.now().toString()
                // typee ->  ReadEvent 내부에서 READ로 자동 설정됨
        );

        messagingTemplate.convertAndSend(
                "/sub/chat/room/" + roomId,
                chatReadEvent
        );
    }


    /* unread count 조회 (채팅방 리스트용)

    @Transactional(readOnly = true)
    public long getUnreadCount(Long roomId, Long userId) {
        return chatRepository.countUnread(roomId, userId);
    }*/


    /* public void sendMessage(ChatMessageResponseDto dto) {

        ChatRoom room = chatRoomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        Users sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("보낸이 없음"));

        Users receiver = userRepository.findById(dto.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("받는이 없음"));

        boolean receiverPresent =
                presenceService.isPresent(room.getId(), receiver.getUserId());

        Chat chat = Chat.createChat(
                room,
                sender,
                receiver,
                dto.getMessage()
        );
        if (receiverPresent) {
            System.out.println("읽음처리");
            chat.markAsRead();
        }

        chatRepository.save(chat);

        room.updateLastMessageTime();

        ChatMessageResponseDto response = ChatMessageResponseDto.builder()
                .messageId(chat.getId())
                .roomId(room.getId())

                .senderId(sender.getUserId())
                .sender(sender.getName())

                .receiverId(receiver.getUserId())
                .receiver(receiver.getName())

                .message(chat.getContent())
                .read(chat.isRead())

                .readAt(chat.getReadAt() != null ? chat.getReadAt().toString() : null)
                .sendDate(chat.getCreatedAt().toString())
                .build();

        try {
            // 메세지 전송
            messagingTemplate.convertAndSend("/sub/chat/room/" + room.getId(), response);

            // 읽음 즉시 반영 이벤트
            if (receiverPresent) {
                messagingTemplate.convertAndSendToUser(
                        sender.getUserId().toString(),
                        "/queue/read",
                        ReadEvent.of(room.getId(), chat.getId(), chat.getReadAt().toString())
                );
            }
        } catch (Exception e) {
            System.err.println("소켓 전송 실패: " + e.getMessage());
        }
    }*/

    @Transactional
    public void sendMessage(ChatMessageSendRequestDto request) {

        ChatRoom room = chatRoomRepository.findById(request.roomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        Users sender = userRepository.findById(request.senderId())
                .orElseThrow(() -> new IllegalArgumentException("보낸이 없음"));

        Users receiver = (room.getRequester().getUserId().equals(sender.getUserId()))
                ? room.getReceiver()
                : room.getRequester();

        boolean receiverPresent = presenceService.isPresent(room.getId(), receiver.getUserId());

        Chat chat = Chat.createChat(room, sender, receiver, request.content());

        System.out.println("👀 receiverPresent = {" + receiverPresent + "}");

        if (receiverPresent) {
            chat.markAsRead();
        }

        chatRepository.save(chat);
        room.updateLastMessageTime();

        ChatMessageResponseDto response = ChatMessageResponseDto.toDto(chat);

        try {
            messagingTemplate.convertAndSend("/sub/chat/room/" + room.getId(), response);

            if (receiverPresent) {
                messagingTemplate.convertAndSendToUser(
                        sender.getUserId().toString(),
                        "/queue/read",
                        ChatReadEvent.of(room.getId(), chat.getId(), chat.getReadAt().toString())
                );
            }

            long unreadCount = chatRepository.countByRoom_IdAndReceiver_UserIdAndIsReadFalse(room.getId(), receiver.getUserId());

            long totalUnreadCount = chatRepository.countByReceiver_UserIdAndIsReadFalse(receiver.getUserId());
            String lastTime = chat.getCreatedAt().toString();

            ChatRoomListUpdateDto updateDto = ChatRoomListUpdateDto.builder()
                    .roomId(room.getId())
                    .lastMessage(chat.getContent())
                    .unreadCount(unreadCount)
                    .time(lastTime)
                    .totalUnreadCount(totalUnreadCount)
                    .build();

            messagingTemplate.convertAndSend(
                    "/sub/user/" + receiver.getUserId() + "/roomList",
                    updateDto
            );
        } catch (Exception e) {
            System.err.println("소켓 전송 실패: " + e.getMessage());
        }
    }


}