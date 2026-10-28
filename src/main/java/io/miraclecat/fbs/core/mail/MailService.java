package io.miraclecat.fbs.core.mail;

import io.miraclecat.fbs.core.config.AsyncConfig;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * packageName    : io.miraclecat.fbs.core.mail
 * fileName       : MailService
 * author         : MiracleCat
 * date           : 2026-10-26
 * description    : HTML 메일 비동기 발송 (templates/mail/*.html)
 *                  메일 설정이 없는 환경(spring.mail 미설정)에서는 발송하지 않고 로그만 남김
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private static final String SENDER_NAME = "Flight Booking";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * @param template templates/mail 아래 파일명 (확장자 제외)
     */
    @Async(AsyncConfig.MAIL_EXECUTOR)
    public void send(String to, String subject, String template, Map<String, Object> variables) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || from.isBlank()) {
            log.warn("[메일 미설정] 발송 생략: to={}, subject={}", to, subject);
            return;
        }

        try {
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process("/mail/" + template, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from, SENDER_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("[메일 발송] to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("[메일 발송 실패] to={}, subject={}", to, subject, e);
        }
    }
}
