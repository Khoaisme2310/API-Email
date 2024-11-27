package com.example.emailapp.repository;

import com.example.emailapp.model.Email;
import com.example.emailapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailRepository extends JpaRepository<Email, Long> {
    List<Email> findByReceiver(User receiver);
}
