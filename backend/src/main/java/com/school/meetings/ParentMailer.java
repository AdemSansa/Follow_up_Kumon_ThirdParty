package com.school.meetings;

import com.school.meetings.model.NotificationKind;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Plain-text emails to parents about an online check-in. Only time, link
 * and who they'll meet — never ratings, concerns or internal notes
 * (business rule 9, docs/PROJECT_SCOPE.md).
 */
@Component
public class ParentMailer {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'at' HH:mm", Locale.ENGLISH);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final MeetingProperties properties;

    public ParentMailer(ObjectProvider<JavaMailSender> mailSender, MeetingProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public record MeetingEmail(String to, String parentFirstName, String studentFirstName, String staffName,
                               LocalDateTime start, int durationMinutes, String joinUrl) {
    }

    public void send(NotificationKind kind, MeetingEmail email) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("Email is not configured (MAIL_HOST)");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(email.to());
        message.setSubject(subject(kind, email));
        message.setText(body(kind, email));
        sender.send(message);
    }

    private String subject(NotificationKind kind, MeetingEmail e) {
        String when = WHEN.format(e.start());
        return switch (kind) {
            case MEETING_INVITE -> "Online check-in for " + e.studentFirstName() + " — " + when;
            case MEETING_RESCHEDULED -> "New time: online check-in for " + e.studentFirstName() + " — " + when;
            case MEETING_CANCELLED -> "Cancelled: online check-in for " + e.studentFirstName();
        };
    }

    private String body(NotificationKind kind, MeetingEmail e) {
        String hello = "Hello" + (e.parentFirstName() != null ? " " + e.parentFirstName() : "") + ",\n\n";
        String when = WHEN.format(e.start()) + " (" + properties.zone().getId() + "), " + e.durationMinutes()
                + " minutes";
        String with = e.staffName() != null ? "With: " + e.staffName() + "\n" : "";
        String sign = "\nSee you soon,\n" + properties.centerName() + "\n";
        return switch (kind) {
            case MEETING_INVITE -> hello
                    + "This is a reminder of " + e.studentFirstName() + "'s online check-in.\n\n"
                    + "When: " + when + "\n" + with
                    + "Join with Google Meet: " + e.joinUrl() + "\n\n"
                    + "No account or app is needed — open the link in your browser a few minutes before.\n"
                    + sign;
            case MEETING_RESCHEDULED -> hello
                    + e.studentFirstName() + "'s online check-in has been moved.\n\n"
                    + "New time: " + when + "\n" + with
                    + "Join with Google Meet: " + e.joinUrl() + "\n"
                    + sign;
            case MEETING_CANCELLED -> hello
                    + e.studentFirstName() + "'s online check-in planned for " + WHEN.format(e.start())
                    + " has been cancelled. We will be in touch to arrange a new time.\n"
                    + sign;
        };
    }
}
