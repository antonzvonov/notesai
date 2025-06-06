package ge.azvonov.notesai;


import com.openai.client.OpenAI;
import com.openai.client.embeddings.EmbeddingsRequest;
import com.openai.client.embeddings.EmbeddingsResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private static final int CHUNK_SIZE = 500;

    private final OpenAI openAI;

    public EmbeddingService(@Value("${openai.api.key}") String apiKey) {
        this.openAI = new OpenAI(apiKey);

    }

    public List<TextEmbedding> embedText(String content) {
        List<TextEmbedding> result = new ArrayList<>();
        for (int pos = 0; pos < content.length(); pos += CHUNK_SIZE) {
            String chunk = content.substring(pos, Math.min(pos + CHUNK_SIZE, content.length()));
            List<Double> vector = fetchEmbedding(chunk);
            result.add(new TextEmbedding(chunk, vector));
        }
        return result;
    }

    private List<Double> fetchEmbedding(String text) {

        EmbeddingsRequest request = EmbeddingsRequest.builder()
                .model("text-embedding-ada-002")
                .input(List.of(text))
                .build();
        EmbeddingsResponse response = openAI.embeddings().create(request);

        return response.getData().get(0).getEmbedding();
    }

    public record TextEmbedding(String text, List<Double> vector) {}
}
