package ge.azvonov.notesai;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class ChatController {

    @GetMapping("/chat")
    public String chat() {
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
        model.addAttribute("response", "Вы сказали: " + message + ", файл: " + fileName);
        model.addAttribute("fileContent", fileContent);
        return "chat";
    }
}
