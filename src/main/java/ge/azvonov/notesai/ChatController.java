package ge.azvonov.notesai;

import ge.azvonov.notesai.db.ChatMessageRepository;
import ge.azvonov.notesai.service.ChatService;
import ge.azvonov.notesai.service.EmbeddingService;
import ge.azvonov.notesai.service.EmbeddingService.TextEmbedding;
import ge.azvonov.notesai.db.SQLiteVectorService;
import ge.azvonov.notesai.db.ProjectRepository;
import ge.azvonov.notesai.Project;
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

    private final SQLiteVectorService sqLiteVectorService;

    private final ChatService chatService;

    private final ProjectRepository projectRepository;

    @Autowired
    public ChatController(ChatMessageRepository repository,
                         EmbeddingService embeddingService,
                         SQLiteVectorService sqLiteVectorService,
                         ChatService chatService,
                         ProjectRepository projectRepository) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.sqLiteVectorService = sqLiteVectorService;
        this.chatService = chatService;
        this.projectRepository = projectRepository;
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        List<ChatMessage> messages = repository.findAll();
        model.addAttribute("messages", messages);
        model.addAttribute("projects", projectRepository.findAll());
        return "chat";
    }

    @PostMapping("/chat")
    public String handleChat(@RequestParam("message") String message,
                             @RequestParam("projectId") Long projectId,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             Model model) throws IOException {
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
            long fileId = sqLiteVectorService.saveFileMetadata(fileName, ext);
            List<TextEmbedding> textEmbeddings = embeddingService.embedText(fileContent);
            for (TextEmbedding chunk : textEmbeddings) {
                sqLiteVectorService.saveTextChunk(fileId, chunk.text(), chunk.vector());
            }
            int i = 0;
        }

        String responseText = "Вы сказали: " + message + ", файл: " + fileName;
        String answer = "";

        if (StringUtils.hasLength(message)) {
            List<TextEmbedding> textEmbeddings = embeddingService.embedText(message);
            List<SQLiteVectorService.SearchResult> context = sqLiteVectorService.findTop10ByCosine(textEmbeddings.get(0).vector());
            List<String> chunks = context.stream().map(SQLiteVectorService.SearchResult::text).toList();
            answer = chatService.askWithContext(chunks, message);
            int i = 0;
        }

        model.addAttribute("response", responseText);
        model.addAttribute("fileContent", answer);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(message);
        chatMessage.setResponse(answer);
        projectRepository.findById(projectId).ifPresent(chatMessage::setProject);
        repository.save(chatMessage);

        List<ChatMessage> messages = repository.findAll();
        model.addAttribute("messages", messages);
        model.addAttribute("projects", projectRepository.findAll());
        return "chat";
    }
}
