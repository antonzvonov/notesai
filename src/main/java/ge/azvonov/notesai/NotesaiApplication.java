package ge.azvonov.notesai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NotesaiApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(NotesaiApplication.class, args);
    }
}
