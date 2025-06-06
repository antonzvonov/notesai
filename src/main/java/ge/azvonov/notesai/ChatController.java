package ge.azvonov.notesai;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import ge.azvonov.notesai.ChatMessage;
import ge.azvonov.notesai.ChatMessageRepository;

import java.util.List;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class ChatController {

    private final ChatMessageRepository repository;

    @Autowired
    public ChatController(ChatMessageRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        List<ChatMessage> messages = repository.findAll();
        model.addAttribute("messages", messages);
        return "chat";
    }

    @PostMapping("/chat")
    public String handleChat(@RequestParam("message") String message,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             Model model) throws IOException {
        String fileName = "без файла";
        String fileContent = null;
        if (file != null && !file.isEmpty()) {
            fileName = file.getOriginalFilename();
            fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        String responseText = "Вы сказали: " + message + ", файл: " + fileName;
        model.addAttribute("response", responseText);
        model.addAttribute("fileContent", fileContent);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(message);
        chatMessage.setResponse(responseText);
        repository.save(chatMessage);

        List<ChatMessage> messages = repository.findAll();
        model.addAttribute("messages", messages);
        return "chat";
    }
}
