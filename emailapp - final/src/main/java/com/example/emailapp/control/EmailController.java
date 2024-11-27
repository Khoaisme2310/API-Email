package com.example.emailapp.control;

import com.example.emailapp.model.Email;
import com.example.emailapp.model.User;
import com.example.emailapp.service.EmailService;
import com.example.emailapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    @PostMapping("/send")
    public ResponseEntity<String> sendInternalEmail(@RequestBody EmailRequest emailRequest) {
        try {
            User sender = userService.findByUsername(emailRequest.getSenderUsername())
                    .orElseThrow(() -> new RuntimeException("Sender not found: " + emailRequest.getSenderUsername()));

            User receiver = userService.findByUsername(emailRequest.getReceiverUsername())
                    .orElseThrow(() -> new RuntimeException("Receiver not found: " + emailRequest.getReceiverUsername()));

            emailService.sendInternalEmail(sender, receiver, emailRequest.getSubject(), emailRequest.getContent());

            return ResponseEntity.ok("Internal email sent successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }

    @PostMapping("/sendgmail")
    public ResponseEntity<String> sendExternalEmail(@RequestBody EmailRequest emailRequest) {
        try {
            User sender = userService.findByUsername(emailRequest.getSenderUsername())
                    .orElseThrow(() -> new RuntimeException("Sender not found: " + emailRequest.getSenderUsername()));

            String receiverEmail = emailRequest.getReceiverUsername();
            if (!isEmail(receiverEmail)) {
                throw new RuntimeException("Invalid receiver email address: " + receiverEmail);
            }

            emailService.sendExternalEmail(sender, receiverEmail, emailRequest.getSubject(), emailRequest.getContent());

            return ResponseEntity.ok("External email sent successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }

    @PostMapping("/sendfile")
    public ResponseEntity<String> sendEmailWithAttachment(
            @RequestParam("senderUsername") String senderUsername,
            @RequestParam("receiverUsername") String receiverUsername,
            @RequestParam("subject") String subject,
            @RequestParam("content") String content,
            @RequestParam("file") MultipartFile file) {
        try {
            User sender = userService.findByUsername(senderUsername)
                    .orElseThrow(() -> new RuntimeException("Sender not found: " + senderUsername));

            if (isEmail(receiverUsername)) {
                emailService.sendExternalEmailWithAttachment(sender, receiverUsername, subject, content, file);
            } else {
                User receiver = userService.findByUsername(receiverUsername)
                        .orElseThrow(() -> new RuntimeException("Receiver not found: " + receiverUsername));
                emailService.sendInternalEmailWithAttachment(sender, receiver, subject, content, file);
            }

            return ResponseEntity.ok("Email with attachment sent successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Failed to send email with attachment: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isEmail(String input) {
        return EMAIL_PATTERN.matcher(input).matches();
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<EmailRequest>> getInbox(@RequestParam String receiverUsername) {
        try {
            User receiver = userService.findByUsername(receiverUsername)
                    .orElseThrow(() -> new RuntimeException("Receiver not found: " + receiverUsername));

            // Lấy danh sách email từ service
            List<Email> emails = emailService.getInbox(receiver);

            // Chuyển đổi email sang DTO EmailRequest
            List<EmailRequest> emailRequests = emails.stream()
                    .map(email -> {
                        EmailRequest emailRequest = new EmailRequest();
                        emailRequest.setSenderUsername(email.getSender().getUsername());
                        emailRequest.setReceiverUsername(email.getReceiver().getUsername());
                        emailRequest.setSubject(email.getSubject());
                        emailRequest.setContent(email.getContent());
                        emailRequest.setAttachmentName(email.getAttachmentName()); // Lưu tên tệp đính kèm
                        emailRequest.setHasAttachment(email.getAttachmentName() != null);  // Kiểm tra có tệp đính kèm hay không
                        return emailRequest;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(emailRequests);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
    @GetMapping("/download/{emailId}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long emailId) {
        try {
            byte[] fileContent = emailService.downloadAttachment(emailId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file\"")
                    .body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    public static class EmailRequest {
        private String senderUsername;
        private String receiverUsername;
        private String subject;
        private String content;
        private String attachmentName; // Tên tệp đính kèm
        private boolean hasAttachment; // Có tệp đính kèm không

        // Getters và setters
        public String getSenderUsername() {
            return senderUsername;
        }

        public void setSenderUsername(String senderUsername) {
            this.senderUsername = senderUsername;
        }

        public String getReceiverUsername() {
            return receiverUsername;
        }

        public void setReceiverUsername(String receiverUsername) {
            this.receiverUsername = receiverUsername;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getAttachmentName() {
            return attachmentName;
        }

        public void setAttachmentName(String attachmentName) {
            this.attachmentName = attachmentName;
        }

        public boolean isHasAttachment() {
            return hasAttachment;
        }

        public void setHasAttachment(boolean hasAttachment) {
            this.hasAttachment = hasAttachment;
        }
    }
}
