package CamNecT.server.domain.gifticon.service;

import CamNecT.server.domain.gifticon.model.GifticonProduct;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GifticonVendorClientImpl implements GifticonVendorClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final SendBAuthEncoder authEncoder = new SendBAuthEncoder();

    @Value("${app.gifticon.vendor.base-url:}")
    private String baseUrl;

    @Value("${app.gifticon.vendor.mid:sendbee}")
    private String mid;

    /**
     * 업체에서 주는 “암호화 키”
     * - 길이에 따라 AES/DES 등이 달라질 수 있어서 cipher에 맞춰 동작하도록 구현했습니다.
     */
    @Value("${app.gifticon.vendor.secret-key:}")
    private String secretKey;

    /**
     * 기본은 AES/ECB/PKCS5Padding 으로 잡아두었습니다.
     * 업체 요구가 다르면 yml에서 변경하세요.
     */
    @Value("${app.gifticon.vendor.cipher:AES/ECB/PKCS5Padding}")
    private String cipher;

    /**
     * CBC 계열이면 iv 필요할 수 있습니다(없으면 ECB로 동작).
     */
    @Value("${app.gifticon.vendor.iv:}")
    private String iv;

    @Override
    public List<GifticonProduct.VendorSnapshot> fetchProducts() {
        if (baseUrl == null || baseUrl.isBlank()) return Collections.emptyList();

        String url = baseUrl + "/api/product/v1/productList";

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String authorization;
        try {
            authorization = authEncoder.encode(mid, ts, secretKey, cipher, iv);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR, e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", authorization);

        ProductListRequest req = new ProductListRequest(mid);

        ResponseEntity<ProductListResponse> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                ProductListResponse.class
        );

        ProductListResponse body = resp.getBody();
        if (body == null || body.result == null) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
        if (!"S0000".equals(body.result.returnCode)) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
        if (body.productList == null) return Collections.emptyList();

        return body.productList.stream()
                .filter(i -> "Y".equalsIgnoreCase(i.displayYN))
                .map(i -> new GifticonProduct.VendorSnapshot(
                        i.goodsId,                 // vendorProductCode
                        i.brand,                   // brandName
                        i.goodsName,               // productName
                        i.salePrice,               // pricePoints (1포인트=1원 가정)
                        i.goodsImg,                // imageUrl
                        0                          // sortScore
                ))
                .toList();
    }

    // ===== SendB 요청/응답 DTO =====
    public record ProductListRequest(String mid) {}

    public static class ProductListResponse {
        public Result result;
        public List<Item> productList;

        public static class Result {
            public String returnCode;
        }

        public static class Item {
            public String goodsId;
            public String brand;
            public String goodsName;
            public String goodsImg;
            public Integer salePrice;
            public String displayYN;
            public String description;
        }
    }
}
