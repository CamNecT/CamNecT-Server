package CamNecT.server.domain.gifticon.controller;

import CamNecT.server.domain.gifticon.dto.request.ConfirmGifticonPurchaseRequest;
import CamNecT.server.domain.gifticon.dto.response.GifticonPurchaseConfirmResponse;
import CamNecT.server.domain.gifticon.service.GifticonPurchaseService;
import CamNecT.server.domain.gifticon.service.GifticonService;
import CamNecT.server.global.common.util.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GifticonPhonePurchaseContractTest {
    @Test
    void newApiRequiresPhoneAndDelegatesNormalizedOrderToExistingAtomicPurchaseService() throws Exception {
        var service = mock(GifticonPurchaseService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new GifticonController(mock(GifticonService.class), service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var mapper = new ObjectMapper();
        var body = new LinkedHashMap<String,Object>(Map.of("productId",10,"quantity",1,"spendPoints",1000,"clientRequestId","phone-order"));
        for (String phone : new String[]{null, "", "  ", "010123", "invalid", "010123456789"}) {
            body.put("recipientPhone", phone);
            mvc.perform(post("/api/gifticons/purchases/confirm-with-phone").contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(body))).andExpect(status().isBadRequest());
        }
        body.remove("recipientPhone");
        mvc.perform(post("/api/gifticons/purchases/confirm-with-phone").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(body))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
        body.put("recipientPhone", " 010-1234-5678 ");
        when(service.confirm(isNull(), any())).thenReturn(new GifticonPurchaseConfirmResponse(100L, LocalDateTime.now()));
        mvc.perform(post("/api/gifticons/purchases/confirm-with-phone").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(body))).andExpect(status().isOk()).andExpect(jsonPath("$.data.purchaseId").value(100));
        verify(service).confirm(isNull(), eq(new ConfirmGifticonPurchaseRequest(10L, 1, 1000, "phone-order", null, null, null, "01012345678")));
    }
}
