package ge.azvonov.notesai.db;

import ge.azvonov.notesai.Project;
import ge.azvonov.notesai.AppUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUser(AppUser user);
}
