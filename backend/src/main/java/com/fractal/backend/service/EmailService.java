package com.fractal.backend.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendWorkspaceInvite(String toEmail, String workspaceName, String inviteToken) {
        String inviteLink = frontendUrl + "/auth/invite?token=" + inviteToken;

        Email from = new Email("no-reply@fractal.com"); // Use a verified sender ID in SendGrid
        String subject = "You've been invited to join " + workspaceName;
        Email to = new Email(toEmail);

        String htmlContent = String.format(
                "<h1>Join %s on Fractal</h1>" +
                        "<p>You have been invited to collaborate on <strong>%s</strong>.</p>" +
                        "<p><a href='%s' style='background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Accept Invitation</a></p>"
                        +
                        "<p>Or copy this link: %s</p>",
                workspaceName, workspaceName, inviteLink, inviteLink);

        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() >= 400) {
                log.error("Failed to send email: " + response.getBody());
            } else {
                log.info("Invitation email sent to " + toEmail);
            }
        } catch (IOException ex) {
            log.error("Error sending email", ex);
        }
    }
}