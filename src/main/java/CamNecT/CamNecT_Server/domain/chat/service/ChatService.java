package CamNecT.CamNecT_Server.domain.chat.service;


import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.CamNecT_Server.domain.activity.repository.recruitment.TeamRecruitmentRepository;
import CamNecT.CamNecT_Server.domain.chat.dto.message.ChatMessageResponseDto;
import CamNecT.CamNecT_Server.domain.chat.dto.message.ChatMessageSendRequestDto;
import CamNecT.CamNecT_Server.domain.chat.dto.message.ChatReadEvent;
import CamNecT.CamNecT_Server.domain.chat.dto.request.response.ChatRequestDetailDto;
import CamNecT.CamNecT_Server.domain.chat.dto.request.response.ChatRequestListDetailDto;
import CamNecT.CamNecT_Server.domain.chat.dto.request.response.ChatRequestListResponseDto;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomListDetailDto;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomListUpdateDto;
import CamNecT.CamNecT_Server.domain.chat.dto.room.ChatRoomWithDetailDto;
import CamNecT.CamNecT_Server.domain.chat.model.Chat;
import CamNecT.CamNecT_Server.domain.chat.model.ChatRequest;
import CamNecT.CamNecT_Server.domain.chat.model.ChatRoom;
import CamNecT.CamNecT_Server.domain.chat.repository.ChatRepository;
import CamNecT.CamNecT_Server.domain.chat.repository.ChatRequestRepository;
import CamNecT.CamNecT_Server.domain.chat.repository.ChatRoomRepository;
import CamNecT.CamNecT_Server.domain.home.dto.HomeResponse;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserTagMapRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.CoffeeChatErrorCode;
import CamNecT.CamNecT_Server.domain.profile.components.majors.model.Majors;
import CamNecT.CamNecT_Server.global.storage.service.PublicUrlIssuer;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import CamNecT.CamNecT_Server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.CamNecT_Server.global.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final UserTagMapRepository userTagMapRepository;
    private final MajorRepository majorRepository;
    private final PublicUrlIssuer publicUrlIssuer;
    private final TeamRecruitmentRepository recruitmentRepository;
    private final ChatPresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    /*
      1. 커피챗 요청 보내기
      Requester -> Receiver임.
     */
    @Transactional
    public Long sendCoffeeChatRequest(Long requesterId, Long receiverId, List<Long> tagIds, String content) {
        if (requesterId.equals(receiverId)) {
            throw new CustomException(CoffeeChatErrorCode.SELF_REQUEST_NOT_ALLOWED);
        }

        if (chatRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatus(
                requesterId, receiverId, ChatRequest.RequestStatus.WAITING)) {
            throw new CustomException(CoffeeChatErrorCode.DUPLICATE_REQUEST);
        }

        if (chatRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatus(
                requesterId, receiverId, ChatRequest.RequestStatus.ACCEPTED)
                || chatRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatus(
                receiverId, requesterId, ChatRequest.RequestStatus.ACCEPTED)) {
            throw new CustomException(CoffeeChatErrorCode.CHATROOM_ALREADY_EXISTS);
        }

        boolean isOpen = userProfileRepository.existsByUserIdAndOpenToCoffeeChatTrue(receiverId);
        if (!isOpen) {
            throw new CustomException(CoffeeChatErrorCode.RECEIVER_COFFEECHAT_DISABLED);
        }


        Users requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.REQUESTER_NOT_FOUND));
        Users receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.RECEIVER_NOT_FOUND));

        List<Tag> tags = tagRepository.findAllById(tagIds);

        if (tags.size() != tagIds.size()) {
            throw new CustomException(CoffeeChatErrorCode.TAG_NOT_FOUND);
        }

        ChatRequest request = ChatRequest.builder()
                .requester(requester)
                .receiver(receiver)
                .type(ChatRequest.RequestType.COFFEE_CHAT)
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
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.REQUEST_NOT_FOUND));

        // 본인 요청인지 검증
        if (!request.getReceiver().getUserId().equals(userId)) {
            throw new CustomException(CoffeeChatErrorCode.REQUEST_ACCESS_DENIED);
        }

        if (isAccepted) {
            request.accept();
            createChatRoom(request);
        } else {
            request.reject();
        }
    }

    @Transactional(readOnly = true)
    public ChatRequestDetailDto getChatRequestDetail(Long requestId, Long userId) {
        ChatRequest request = chatRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.REQUEST_NOT_FOUND));

        if (!request.getReceiver().getUserId().equals(userId) && !request.getRequester().getUserId().equals(userId)) {
            throw new CustomException(CoffeeChatErrorCode.REQUEST_ACCESS_DENIED);
        }

        String title = "커피챗 요청";
        if (request.getType() == ChatRequest.RequestType.TEAM_RECRUIT && request.getRecruitmentId() != null) {
            title = recruitmentRepository.findById(request.getRecruitmentId())
                    .map(TeamRecruitment::getTitle)
                    .orElse("삭제된 모집 공고입니다.");
        }

        boolean isReceiver = request.getReceiver().getUserId().equals(userId);
        Users me = isReceiver ? request.getReceiver() : request.getRequester();
        Users opponent = isReceiver ? request.getRequester() : request.getReceiver();

        UserProfile opProfile = userProfileRepository.findByUserId(opponent.getUserId())
                .orElse(null);

        String majorName = "전공 미입력";
        String profileImgUrl = "/images/default.png";
        if (opProfile != null) {
            if (opProfile.getMajorId() != null) {
                majorName = majorRepository.findById(opProfile.getMajorId())
                        .map(Majors::getMajorNameKor)
                        .orElse("알 수 없는 전공");
            }
            profileImgUrl = publicUrlIssuer.issuePublicUrl(opProfile.getProfileImageKey());
        }

        List<String> opTagNames = userTagMapRepository.findAllTagsByUserId(opponent.getUserId())
                .stream()
                .map(Tag::getName)
                .toList();

        return ChatRequestDetailDto.from(me, opponent, opProfile, request, majorName, opTagNames, profileImgUrl, title);
    }

    @Transactional(readOnly = true)
    public ChatRequestListResponseDto getChatRequestList(Long userId, ChatRequest.RequestType type) {
        List<ChatRequest> requests = chatRequestRepository
                .findAllByReceiver_UserIdAndTypeAndStatusOrderByCreatedAtDesc(
                        userId, type, ChatRequest.RequestStatus.WAITING);

        if (requests.isEmpty()) {
            return new ChatRequestListResponseDto(List.of());
        }

        Set<Long> recruitmentIds = requests.stream()
                .map(ChatRequest::getRecruitmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> recruitmentTitleMap = recruitmentIds.isEmpty() ? Map.of() :
                recruitmentRepository.findAllById(recruitmentIds).stream()
                        .collect(Collectors.toMap(TeamRecruitment::getRecruitId, TeamRecruitment::getTitle));

        List<Long> opponentIds = requests.stream()
                .map(req -> req.getRequester().getUserId())
                .distinct()
                .toList();

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllByUserIdIn(opponentIds)
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<ChatRequestListDetailDto> dtoList = requests.stream()
                .map(request -> {
                    Users opponent = request.getRequester();
                    UserProfile opProfile = profileMap.get(opponent.getUserId());

                    String majorName = "전공 미입력";
                    String profileImgUrl = "/images/default.png";
                    String title = recruitmentTitleMap.getOrDefault(request.getRecruitmentId(), "커피챗 요청");

                    if (opProfile != null) {
                        if (opProfile.getMajorId() != null) {
                            majorName = majorRepository.findById(opProfile.getMajorId())
                                    .map(Majors::getMajorNameKor)
                                    .orElse("알 수 없는 전공");
                        }

                        if (StringUtils.hasText(opProfile.getProfileImageKey())) {
                            profileImgUrl = publicUrlIssuer.issuePublicUrl(opProfile.getProfileImageKey());
                        }
                    }

                    return ChatRequestListDetailDto.from(
                            opponent,
                            opProfile,
                            request,
                            majorName,
                            profileImgUrl,
                            title
                    );
                })
                .toList();

        return new ChatRequestListResponseDto(dtoList);
    }

    /*
     2-1. 채팅방 생성 (수락 시 자동 호출)
     */
    private void createChatRoom(ChatRequest request) {
        ChatRoom chatRoom = ChatRoom.createRoom(request, request.getRequester(), request.getReceiver());
        chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public List<ChatMessageResponseDto> getChatHistory(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.CHATROOM_NOT_FOUND));

        Users opponent = (room.getRequester().getUserId().equals(userId)) ? room.getReceiver() : room.getRequester();

        markAllAsRead(roomId, opponent);

        List<Chat> chatHistory = chatRepository.findAllByRoomId(roomId);

        return chatHistory.stream()
                .map(ChatMessageResponseDto::toDto)
                .toList();
    }


    @Transactional
    public ChatRoomWithDetailDto getRoomWithDetails(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findByUserIdWithDetails(roomId)
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.CHATROOM_NOT_FOUND));

        Users me = (room.getRequester().getUserId().equals(userId)) ? room.getRequester() : room.getReceiver();
        Users opponent = (room.getRequester().getUserId().equals(userId)) ? room.getReceiver() : room.getRequester();

        UserProfile opProfile = userProfileRepository.findByUserId(opponent.getUserId())
                .orElse(null);

        List<Tag> opTags = userTagMapRepository.findAllTagsByUserId(opponent.getUserId());

        List<String> tagNames = opTags.stream()
                .map(Tag::getName)
                .toList();

        String majorName = "전공 미입력";
        if (opProfile != null && opProfile.getMajorId() != null) {
            majorName = majorRepository.findById(opProfile.getMajorId())
                    .map(Majors::getMajorNameKor)
                    .orElse("알 수 없는 전공");
        }

        List<ChatMessageResponseDto> chatHistory = this.getChatHistory(roomId, userId);

        return ChatRoomWithDetailDto.from(room, me, opponent, opProfile, majorName, tagNames, chatHistory);
    }


    public List<ChatRoomListDetailDto> getChatRoomList(Long userId, ChatRequest.RequestType type) {
        Users me = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

//        List<ChatRoom> myRooms = chatRoomRepository.findAllByUserIdWithBasicInfo(userId);
        List<ChatRoom> myRooms;

        if (type == null) {
            // 전체 조회
            myRooms = chatRoomRepository.findAllByUserIdWithBasicInfo(userId);
        } else {
            // 타입별 조회
            myRooms = chatRoomRepository.findAllByUserIdAndType(userId, type);
        }

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
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

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
                    String profileImgUrl = "";

                    if (opProfile != null) {
                        studentYear = opProfile.getYearLevel().toString();
                        if (opProfile.getMajorId() != null) {
                            majorName = majorRepository.findById(opProfile.getMajorId())
                                    .map(Majors::getMajorNameKor)
                                    .orElse("알 수 없는 전공");
                        }
                        profileImgUrl = publicUrlIssuer.issuePublicUrl(opProfile.getProfileImageKey());

                    }

                    Long count = unreadCounts.getOrDefault(room.getId(), 0L);
                    String lastMessage = lastMessageMap.getOrDefault(room.getId(), "대화 내용이 없습니다.");

                    return ChatRoomListDetailDto.of(room, me, count, majorName, studentYear, lastMessage, profileImgUrl);
                })
                .toList();
    }


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
        Long lastMessageId = unreadMessages.isEmpty() ? 0L : unreadMessages.getLast().getId();

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

    @Transactional
    public void sendMessage(Long senderId, ChatMessageSendRequestDto request) {

        ChatRoom room = chatRoomRepository.findById(request.roomId())
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.CHATROOM_NOT_FOUND));

        Users sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

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

            String lastTime = chat.getCreatedAt().toString();

            // 수신자의 채팅목록 갱신
            long unreadCount = chatRepository.countByRoom_IdAndReceiver_UserIdAndIsReadFalse(room.getId(), receiver.getUserId());
            long totalUnreadCount = chatRepository.countByReceiver_UserIdAndIsReadFalse(receiver.getUserId());

            ChatRoomListUpdateDto updateDto = ChatRoomListUpdateDto.builder()
                    .roomId(room.getId())
                    .lastMessage(chat.getContent())
                    .unreadCount(unreadCount)
                    .time(lastTime)
                    .totalUnreadCount(totalUnreadCount)
                    .build();

            messagingTemplate.convertAndSend(
                    "/sub/user/" + receiver.getUserId() + "/rooms",
                    updateDto
            );

            // 본인 채팅목록 갱신
            long senderTotalCount = chatRepository.countByReceiver_UserIdAndIsReadFalse(sender.getUserId());

            ChatRoomListUpdateDto senderUpdateDto = ChatRoomListUpdateDto.builder()
                    .roomId(room.getId())
                    .lastMessage(chat.getContent())
                    .unreadCount(0L)
                    .time(lastTime)
                    .totalUnreadCount(senderTotalCount)
                    .build();

            messagingTemplate.convertAndSend(
                    "/sub/user/" + sender.getUserId() + "/rooms",
                    senderUpdateDto
            );
        } catch (Exception e) {
            System.err.println("소켓 전송 실패: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public HomeResponse.CoffeeChatSection getHomeInbox(Long userId, int limit) {

        long pendingCount = chatRequestRepository.countByReceiver_UserIdAndStatus(
                userId, ChatRequest.RequestStatus.WAITING
        );
        if (pendingCount == 0) return HomeResponse.CoffeeChatSection.empty();

        List<ChatRequest> latest = chatRequestRepository.findLatestReceivedRequests(
                userId,
                ChatRequest.RequestStatus.WAITING,
                PageRequest.of(0, limit)
        );
        if (latest.isEmpty()) {
            return new HomeResponse.CoffeeChatSection(pendingCount, List.of());
        }

        List<Long> senderIds = latest.stream()
                .map(cr -> cr.getRequester().getUserId())
                .distinct()
                .toList();

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllByUserIdIn(senderIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        Set<Long> majorIds = profileMap.values().stream()
                .map(UserProfile::getMajorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> majorNameMap = majorIds.isEmpty()
                ? Map.of()
                : majorRepository.findAllById(majorIds).stream()
                .collect(Collectors.toMap(Majors::getMajorId, Majors::getMajorNameKor));

        List<HomeResponse.CoffeeChatSection.CoffeeChatPreview> previews = latest.stream()
                .map(cr -> {
                    Users sender = cr.getRequester();
                    UserProfile p = profileMap.get(sender.getUserId());

                    String majorName = null;
                    String studentNo = null;

                    if (p != null) {
                        if (p.getMajorId() != null) {
                            majorName = majorNameMap.get(p.getMajorId()); // 없으면 null
                        }
                        studentNo = (StringUtils.hasText(p.getStudentNo()) ? p.getStudentNo() : null);
                    }

                    return new HomeResponse.CoffeeChatSection.CoffeeChatPreview(
                            cr.getId(),
                            sender.getUserId(),
                            sender.getName(),
                            majorName,
                            studentNo
                    );
                })
                .toList();

        return new HomeResponse.CoffeeChatSection(pendingCount, previews);
    }

}