package com.project.chatApp.service;

import com.project.chatApp.entity.Message;
import com.project.chatApp.repository.MessageRepository;
import com.project.chatApp.repository.MessageRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    public MessageRepository messageRepository;

    @Autowired
    public MessageRepositoryImpl messageRepositoryImpl;

    public List<Message> getAllMessages(String senderId, String receiverId) {
        List<Message> messages = messageRepository.getMessagesOfUsersChat(senderId, receiverId);
        return messages;
    }

    public List<Message> getNewMessages(String receiverId) {
        List<Message> messages = messageRepository.getNewMessages(receiverId);
        return messages;
    }

    public void updateNewMessages(String receiverId, String status) {
        messageRepositoryImpl.updateNewMessages(receiverId, status);
    }

    public void addMessage(Message message) {
        messageRepository.save(message);
    }

}
