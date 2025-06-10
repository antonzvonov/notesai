package ge.azvonov.notesai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AudioProcessingService {

    @Value("${python.cmd:python3}")
    private String pythonCmd;

    @Async
    public void process(long fileId, String path) {
        ProcessBuilder pb = new ProcessBuilder(pythonCmd,
                "src/main/python/process_audio.py",
                Long.toString(fileId),
                path);
        pb.inheritIO();
        try {
            pb.start();
        } catch (IOException e) {
            // logging can be added here
            e.printStackTrace();
        }
    }
}
