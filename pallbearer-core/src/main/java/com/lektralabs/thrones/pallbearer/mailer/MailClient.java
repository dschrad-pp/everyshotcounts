package com.lektralabs.thrones.pallbearer.mailer;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MailClient {

    private static final String FROM = "support@lektralabs.com";

    @Inject
    Mailer mailer;

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance forgotPassword(String temporaryPassword);
        public static native TemplateInstance forgotUsername(String username);
        public static native TemplateInstance newRegistration(String sixDigitCode);
    }

    public void sendTextEmail(String to, String subject, String body) {
        Mail mail = buildTextMail(to, subject, body);
        mailer.send(mail);
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        Mail mail = buildHtmlMail(to, subject, htmlBody);
        mailer.send(mail);
    }

    public void sendForgotPassword(String to, String temporaryPassword) {
        String body = Templates.forgotPassword(temporaryPassword).render();
        Mail mail = buildHtmlMail(to, "Forgot Password", body);
        mailer.send(mail);
    }

    public void sendForgotUsername(String to, String username) {
        String body = Templates.forgotUsername(username).render();
        Mail mail = buildHtmlMail(to, "Forgot Username", body);
        mailer.send(mail);
    }

    public void sendNewRegistration(String to, String sixDigitCode) {
        String body = Templates.forgotUsername(sixDigitCode).render();
        Mail mail = buildHtmlMail(to, "New Registration", body);
        mailer.send(mail);
    }

    private static Mail buildHtmlMail(String to, String subject, String htmlBody) {
        Mail mail = new Mail()
                .setFrom(FROM)
                .setReplyTo(FROM)
                .addTo(to)
                .setSubject(subject)
                .setHtml(htmlBody);
        return mail;
    }

    private static Mail buildTextMail(String to, String subject, String body) {
        Mail mail = new Mail()
                .setFrom(FROM)
                .setReplyTo(FROM)
                .addTo(to)
                .setSubject(subject)
                .setText(body);
        return mail;
    }
}
