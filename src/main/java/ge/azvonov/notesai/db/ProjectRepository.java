package ge.azvonov.notesai.db;

import ge.azvonov.notesai.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
