package ge.azvonov.notesai;

import ge.azvonov.notesai.db.ProjectRepository;
import ge.azvonov.notesai.db.UserRepository;
import ge.azvonov.notesai.db.VectorService;
import ge.azvonov.notesai.db.FileMetadata;
import ge.azvonov.notesai.AppUser;
import ge.azvonov.notesai.service.EmbeddingService;
import ge.azvonov.notesai.service.EmbeddingService.TextEmbedding;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectRepository repository;
    private final UserRepository userRepository;
    private final VectorService vectorService;
    private final EmbeddingService embeddingService;

    @Autowired
    public ProjectController(ProjectRepository repository,
                             UserRepository userRepository,
                             VectorService vectorService,
                             EmbeddingService embeddingService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.vectorService = vectorService;
        this.embeddingService = embeddingService;
    }

    private AppUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String list(Model model) {
        AppUser user = currentUser();
        List<Project> projects = repository.findByUser(user);
        model.addAttribute("projects", projects);
        Map<Long, List<FileMetadata>> files = new HashMap<>();
        for (Project p : projects) {
            files.put(p.getId(), vectorService.listFiles(p.getId()));
        }
        model.addAttribute("files", files);
        return "projects";
    }

    @PostMapping("/add")
    public String add(@RequestParam("name") String name) {
        if (StringUtils.hasText(name)) {
            Project p = new Project();
            p.setName(name);
            p.setUser(currentUser());
            repository.save(p);
        }
        return "redirect:/projects";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") Long id) {
        AppUser user = currentUser();
        repository.findById(id).filter(p -> p.getUser().equals(user)).ifPresent(repository::delete);
        return "redirect:/projects";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam Long projectId, @RequestParam("file") MultipartFile file) throws IOException {
        AppUser user = currentUser();
        Project project = repository.findById(projectId).orElse(null);
        if (project == null || !project.getUser().equals(user) || file.isEmpty()) {
            return "redirect:/projects";
        }
        String fileName = file.getOriginalFilename();
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            ext = fileName.substring(dot + 1);
        }
        long fileId = vectorService.saveFileMetadata(projectId, fileName, ext);
        String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        List<TextEmbedding> textEmbeddings = embeddingService.embedText(fileContent);
        for (TextEmbedding chunk : textEmbeddings) {
            vectorService.saveTextChunk(fileId, chunk.text(), chunk.vector());
        }
        return "redirect:/projects";
    }

    @PostMapping("/delete-file")
    public String deleteFile(@RequestParam Long projectId, @RequestParam Long fileId) {
        AppUser user = currentUser();
        Project project = repository.findById(projectId).orElse(null);
        if (project != null && project.getUser().equals(user)) {
            vectorService.deleteFile(fileId);
        }
        return "redirect:/projects";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        AppUser user = currentUser();
        Project p = repository.findById(id).orElseThrow();
        if (!p.getUser().equals(user)) {
            return "redirect:/projects";
        }
        model.addAttribute("project", p);
        return "edit_project";
    }

    @PostMapping("/edit")
    public String edit(@RequestParam Long id, @RequestParam String name) {
        AppUser user = currentUser();
        Project p = repository.findById(id).orElseThrow();
        if (p.getUser().equals(user)) {
            p.setName(name);
            repository.save(p);
        }
        return "redirect:/projects";
    }
}
