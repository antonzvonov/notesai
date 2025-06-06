package ge.azvonov.notesai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class NotesaiApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(NotesaiApplication.class, args);
    }
}
