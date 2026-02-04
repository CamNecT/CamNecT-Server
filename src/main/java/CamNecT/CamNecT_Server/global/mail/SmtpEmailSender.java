package CamNecT.CamNecT_Server.global.mail;

import CamNecT.CamNecT_Server.domain.verification.document.dto.AdminReviewDocumentVerificationRequest;
import CamNecT.CamNecT_Server.domain.verification.document.model.DocumentType;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.VerificationErrorCode;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${app.auth.email-verification.mail.subject:CamNecT 이메일 인증 링크}")
    private String subject;

    @Override
    public void sendEmailVerificationCode(String toEmail, String code, long expiresMinutes) {
        try {
            var mimeMessage = mailSender.createMimeMessage();

            var helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8"
            );

            helper.setTo(toEmail);

            if (StringUtils.hasText(from)) {
                helper.setFrom(from);
            }

            helper.setSubject(subject);

            String text = """
                CamNecT 이메일 인증번호입니다.

                인증번호: %s

                유효시간: %d분
                """.formatted(code, expiresMinutes);

            String html = """
                <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                  <h2>이메일 인증번호</h2>
                  <p>아래 <b>6자리 인증번호</b>를 사이트에 입력해 인증을 완료해 주세요.</p>

                  <div style="margin: 16px 0; padding: 14px; border: 1px solid #ddd; border-radius: 8px;">
                    <div style="font-size: 14px; color: #555;">인증번호</div>
                    <div style="font-size: 28px; letter-spacing: 6px; font-weight: 700;">%s</div>
                  </div>

                  <p style="font-size: 12px; color: #777;">
                    유효시간: %d분
                  </p>
                </div>
                """.formatted(code, expiresMinutes);

            // 보안상 code는 로그에 남기지 않는 편이 좋습니다.
            log.info("[mail] send verification code to={}", toEmail);
            helper.setText(text, html); //html 우선. html이 안되는 환경이면 text
            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException e) {
            log.error("[mail] send failed to={}", toEmail, e);
            throw new CustomException(VerificationErrorCode.MAIL_SEND_FAILED, e);
        }
    }

    @Override
    public void sendDocumentVerificationResult(String toEmail,
                                        DocumentType docType,
                                        AdminReviewDocumentVerificationRequest.Decision decision,
                                        String reason) {
        try {
            var mimeMessage = mailSender.createMimeMessage();

            var helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8"
            );

            helper.setTo(toEmail);

            if (StringUtils.hasText(from)) {
                helper.setFrom(from);
            }

            String docName = switch (docType) {
                case ENROLLMENT_CERTIFICATE -> "재학증명서";
                case GRADUATION_CERTIFICATE -> "졸업증명서";
            };

            boolean approved = (decision == AdminReviewDocumentVerificationRequest.Decision.APPROVE);
            String safeReason = (reason == null) ? "" : reason.trim();

            String subjectFinal = approved
                    ? "CamNecT 증명서 인증 승인 안내"
                    : "CamNecT 증명서 인증 거부 안내";
            helper.setSubject(subjectFinal);

            // ----- TEXT -----
            String text;
            if (approved) {
                text = """
                        CamNecT 증명서 인증 결과 안내
                        
                        제출하신 %s 인증이 승인되었습니다.
                        
                        이제 서비스 이용을 진행하실 수 있습니다.
                        감사합니다.
                        """.formatted(docName);
            } else {
                String reasonLine = safeReason.isBlank() ? "사유: (미기재)" : "사유: " + safeReason;
                text = """
                        CamNecT 증명서 인증 결과 안내
                        
                        제출하신 %s 인증이 거부되었습니다.
                        
                        %s
                        
                        확인 후 증명서를 다시 제출해 주세요.
                        """.formatted(docName, reasonLine);
            }

            // ----- HTML -----
            String badge = approved ? "승인" : "거부";
            String message = approved
                    ? "제출하신 <b>%s</b> 인증이 <b>승인</b>되었습니다. 이제 서비스 이용을 진행하실 수 있습니다."
                    .formatted(docName)
                    : "제출하신 <b>%s</b> 인증이 <b>거부</b>되었습니다. 아래 사유를 확인 후 다시 제출해 주세요."
                    .formatted(docName);

            String reasonHtml = "";
            if (!approved) {
                String reasonBody = safeReason.isBlank() ? "(미기재)" : escapeHtml(safeReason);
                reasonHtml = """
                        <div style="margin-top: 14px; padding: 14px; border: 1px solid #eee; border-radius: 10px;">
                          <div style="font-size: 13px; color: #666; margin-bottom: 6px;">거부 사유</div>
                          <div style="font-size: 14px; white-space: pre-line;">%s</div>
                        </div>
                        """.formatted(reasonBody);
            }

            String html = """
                    <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #222;">
                      <h2 style="margin: 0 0 12px;">증명서 인증 결과 안내</h2>
                    
                      <div style="display: inline-block; padding: 6px 10px; border-radius: 999px; font-size: 12px; background: #f3f4f6;">
                        %s
                      </div>
                    
                      <p style="margin: 14px 0 0;">%s</p>
                    
                      %s
                    
                      <p style="margin-top: 18px; font-size: 12px; color: #777;">
                        본 메일은 발신 전용입니다.
                      </p>
                    </div>
                    """.formatted(badge, message, reasonHtml);

            log.info("[mail] send document verification result to={} decision={}", toEmail, decision);
            helper.setText(text, html);
            mailSender.send(mimeMessage);

        } catch (MessagingException | MailException e) {
            log.error("[mail] send failed to={}", toEmail, e);
            throw new CustomException(VerificationErrorCode.MAIL_SEND_FAILED, e);
        }
    }

    /**
     * 간단 HTML escape (사유에 꺽쇠 등 들어갈 때 깨짐 방지)
     * - 필요 최소만 처리
     */
    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

