package com.alphadraft.skeleton.league;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

// Sends league emails through the mail server in the MAIL_* settings. When no mail server is
// configured the message is written to the log instead, so the league still runs in development.
// A failed send is logged and never breaks the league action that caused it.
@Component
public class EmailNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    private final ObjectProvider<JavaMailSender> mail;
    private final String from;
    private final boolean configured;

    public EmailNotifier(ObjectProvider<JavaMailSender> mail,
                         @Value("${app.mail.from:alphadraft@localhost}") String from,
                         @Value("${spring.mail.host:}") String host) {
        this.mail = mail;
        this.from = from;
        this.configured = !host.isBlank();
    }

    @Override
    public void send(String to, String subject, String body) {
        JavaMailSender sender = configured ? mail.getIfAvailable() : null;
        if (sender == null) {
            log.info("Email not sent (no mail server configured) to {}: {}", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (RuntimeException e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
        }
    }
}
