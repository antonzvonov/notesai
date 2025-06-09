package ge.azvonov.notesai.db;

import ge.azvonov.notesai.ChatMessage;
import ge.azvonov.notesai.AppUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUser(AppUser user);

    List<ChatMessage> findByUserAndProjectIdOrderByTimestamp(AppUser user, Long projectId);
}
