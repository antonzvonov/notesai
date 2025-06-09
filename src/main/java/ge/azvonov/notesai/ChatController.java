package ge.azvonov.notesai;

import ge.azvonov.notesai.db.ChatMessageRepository;
import ge.azvonov.notesai.service.ChatService;
import ge.azvonov.notesai.service.EmbeddingService;
import ge.azvonov.notesai.service.EmbeddingService.TextEmbedding;
import ge.azvonov.notesai.db.VectorService;
import ge.azvonov.notesai.db.ProjectRepository;
import ge.azvonov.notesai.Project;
import ge.azvonov.notesai.db.UserRepository;
import ge.azvonov.notesai.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class ChatController {

    private final ChatMessageRepository repository;

    private final EmbeddingService embeddingService;

    private final VectorService vectorService;

    private final ChatService chatService;

    private final ProjectRepository projectRepository;

    private final UserRepository userRepository;

    @Autowired
    public ChatController(ChatMessageRepository repository,
                         EmbeddingService embeddingService,
                         VectorService vectorService,
                         ChatService chatService,
                         ProjectRepository projectRepository,
                         UserRepository userRepository) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.vectorService = vectorService;
        this.chatService = chatService;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    private AppUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        AppUser user = currentUser();
        List<ChatMessage> messages = repository.findByUser(user);
        model.addAttribute("messages", messages);
        List<Project> projects = projectRepository.findByUser(user);
        model.addAttribute("projects", projects);
        if (!projects.isEmpty()) {
            model.addAttribute("selectedProjectId", projects.get(0).getId());
        }
        return "chat";
    }

    @PostMapping("/chat")
    public String handleChat(@RequestParam("message") String message,
                             @RequestParam("projectId") Long projectId,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             Model model) throws IOException {
        AppUser user = currentUser();
        String fileName = "без файла";
        String fileContent = null;
        if (file != null && !file.isEmpty()) {
            fileName = file.getOriginalFilename();
            fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String ext = "";
            int dot = fileName.lastIndexOf('.');
            if (dot >= 0) {
                ext = fileName.substring(dot + 1);
            }
            long fileId = vectorService.saveFileMetadata(projectId, fileName, ext);
            List<TextEmbedding> textEmbeddings = embeddingService.embedText(fileContent);
            for (TextEmbedding chunk : textEmbeddings) {
                vectorService.saveTextChunk(fileId, chunk.text(), chunk.vector());
            }
            int i = 0;
        }

        String responseText = "Вы сказали: " + message + ", файл: " + fileName;
        String answer = "";

        if (StringUtils.hasLength(message)) {
            List<TextEmbedding> textEmbeddings = embeddingService.embedText(message);
            List<VectorService.SearchResult> context = vectorService.findTop10ByCosine(projectId, textEmbeddings.get(0).vector());
            List<String> chunks = context.stream().map(VectorService.SearchResult::text).toList();
            answer = chatService.askWithContext(chunks, message);
            int i = 0;
        }

        model.addAttribute("response", responseText);
        model.addAttribute("answer", answer);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(message);
        chatMessage.setResponse(answer);
        projectRepository.findById(projectId).filter(p -> p.getUser().equals(user)).ifPresent(chatMessage::setProject);
        chatMessage.setUser(user);
        repository.save(chatMessage);

        model.addAttribute("selectedProjectId", projectId);

        List<ChatMessage> messages = repository.findByUser(user);
        model.addAttribute("messages", messages);
        model.addAttribute("projects", projectRepository.findByUser(user));
        return "chat";
    }
}
