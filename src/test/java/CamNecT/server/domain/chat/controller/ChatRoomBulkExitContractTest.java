package CamNecT.server.domain.chat.controller;

import CamNecT.server.domain.chat.repository.ChatRequestRepository;
import CamNecT.server.domain.chat.service.ChatService;
import CamNecT.server.global.common.auth.UserId;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.CoffeeChatErrorCode;
import CamNecT.server.global.common.util.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatRoomBulkExitContractTest {
    private ChatService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ChatService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ChatRoomController(service, mock(ChatRequestRepository.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(UserId.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
                        return 1L;
                    }
                }).build();
    }

    @Test
    void acceptsSelectedIdsAndUsesAuthenticatedUser() throws Exception {
        mvc.perform(patch("/api/chat/rooms/exit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomIds\":[20,10,20]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        verify(service).exitClosedChatRooms(List.of(20L, 10L, 20L), 1L);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void acceptsSelectionSizeBoundaries(int count) throws Exception {
        mvc.perform(patch("/api/chat/rooms/exit").contentType(MediaType.APPLICATION_JSON).content(body(count)))
                .andExpect(status().isOk());
        verify(service).exitClosedChatRooms(LongStream.rangeClosed(1, count).boxed().toList(), 1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{\"roomIds\":null}", "{\"roomIds\":[]}",
            "{\"roomIds\":[null]}", "{\"roomIds\":[0]}", "{\"roomIds\":[-1]}", "{\"roomIds\":[\"bad\"]}"})
    void rejectsInvalidPayloadBeforeServiceCall(String body) throws Exception {
        mvc.perform(patch("/api/chat/rooms/exit").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMoreThanOneHundredIds() throws Exception {
        mvc.perform(patch("/api/chat/rooms/exit").contentType(MediaType.APPLICATION_JSON).content(body(101)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
        verifyNoInteractions(service);
    }

    @Test
    void exposesOpenRoomAndInaccessibleRoomErrors() throws Exception {
        doThrow(new CustomException(CoffeeChatErrorCode.CHATROOM_NOT_CLOSED))
                .doThrow(new CustomException(CoffeeChatErrorCode.CHATROOM_NOT_FOUND))
                .when(service).exitClosedChatRooms(List.of(1L), 1L);
        mvc.perform(patch("/api/chat/rooms/exit").contentType(MediaType.APPLICATION_JSON).content(body(1)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(48007));
        mvc.perform(patch("/api/chat/rooms/exit").contentType(MediaType.APPLICATION_JSON).content(body(1)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(48402));
    }

    private String body(int count) {
        return "{\"roomIds\":[" + LongStream.rangeClosed(1, count).mapToObj(Long::toString)
                .collect(Collectors.joining(",")) + "]}";
    }
}
