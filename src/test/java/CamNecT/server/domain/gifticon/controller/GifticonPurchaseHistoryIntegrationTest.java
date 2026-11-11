package CamNecT.server.domain.gifticon.controller;

import CamNecT.server.domain.gifticon.model.GifticonProduct;
import CamNecT.server.domain.gifticon.model.GifticonPurchase;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.global.jwt.service.TokenSessionService;
import CamNecT.server.global.jwt.util.JwtUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        String sessionId = UUID.randomUUID().toString();
        String token = jwt.generateAccessToken(owner.getUserId(), UserRole.USER, sessionId);
        sessions.create(owner.getUserId(), token, jwt.generateRefreshToken(owner.getUserId(), UserRole.USER, sessionId));

        String url = "/api/gifticons/purchases";
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).header("Authorization", "Bearer " + token).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].purchaseId").value(newest.getId()))
                .andExpect(jsonPath("$.data.items[0].totalPricePoints").value(200))
                .andExpect(jsonPath("$.data.items[0].recipientEmail").doesNotExist())
                .andExpect(jsonPath("$.data.nextCursor").value(newest.getId()));
        mvc.perform(get(url).header("Authorization", "Bearer " + token)
                        .param("size", "1").param("cursor", newest.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].purchaseId").value(oldest.getId()))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    private Users user() {
        var user = Users.builder().username(UUID.randomUUID().toString()).name("회원")
                .passwordHash("unused").role(UserRole.USER).status(UserStatus.ACTIVE).build();
        em.persist(user);
        return user;
    }

    private GifticonPurchase order(Users owner, GifticonProduct product) {
        var purchase = GifticonPurchase.builder().user(owner).product(product).quantity(2)
                .unitPricePoints(100).totalPricePoints(200).requestedAt(LocalDateTime.now())
                .buyerName("회원").recipientEmail("private@example.com").build();
        em.persist(purchase);
        return purchase;
    }
}
