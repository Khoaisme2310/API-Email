package com.example.emailapp.service;

import com.example.emailapp.model.Email;
import com.example.emailapp.model.User;
import com.example.emailapp.repository.EmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private JavaMailSender mailSender;

    public void sendInternalEmail(User sender, User receiver, String subject, String content) {
        Email email = new Email();
        email.setSender(sender);
        email.setReceiver(receiver);
        email.setSubject(subject);
        email.setContent(content);
        email.setSentTime(LocalDateTime.now());

        emailRepository.save(email);
    }

    public void sendExternalEmail(User sender, String receiverAddress, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(sender.getEmail());
            helper.setTo(receiverAddress);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending external email", e);
        }
    }

    public void sendExternalEmailWithAttachment(User sender, String receiverAddress, String subject, String content, MultipartFile file) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(sender.getEmail());
            helper.setTo(receiverAddress);
            helper.setSubject(subject);
            helper.setText(content, true);

            if (file != null && !file.isEmpty()) {
                helper.addAttachment(file.getOriginalFilename(), file);
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email with attachment", e);
        }
    }

    public void sendInternalEmailWithAttachment(User sender, User receiver, String subject, String content, MultipartFile file) throws IOException {
        Email email = new Email();
        email.setSender(sender);
        email.setReceiver(receiver);
        email.setSubject(subject);
        email.setContent(content);
        email.setSentTime(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            // Lưu tên và đường dẫn của file
            String filePath = saveFileLocally(file);
            email.setAttachmentName(file.getOriginalFilename());  // Lưu tên tệp
            email.setAttachmentPath(filePath);  // Lưu đường dẫn
        }

        emailRepository.save(email);
    }
    // Xử lý tải file đính kèm
    public byte[] downloadAttachment(Long emailId) throws IOException {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        if (email.getAttachmentPath() == null) {
            throw new RuntimeException("No attachment found");
        }

        Path filePath = Paths.get(email.getAttachmentPath());
        return Files.readAllBytes(filePath);
    }

    public String saveFileLocally(MultipartFile file) throws IOException {
        Path filePath = Paths.get("uploads/" + file.getOriginalFilename());
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, file.getBytes());
        return filePath.toAbsolutePath().toString();
    }


    public List<Email> getInbox(User receiver) {
        return emailRepository.findByReceiver(receiver);
    }
}
