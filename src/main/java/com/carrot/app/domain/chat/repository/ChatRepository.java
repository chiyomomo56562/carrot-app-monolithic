package com.carrot.app.domain.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import com.carrot.app.domain.chat.document.ChatMessage;

public interface ChatRepository extends MongoRepository<ChatMessage, String> {
    Slice<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    @Query(value = "{ 'roomId' : ?0, 'senderId' : { $ne : ?1 }, 'isRead' : false }")
    @Update(value = "{ '$set' : { 'isRead' : true } }")
    void updateIsReadByRoomIdAndSenderIdNot(String roomId, Long userId);

    long countByRoomIdAndSenderIdNotAndIsReadFalse(String roomId, Long senderId);
}
