package CamNecT.server.domain.community.service;

import CamNecT.server.domain.community.model.enums.BoardCode;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.tag.dto.TagDto;
import CamNecT.server.global.tag.repository.TagRepository;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class PostTagSuggestionServiceTest {
    @Test
    void onlySelectableTagsAreReturnedInReaderOrderAndFailuresDoNotWrite() throws Exception {
        TagRepository tags = mock(TagRepository.class);
        when(tags.findSelectableForPost(anyCollection())).thenReturn(List.of(new TagDto(53L, "백엔드")));
        HttpServer stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/suggest", exchange -> {
            try (exchange) {
                byte[] data = """
                        [{"id":1,"evidence":null},{"id":53,"evidence":"개발"},{"id":53,"evidence":"중복"}]
                        """.getBytes(StandardCharsets.UTF_8);
                byte[] request = exchange.getRequestBody().readAllBytes();
                if (!String.valueOf(request.length).equals(exchange.getRequestHeaders().getFirst("Content-Length"))) {
                    exchange.sendResponseHeaders(413, -1);
                    return;
                }
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, data.length);
                exchange.getResponseBody().write(data);
            }
        });
        stub.start();
        var service = new PostTagSuggestionService(tags, new ObjectMapper(), "http://127.0.0.1:" + stub.getAddress().getPort());
        var draft = new PostTagSuggestionService.Draft(BoardCode.QUESTION, "질문", "서버 개발");
        try {
            assertThat(service.suggest(new PostTagSuggestionService.Draft(BoardCode.INFO, "", " "))).isEmpty();
            verifyNoInteractions(tags);
            assertThat(service.suggest(draft)).containsExactly(new PostTagSuggestionService.Suggestion(53, "백엔드", "개발"));
        } finally {
            stub.stop(0);
        }
        assertThatThrownBy(() -> service.suggest(draft)).isInstanceOf(CustomException.class);
        verify(tags).findSelectableForPost(anyCollection());
        verifyNoMoreInteractions(tags);
    }
}
