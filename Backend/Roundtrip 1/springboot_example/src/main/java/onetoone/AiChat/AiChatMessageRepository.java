package onetoone.AiChat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    /** Fetch all AI chat history for a specific user, ordered oldest-first */
    List<AiChatMessage> findByUserIdOrderBySentAtAsc(Long userId);
}