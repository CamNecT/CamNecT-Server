package CamNecT.CamNecT_Server.domain.gifticon.service;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class SendBAuthEncoder {

    /**
     * SendB 스펙: "{mid}|{yyyyMMddHHmmss}" 를 PKCS5Padding 방식으로 암호화하여 Authorization 헤더에 사용
     * cipher 예시:
     * - AES/ECB/PKCS5Padding (iv 불필요)
     * - AES/CBC/PKCS5Padding (iv 필요)
     * - DES/ECB/PKCS5Padding (키 길이 8)
     */
    public String encode(String mid, String timestamp, String secretKey, String cipher, String iv) throws Exception {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("app.gifticon.vendor.secret-key is required");
        }

        String plain = mid + "|" + timestamp;
        byte[] plainBytes = plain.getBytes(StandardCharsets.UTF_8);

        String algo = cipher.split("/")[0]; // AES, DES 등
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

        // 키 길이 미스매치 방지: 너무 길면 잘라서 맞추는 방식(업체 키 규격에 맞추는 게 최우선)
        // AES: 16/24/32, DES: 8
        keyBytes = normalizeKey(keyBytes, algo);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, algo);
        Cipher c = Cipher.getInstance(cipher);

        if (cipher.contains("/CBC/")) {
            if (iv == null || iv.isBlank()) {
                throw new IllegalArgumentException("CBC mode requires app.gifticon.vendor.iv");
            }
            byte[] ivBytes = normalizeIv(iv.getBytes(StandardCharsets.UTF_8), c.getBlockSize());
            c.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(ivBytes));
        } else {
            c.init(Cipher.ENCRYPT_MODE, keySpec);
        }

        byte[] enc = c.doFinal(plainBytes);
        return Base64.getEncoder().encodeToString(enc);
    }

    private byte[] normalizeKey(byte[] keyBytes, String algo) {
        if ("DES".equalsIgnoreCase(algo)) {
            return fit(keyBytes, 8);
        }
        // 기본 AES 가정
        if ("AES".equalsIgnoreCase(algo)) {
            if (keyBytes.length <= 16) return fit(keyBytes, 16);
            if (keyBytes.length <= 24) return fit(keyBytes, 24);
            return fit(keyBytes, 32);
        }
        // 기타 알고리즘이면 그대로
        return keyBytes;
    }

    private byte[] normalizeIv(byte[] ivBytes, int blockSize) {
        return fit(ivBytes, blockSize);
    }

    private byte[] fit(byte[] src, int size) {
        byte[] out = new byte[size];
        for (int i = 0; i < size; i++) {
            out[i] = (i < src.length) ? src[i] : 0;
        }
        return out;
    }
}