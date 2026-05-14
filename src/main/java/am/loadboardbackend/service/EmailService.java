package am.loadboardbackend.service;

import am.loadboardbackend.mailing.AbstractEmailContext;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2_000;

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    /**
     * Sends an HTML email rendered from a Thymeleaf template.
     * Runs asynchronously and retries up to 3 times on transient SMTP failures
     * (e.g. SocketTimeoutException) before giving up.
     */
    @Async
    public void sendEmail(AbstractEmailContext emailContext) {
        String to = emailContext.getTo();
        String subject = emailContext.getSubject();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(
                        message,
                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                        StandardCharsets.UTF_8.name()
                );

                Context thymeleafContext = new Context();
                thymeleafContext.setVariables(emailContext.getContext());
                String html = templateEngine.process(emailContext.getTemplateLocation(), thymeleafContext);

                helper.setTo(to);
                if (emailContext.getFrom() != null) {
                    helper.setFrom(emailContext.getFrom());
                }
                helper.setSubject(subject);
                helper.setText(html, true);

                mailSender.send(message);
                log.info("Email sent to={} subject={} attempt={}", to, subject, attempt);
                return; // success — stop retrying

            } catch (MessagingException | MailException e) {
                boolean isTimeout = e.getMessage() != null &&
                        (e.getMessage().contains("timeout") || e.getMessage().contains("Timeout")
                                || e.getMessage().contains("timed out"));

                if (attempt < MAX_ATTEMPTS && isTimeout) {
                    log.warn("Email attempt {}/{} timed out for to={}, retrying in {}ms — {}",
                            attempt, MAX_ATTEMPTS, to, RETRY_DELAY_MS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    log.error("Failed to send email to={} subject={} after {} attempt(s): {}",
                            to, subject, attempt, e.getMessage(), e);
                    return;
                }
            }
        }
    }
}
