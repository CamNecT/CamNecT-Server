package CamNecT.server.domain.chat.service;


import CamNecT.server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.server.domain.activity.repository.recruitment.TeamRecruitmentRepository;
import CamNecT.server.domain.chat.dto.message.ChatMessageResponseDto;
import CamNecT.server.domain.chat.dto.message.ChatMessageSendRequestDto;
import CamNecT.server.domain.chat.dto.message.ChatReadEvent;
import CamNecT.server.domain.chat.dto.request.response.ChatRequestDetailDto;
import CamNecT.server.domain.chat.dto.request.response.ChatRequestListDetailDto;
import CamNecT.server.domain.chat.dto.request.response.ChatRequestListResponseDto;
import CamNecT.server.domain.chat.dto.room.ChatRoomListDetailDto;
import CamNecT.server.domain.chat.dto.room.ChatRoomListUpdateDto;
import CamNecT.server.domain.chat.dto.room.ChatRoomWithDetailDto;
import CamNecT.server.domain.chat.model.Chat;
import CamNecT.server.domain.chat.model.ChatRequest;
import CamNecT.server.domain.chat.model.ChatRoom;
import CamNecT.server.domain.chat.repository.ChatRepository;
import CamNecT.server.domain.chat.repository.ChatRequestRepository;
import CamNecT.server.domain.chat.repository.ChatRoomRepository;
import CamNecT.server.domain.home.dto.HomeResponse;
import CamNecT.server.domain.profile.dto.ProfileGlobalDto;
import CamNecT.server.domain.users.model.UserProfile;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserProfileRepository;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.domain.users.repository.UserTagMapRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.CoffeeChatErrorCode;
import CamNecT.server.domain.profile.components.majors.model.Majors;
import CamNecT.server.global.notification.event.*;
import CamNecT.server.global.notification.event.CoffeeChatAcceptedEvent;
import CamNecT.server.global.notification.event.CoffeeChatRequestedEvent;
import CamNecT.server.global.notification.event.NewChatMessageEvent;
import CamNecT.server.global.notification.event.TeamRecruitAcceptedEvent;
import CamNecT.server.global.point.model.PointEvent;
import CamNecT.server.global.point.service.PointService;
import CamNecT.server.global.storage.service.PublicUrlIssuer;
import CamNecT.server.global.tag.model.Tag;
import CamNecT.server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.server.global.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    @Value("${app.point.reward.coffee-chat-accepted:500}")
    private int rewardCoffeeChatAccepted;

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRequestRepository chatRequestRepository;
    private final TagRepository tagRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserTagMapRepository userTagMapRepository;
    private final MajorRepository majorRepository;
    private final TeamRecruitmentRepository recruitmentRepository;

    private final PublicUrlIssuer publicUrlIssuer;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatPresenceService presenceService;
    private final PointService pointService;

    /*
      1. 커피챗 요청 보내기
      Requester -> Receiver임.
     */
    @Transactional
    public Long sendCoffeeChatRequest(Long requesterId, Long receiverId, List<Long> tagIds, String content) {
        if (requesterId.equals(receiverId)) {
            throw new CustomException(CoffeeChatErrorCode.SELF_REQUEST_NOT_ALLOWED);
        }

        if (chatRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatusAndType(
                requesterId, receiverId, ChatRequest.RequestStatus.WAITING, ChatRequest.RequestType.COFFEE_CHAT)
                || chatRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatusAndType(
                receiverId, requesterId, ChatRequest.RequestStatus.WAITING, ChatRequest.RequestType.COFFEE_CHAT)
        ) {
            throw new CustomException(CoffeeChatErrorCode.DUPLICATE_REQUEST);
        }

        if (chatRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatusAndType(
                requesterId, receiverId, ChatRequest.RequestStatus.ACCEPTED, ChatRequest.RequestType.COFFEE_CHAT)
                || chatRequestRepository.existsByRequester_UserIdAndReceiver_UserIdAndStatusAndType(
                receiverId, requesterId, ChatRequest.RequestStatus.ACCEPTED, ChatRequest.RequestType.COFFEE_CHAT)) {
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

        Long requestId = chatRequestRepository.save(request).getId();

        log.info("[coffeechat] txActive(beforePublish)={}, requestId={}",
                TransactionSynchronizationManager.isActualTransactionActive(), requestId);

        eventPublisher.publishEvent(new CoffeeChatRequestedEvent(
                receiverId,
                requesterId,
                requestId
        ));

        log.info("[coffeechat] published event. txActive(afterPublish)={}",
                TransactionSynchronizationManager.isActualTransactionActive());

        return requestId;
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
        if (!request.getReceiver().getUserId().equals(userId)) throw new CustomException(CoffeeChatErrorCode.REQUEST_ACCESS_DENIED);

        if (request.getStatus().equals(ChatRequest.RequestStatus.ACCEPTED)) { return; }
        if (request.getStatus().equals(ChatRequest.RequestStatus.REJECTED)) { return; }

        if (isAccepted) {
            request.accept();
            Long requesterId = request.getRequester().getUserId();
            Long roomId = createChatRoom(request);
            pointService.earnPoint(requesterId, rewardCoffeeChatAccepted,
                    PointEvent.coffeeChatAccepted(requesterId,request.getId()));
            tryRewardCoffeeChatAcceptedPoint(request);
            publishAcceptedNotification(request, roomId);
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
        String studentNo = "학번 미입력";
        String profileImgUrl = "/images/default.png";
        if (opProfile != null) {
            if (opProfile.getMajorId() != null) {
                majorName = majorRepository.findById(opProfile.getMajorId())
                        .map(Majors::getMajorNameKor)
                        .orElse("알 수 없는 전공");
            }
            studentNo = opProfile.getStudentNo();
            profileImgUrl = publicUrlIssuer.issuePublicUrl(opProfile.getProfileImageKey());
        }

        List<String> opTagNames = userTagMapRepository.findAllTagsByUserId(opponent.getUserId())
                .stream()
                .map(Tag::getName)
                .toList();

        return ChatRequestDetailDto.from(me, opponent, request, majorName, studentNo, opTagNames, profileImgUrl, title);
    }

    @Transactional(readOnly = true)
    public ChatRequestListResponseDto getChatRequestList(Long userId, ChatRequest.RequestType type) {
        List<ChatRequest> requests = chatRequestRepository.findRequestsWithRequester(
                userId, type, ChatRequest.RequestStatus.WAITING);

        if (requests.isEmpty()) {
            return new ChatRequestListResponseDto(List.of());
        }

        Set<Long> recruitmentIds = requests.stream()
                .map(ChatRequest::getRecruitmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> recruitmentTitleMap = recruitmentIds.isEmpty() ? new HashMap<>() :
                recruitmentRepository.findAllById(recruitmentIds).stream()
                        .collect(Collectors.toMap(TeamRecruitment::getRecruitId, TeamRecruitment::getTitle));

        List<Long> opponentIds = requests.stream()
                .map(req -> req.getRequester().getUserId())
                .distinct()
                .toList();

        Map<Long, ProfileGlobalDto> globalMap = userProfileRepository.findGlobalsByUserIdIn(opponentIds).stream()
                .collect(Collectors.toMap(ProfileGlobalDto::userId, it -> it));

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllByUserIdIn(opponentIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<ChatRequestListDetailDto> dtoList = requests.stream()
                .map(request -> {
                    Users opponent = request.getRequester();
                    ProfileGlobalDto g = globalMap.get(opponent.getUserId());
                    UserProfile opProfile = profileMap.get(opponent.getUserId());

                    String majorName = "전공 미입력";
                    if (g != null && StringUtils.hasText(g.majorName())) {
                        majorName = g.majorName();
                    }

                    String profileImgUrl = "/images/default.png";
                    if (g != null && StringUtils.hasText(g.profileImageKey())) {
                        profileImgUrl = publicUrlIssuer.issuePublicUrl(g.profileImageKey());
                    }

                    String title = recruitmentTitleMap.getOrDefault(request.getRecruitmentId(), "커피챗 요청");

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
    private Long createChatRoom(ChatRequest request) {
        // 1) 이미 생성된 방이 있으면 그대로 반환 (중복 호출/재시도 대비)
        Optional<ChatRoom> existing = chatRoomRepository.findByRequest_Id(request.getId());
        if (existing.isPresent()) return existing.get().getId();

        // 2) 없으면 생성 시도 (동시성 대비: 유니크 터지면 다시 조회)
        try {
            ChatRoom chatRoom = ChatRoom.createRoom(request, request.getRequester(), request.getReceiver());
            ChatRoom saved = chatRoomRepository.save(chatRoom);
            return saved.getId();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // request_id UNIQUE에 걸린 케이스 → 이미 누가 만들었음
            return chatRoomRepository.findByRequest_Id(request.getId())
                    .map(ChatRoom::getId)
                    .orElseThrow(() -> e);
        }
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
        ChatRoom room = chatRoomRepository.findByUserIdWithDetails(roomId, userId)
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.CHATROOM_NOT_FOUND));

        Users me = (room.getRequester().getUserId().equals(userId)) ? room.getRequester() : room.getReceiver();
        Users opponent = (room.getRequester().getUserId().equals(userId)) ? room.getReceiver() : room.getRequester();

        UserProfile opProfile = userProfileRepository.findByUserId(opponent.getUserId()).orElse(null);
        String majorName = "전공 미입력";
        String profileImgUrl = "/images/default.png";

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

        List<String> tagNames = userTagMapRepository.findAllTagsByUserId(opponent.getUserId())
                .stream()
                .map(Tag::getName)
                .toList();

        List<ChatMessageResponseDto> chatHistory = this.getChatHistory(roomId, userId);

        String title = "커피챗 요청";
        if (room.getRequest().getType() == ChatRequest.RequestType.TEAM_RECRUIT) {
            Long recruitmentId = room.getRequest().getRecruitmentId();
            if (recruitmentId != null) {
                title = recruitmentRepository.findById(recruitmentId)
                        .map(TeamRecruitment::getTitle)
                        .orElse("삭제된 모집 공고입니다.");
            }
        }

        return ChatRoomWithDetailDto.from(room, me, opponent, opProfile, majorName, tagNames, chatHistory, title, profileImgUrl);
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
                    String profileImgUrl = "/images/default.png";

                    if (opProfile != null) {
                        studentYear = (opProfile.getYearLevel() != null) ? opProfile.getYearLevel().toString() : "미입력";
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

        log.info("📚 읽음 처리 완료: {}건 저장됨.", unreadMessages.size());

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

        // 읽은 당사자(Reader)의 '채팅 목록/전체 배지' 갱신을 위해 소켓 발송

        long totalUnreadCount = chatRepository.countByReceiver_UserIdAndIsReadFalse(reader.getUserId());

        long roomUnreadCount = 0L;

        String lastContent = unreadMessages.getLast().getContent();
        String lastTime = unreadMessages.getLast().getCreatedAt().toString();

        ChatRoomListUpdateDto updateDto = ChatRoomListUpdateDto.builder()
                .roomId(roomId)
                .lastMessage(lastContent)
                .unreadCount(roomUnreadCount)
                .time(lastTime)
                .totalUnreadCount(totalUnreadCount)
                .build();

        messagingTemplate.convertAndSend(
                "/sub/user/" + reader.getUserId() + "/rooms",
                updateDto
        );

        log.info("🚀 [Socket] 읽음 처리 후 목록 갱신 전송: /sub/user/{}/rooms (남은 전체 미독: {})",
                reader.getUserId(), totalUnreadCount);

    }

    @Transactional
    public void sendMessage(Long senderId, ChatMessageSendRequestDto request) {
        log.info("[CHAT-SEND] === 메세지 전송 시작 === RoomID: {}, SenderID: {}", request.roomId(), senderId);

        ChatRoom room = chatRoomRepository.findById(request.roomId())
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.CHATROOM_NOT_FOUND));

        if (room.getStatus() == ChatRoom.RoomStatus.CLOSE) {
            log.error("❌ [CHAT-ERROR] 이미 종료된 채팅방입니다. RoomID: {}", request.roomId());
            throw new CustomException(CoffeeChatErrorCode.COFFEE_CHAT_CLOSED);
        }

        Users sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        Users receiver = (room.getRequester().getUserId().equals(sender.getUserId()))
                ? room.getReceiver()
                : room.getRequester();
        log.info("👤 Sender: {} -> Receiver: {}", sender.getUserId(), receiver.getUserId());

        boolean receiverPresent = presenceService.isPresent(room.getId(), receiver.getUserId());
        log.info("👀 상대방(receiver) 접속 여부: {}", receiverPresent);

        Chat chat = Chat.createChat(room, sender, receiver, request.content());

        if (receiverPresent) {
            chat.markAsRead();
            log.info("✅ 상대방이 접속 중이므로 읽음 처리됨");
        }

        chatRepository.save(chat);
        room.updateLastMessageTime();
        log.info("💾 메시지 DB 저장 완료 (ChatID: {})", chat.getId());

        /// 상대방 부대중일때 알림 발송(도메인에서 알림구현)
        if (!receiverPresent) {
            eventPublisher.publishEvent(new NewChatMessageEvent(
                    receiver.getUserId(), // 받는 사람
                    sender.getUserId(),   // 보낸 사람
                    room.getId(),         // 채팅방 ID
                    chat.getContent()     // 메시지 내용 (Event 내부에서 길이 조절됨)
            ));
            log.info("🔔 상대방 부재중 - 알림 이벤트(EventPublisher) 발행");
        }

        ChatMessageResponseDto response = ChatMessageResponseDto.toDto(chat);

        try {
            String roomDest = "/sub/chat/room/" + room.getId();
            messagingTemplate.convertAndSend("/sub/chat/room/" + room.getId(), response);

/*            if (receiverPresent) {
                messagingTemplate.convertAndSendToUser(
                        sender.getUserId().toString(),
                        "/queue/read",
                        ChatReadEvent.of(room.getId(), chat.getId(), chat.getReadAt().toString())
                );
            }*/
            log.info("🚀 [Socket] 채팅방 브로드캐스트 전송: {}", roomDest);
            String lastTime = chat.getCreatedAt().toString();

            // 수신자의 채팅목록 갱신
            long unreadCount = chatRepository.countByRoom_IdAndReceiver_UserIdAndIsReadFalse(room.getId(), receiver.getUserId());
            long totalUnreadCount = chatRepository.countByReceiver_UserIdAndIsReadFalse(receiver.getUserId());
            String receiverDest = "/sub/user/" + receiver.getUserId() + "/rooms";

            ChatRoomListUpdateDto updateDto = ChatRoomListUpdateDto.builder()
                    .roomId(room.getId())
                    .lastMessage(chat.getContent())
                    .unreadCount(unreadCount)
                    .time(lastTime)
                    .totalUnreadCount(totalUnreadCount)
                    .build();
            log.info("🚀 [Socket] 수신자 목록 갱신 전송: {} (미읽음: {})", receiverDest, unreadCount);

            messagingTemplate.convertAndSend(
                    "/sub/user/" + receiver.getUserId() + "/rooms",
                    updateDto
            );

            // 본인 채팅목록 갱신
            long senderTotalCount = chatRepository.countByReceiver_UserIdAndIsReadFalse(sender.getUserId());
            String senderDest = "/sub/user/" + sender.getUserId() + "/rooms";

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
            log.info("🚀 [Socket] 발신자 목록 갱신 전송: {}", senderDest);
        } catch (Exception e) {
            log.error("❌ [Socket-ERROR] 전송 실패: {}", e.getMessage(), e);
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
                .distinct().toList();

        Map<Long, ProfileGlobalDto> globalMap =
                userProfileRepository.findGlobalsByUserIdIn(senderIds).stream()
                        .collect(Collectors.toMap(ProfileGlobalDto::userId, it -> it));

        List<HomeResponse.CoffeeChatSection.CoffeeChatPreview> previews = latest.stream()
                .map(cr -> {
                    Users sender = cr.getRequester();
                    ProfileGlobalDto g = globalMap.get(sender.getUserId());

                    String majorName = (g != null ? g.majorName() : null);
                    String studentNo = (g != null && StringUtils.hasText(g.studentNo()) ? g.studentNo() : null);

                    return new HomeResponse.CoffeeChatSection.CoffeeChatPreview(
                            cr.getId(),
                            sender.getUserId(),
                            sender.getName(), // 이름은 sender에서 그대로
                            majorName,
                            studentNo
                    );
                })
                .toList();

        return new HomeResponse.CoffeeChatSection(pendingCount, previews);
    }

    @Transactional
    public void rejectAllCoffeeChatRequests(Long userId, ChatRequest.RequestType requestType) {
        List<ChatRequest> requests = chatRequestRepository.findAllByReceiver_UserIdAndTypeAndStatus(
                userId, requestType, ChatRequest.RequestStatus.WAITING);

        requests.forEach(ChatRequest::reject);
    }

    @Transactional
    public void rejectAllTeamRecruitRequestsByRecruitment(Long userId, Long recruitmentId) {
        List<ChatRequest> requests = chatRequestRepository.findAllByReceiver_UserIdAndTypeAndRecruitmentIdAndStatus(
                userId,
                ChatRequest.RequestType.TEAM_RECRUIT,
                recruitmentId,
                ChatRequest.RequestStatus.WAITING
        );

        requests.forEach(ChatRequest::reject);
    }
    
    public void closeChatRoom(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findByUserIdWithDetails(roomId, userId)
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.CHATROOM_NOT_FOUND));
        room.closeRoom();
    }

    public void exitOfChatRoom(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findByUserIdWithDetails(roomId, userId)
                .orElseThrow(() -> new CustomException(CoffeeChatErrorCode.CHATROOM_NOT_FOUND));
        room.leave(userId);

        ChatRequest request = room.getRequest();
        if (request == null) {
            throw new CustomException(CoffeeChatErrorCode.REQUESTER_NOT_FOUND);
        }
        request.closeRequest();

    }

    private void publishAcceptedNotification(ChatRequest request, Long roomId) {
        Long receiverUserId = request.getRequester().getUserId(); // 요청자
        Long actorUserId = request.getReceiver().getUserId();     // 승인자

        if (request.getType() == ChatRequest.RequestType.TEAM_RECRUIT) {
            eventPublisher.publishEvent(new TeamRecruitAcceptedEvent(
                    receiverUserId, actorUserId, roomId, request.getRecruitmentId()
            ));
        } else {
            eventPublisher.publishEvent(new CoffeeChatAcceptedEvent(
                    receiverUserId, actorUserId, roomId, request.getId()
            ));
        }
    }

    private void tryRewardCoffeeChatAcceptedPoint(ChatRequest request) {
        Long requestId = request.getId();
        Long targetUserId = (request.getRequester() == null) ? null : request.getRequester().getUserId();

        if (requestId == null || targetUserId == null) {
            log.warn("[coffeechat] skip point reward. requestId={}, targetUserId={}", requestId, targetUserId);
            return;
        }
        try {
            pointService.earnPoint(targetUserId, rewardCoffeeChatAccepted,
                    PointEvent.coffeeChatAccepted(targetUserId,request.getId()));
        } catch (Exception ex) {
            log.warn("[coffeechat] point reward failed. requestId={}, userId={}", requestId, targetUserId, ex);
        }
    }
}