package com.example.swp391.aistudenthub.feature.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final RestClient restClient;

    @Value("${brevo.api-key:placeholder}")
    private String apiKey;

    @Value("${brevo.from-email:aistudyhub@11547453.brevosend.com}")
    private String fromEmail;

    @Value("${brevo.from-name:AI Study Hub}")
    private String fromName;

    @Value("${app.frontend-url:${app.base-url:http://localhost:5173}}")
    private String baseUrl;

    public EmailService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    /**
     * Gửi email HTML chứa link đặt lại mật khẩu qua Brevo HTTP API.
     * Token là UUID raw, hash SHA-256 lưu DB. Link có hiệu lực 1 giờ.
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;

        Map<String, Object> body = Map.of(
                "sender", Map.of("email", fromEmail, "name", fromName),
                "to", List.of(Map.of("email", toEmail)),
                "subject", "[AI Study Hub] Đặt lại mật khẩu của bạn",
                "htmlContent", buildResetEmailHtml(resetLink)
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Password reset email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Dùng String.replace() thay vì .formatted() để tránh lỗi
     * UnknownFormatConversionException do ký tự % trong CSS (vd: border-radius:50%)
     */
    private String buildResetEmailHtml(String resetLink) {
        String template = "<!DOCTYPE html>"
            + "<html lang=\"vi\">"
            + "<head>"
            + "  <meta charset=\"UTF-8\">"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "  <title>Đặt lại mật khẩu - AI Study Hub</title>"
            + "</head>"
            + "<body style=\"margin:0;padding:0;background-color:#f4f4f5;"
            +              "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;\">"

            + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\""
            + "       style=\"background-color:#f4f4f5;padding:32px 16px;\">"
            + "  <tr><td align=\"center\">"

            // ── Card ──
            + "    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\""
            + "           style=\"max-width:600px;background:#ffffff;border-radius:8px;"
            + "                  box-shadow:0 1px 4px rgba(0,0,0,0.08);overflow:hidden;\">"

            // Header bar
            + "      <tr>"
            + "        <td style=\"background:#111827;padding:18px 32px;\">"
            + "          <span style=\"font-size:16px;font-weight:700;color:#ffffff;"
            + "                       letter-spacing:-0.3px;\">AI Study Hub</span>"
            + "        </td>"
            + "      </tr>"

            // Body
            + "      <tr>"
            + "        <td style=\"padding:40px 40px 0;\">"

            // Lock icon
            + "          <div style=\"display:inline-block;background:#f3f4f6;border-radius:50%;"
            + "                      padding:12px;margin-bottom:20px;line-height:1;\">"
            + "            <span style=\"font-size:28px;\">&#128272;</span>"
            + "          </div>"

            // Title
            + "          <h1 style=\"margin:0 0 12px;font-size:22px;font-weight:700;"
            + "                     color:#111827;line-height:1.3;\">Đặt lại mật khẩu</h1>"

            // Description
            + "          <p style=\"margin:0 0 28px;font-size:15px;line-height:1.65;color:#374151;\">"
            + "            Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn."
            + "            Nhấn vào nút bên dưới để tiếp tục."
            + "          </p>"

            // Button box
            + "          <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\""
            + "                 style=\"border:1.5px solid #e5e7eb;border-radius:8px;"
            + "                        margin-bottom:20px;background:#fafafa;\">"
            + "            <tr>"
            + "              <td align=\"center\" style=\"padding:10px 24px 6px;\">"
            + "                <span style=\"font-size:11px;font-weight:600;color:#9ca3af;"
            + "                             letter-spacing:2px;text-transform:uppercase;\">"
            + "                  LIÊN KẾT ĐẶT LẠI MẬT KHẨU"
            + "                </span>"
            + "              </td>"
            + "            </tr>"
            + "            <tr>"
            + "              <td align=\"center\" style=\"padding:10px 24px 24px;\">"
            + "                <a href=\"{{RESET_LINK}}\""
            + "                   style=\"display:inline-block;padding:13px 32px;"
            + "                          background:#111827;color:#ffffff;"
            + "                          text-decoration:none;font-size:15px;font-weight:600;"
            + "                          border-radius:6px;letter-spacing:0.2px;\">"
            + "                  Đặt lại mật khẩu &#8594;"
            + "                </a>"
            + "              </td>"
            + "            </tr>"
            + "          </table>"

            // Warning box — yellow left border
            + "          <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\""
            + "                 style=\"border-left:3px solid #f59e0b;"
            + "                        background:#fffbeb;border-radius:0 6px 6px 0;"
            + "                        margin-bottom:32px;\">"
            + "            <tr>"
            + "              <td style=\"padding:12px 16px;font-size:13.5px;"
            + "                         color:#92400e;line-height:1.6;\">"
            + "                &#9200; Link này có hiệu lực trong <strong>1 giờ</strong>."
            + "                Không chia sẻ link với bất kỳ ai."
            + "              </td>"
            + "            </tr>"
            + "          </table>"

            + "        </td>"
            + "      </tr>"

            // Divider
            + "      <tr>"
            + "        <td style=\"padding:0 40px;\">"
            + "          <div style=\"height:1px;background:#f3f4f6;\"></div>"
            + "        </td>"
            + "      </tr>"

            // Security note
            + "      <tr>"
            + "        <td style=\"padding:24px 40px;\">"
            + "          <p style=\"margin:0;font-size:13px;color:#6b7280;line-height:1.7;\">"
            + "            Không phải bạn yêu cầu? Hãy bỏ qua email này &mdash;"
            + "            mật khẩu của bạn vẫn an toàn."
            + "          </p>"
            + "          <p style=\"margin:12px 0 0;font-size:12px;color:#9ca3af;line-height:1.6;\">"
            + "            Nếu nút không hoạt động, sao chép link sau vào trình duyệt:<br>"
            + "            <a href=\"{{RESET_LINK}}\" style=\"color:#6366f1;text-decoration:none;"
            + "               word-break:break-all;\">{{RESET_LINK}}</a>"
            + "          </p>"
            + "        </td>"
            + "      </tr>"

            // Footer
            + "      <tr>"
            + "        <td style=\"background:#f9fafb;padding:16px 40px;"
            + "                   border-top:1px solid #f3f4f6;\">"
            + "          <p style=\"margin:0;font-size:12px;color:#9ca3af;"
            + "                    text-align:center;line-height:1.8;\">"
            + "            &copy; 2025 AI Study Hub &nbsp;&middot;&nbsp;"
            + "            <a href=\"#\" style=\"color:#9ca3af;text-decoration:none;\">Chính sách bảo mật</a>"
            + "            &nbsp;&middot;&nbsp;"
            + "            <a href=\"#\" style=\"color:#9ca3af;text-decoration:none;\">Hủy đăng ký</a>"
            + "          </p>"
            + "        </td>"
            + "      </tr>"

            + "    </table>"  // /card
            + "  </td></tr>"
            + "</table>"
            + "</body></html>";

        return template.replace("{{RESET_LINK}}", resetLink);
    }
}
