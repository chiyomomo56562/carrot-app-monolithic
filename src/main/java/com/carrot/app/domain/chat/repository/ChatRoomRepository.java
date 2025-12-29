package com.carrot.app.domain.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import com.carrot.app.domain.chat.document.ChatRoom;

import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);

    Slice<ChatRoom> findBySellerIdOrBuyerIdOrderByLastMessageSentAtDesc(Long sellerId, Long buyerId, Pageable pageable);
}
