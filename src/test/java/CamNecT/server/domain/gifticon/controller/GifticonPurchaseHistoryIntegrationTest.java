package CamNecT.server.domain.gifticon.controller;

import CamNecT.server.domain.gifticon.model.GifticonProduct;
import CamNecT.server.domain.gifticon.model.GifticonPurchase;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.global.jwt.service.TokenSessionService;
import CamNecT.server.global.jwt.util.JwtUtil;
import CamNecT.server.global.point.model.PointWallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GifticonPurchaseHistoryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired JwtUtil jwt;
    @Autowired TokenSessionService sessions;
    @Autowired ObjectMapper json;

    @Test
    void confirmedPurchaseAppearsOnceWithHistoricalPriceAndNoPrivateFields() throws Exception {
        Users owner = user();
        var product = product();
        em.persist(PointWallet.builder().userId(owner.getUserId()).balance(2000).build());
        em.flush();
        String token = token(owner);
        String body = json.writeValueAsString(java.util.Map.of(
                "productId", product.getId(), "quantity", 2, "spendPoints", 1800,
                "clientRequestId", UUID.randomUUID().toString(), "recipientName", "수신자",
                "recipientEmail", "private@example.com", "giftMessage", "개인 메시지"));
        var confirmed = mvc.perform(post("/api/gifticons/purchases/confirm")
                        .header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        long purchaseId = json.readTree(confirmed.getResponse().getContentAsString()).at("/data/purchaseId").asLong();
        mvc.perform(post("/api/gifticons/purchases/confirm")
                        .header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.purchaseId").value(purchaseId));
        product.updateFromVendor(new GifticonProduct.VendorSnapshot(null, null, null, 1200, null, null), LocalDateTime.now());
        product.deactivate(LocalDateTime.now());
        em.flush();
        em.clear();

        var history = mvc.perform(get("/api/gifticons/purchases").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].purchaseId").value(purchaseId))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].unitPricePoints").value(900))
                .andExpect(jsonPath("$.data.items[0].totalPricePoints").value(1800))
                .andExpect(jsonPath("$.data.items[0].processingSuccess").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].processedAt").doesNotExist()).andReturn();
        Set<String> actualFields = new HashSet<>();
        json.readTree(history.getResponse().getContentAsString()).at("/data/items/0").fieldNames().forEachRemaining(actualFields::add);
        actualFields.removeAll(Set.of("processedAt", "processingSuccess"));
        // Keep contacts, recipient details, gift messages and internal administration data private.
        assertEquals(Set.of("purchaseId", "productId", "productName", "brandName", "imageUrl", "quantity",
                "unitPricePoints", "totalPricePoints", "requestedAt"), actualFields);
    }

    @Test
    void mapsProcessingResultsForHistoricalOrdersWithoutContactSnapshots() throws Exception {
        Users owner = user();
        var product = product();
        LocalDateTime processedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        for (boolean success : new boolean[]{true, false}) {
            em.persist(GifticonPurchase.builder().user(owner).product(product).quantity(1)
                    .unitPricePoints(100).totalPricePoints(100).buyerName("회원")
                    .requestedAt(processedAt.minusDays(1)).adminProcessedAt(processedAt)
                    .adminSuccess(success).adminMemo("내부 관리자 메모").build());
        }
        em.flush();
        em.clear();
        mvc.perform(get("/api/gifticons/purchases").header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].processingSuccess").value(false))
                .andExpect(jsonPath("$.data.items[1].processingSuccess").value(true))
                .andExpect(jsonPath("$.data.items[0].processedAt").value("2026-01-02T03:04:05"))
                .andExpect(jsonPath("$.data.items[0].adminMemo").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"size,0", "size,-1", "size,101", "size,2147483647", "cursor,0", "cursor,-1"})
    void rejectsInvalidPagingBeforeQuerying(String name, String value) throws Exception {
        mvc.perform(get("/api/gifticons/purchases").header("Authorization", "Bearer " + token(user()))
                        .param(name, value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paginatesOnlyOwnOrdersUsingHistoricalPriceEvenForRetiredProducts() throws Exception {
        Users owner = user();
        Users other = user();
        var product = GifticonProduct.builder().vendorProductCode(UUID.randomUUID().toString())
                .brandName("브랜드").productName("쿠폰").imageUrl("image").pricePoints(900).isActive(false).build();
        em.persist(product);
        var oldest = order(owner, product);
        order(other, product);
        var newest = order(owner, product);
        em.flush();
        String token = token(owner);

        String url = "/api/gifticons/purchases";
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).header("Authorization", "Bearer " + token).param("size", "1")
                        .param("userId", other.getUserId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].purchaseId").value(newest.getId()))
                .andExpect(jsonPath("$.data.items[0].totalPricePoints").value(200))
                .andExpect(jsonPath("$.data.items[0].recipientEmail").doesNotExist())
                .andExpect(jsonPath("$.data.nextCursor").value(newest.getId()));
        // Inserting another order must not shift the already issued cursor's next page.
        order(owner, product);
        em.flush();
        mvc.perform(get(url).header("Authorization", "Bearer " + token)
                        .param("size", "1").param("cursor", newest.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].purchaseId").value(oldest.getId()))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    private Users user() {
        var user = Users.builder().username(UUID.randomUUID().toString()).name("회원")
                .email(UUID.randomUUID() + "@example.com")
                .passwordHash("unused").role(UserRole.USER).status(UserStatus.ACTIVE).build();
        em.persist(user);
        return user;
    }

    private String token(Users owner) {
        String sessionId = UUID.randomUUID().toString();
        String token = jwt.generateAccessToken(owner.getUserId(), UserRole.USER, sessionId);
        sessions.create(owner.getUserId(), token, jwt.generateRefreshToken(owner.getUserId(), UserRole.USER, sessionId));
        return token;
    }

    private GifticonProduct product() {
        var product = GifticonProduct.builder().vendorProductCode(UUID.randomUUID().toString())
                .brandName("브랜드").productName("쿠폰").imageUrl("image").pricePoints(900).build();
        em.persist(product);
        return product;
    }

    private GifticonPurchase order(Users owner, GifticonProduct product) {
        var purchase = GifticonPurchase.builder().user(owner).product(product).quantity(2)
                .unitPricePoints(100).totalPricePoints(200).requestedAt(LocalDateTime.now())
                .buyerName("회원").recipientEmail("private@example.com").build();
        em.persist(purchase);
        return purchase;
    }
}
