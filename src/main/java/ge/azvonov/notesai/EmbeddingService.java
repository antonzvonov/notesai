package ge.azvonov.notesai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingModel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private static final int CHUNK_SIZE = 500;
    private final OpenAIClient openAIClient;

    public EmbeddingService(@Value("${openai.api.key}") String apiKey) {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public List<TextEmbedding> embedText(String content) {
        List<String> chunks = new ArrayList<>();
        for (int pos = 0; pos < content.length(); pos += CHUNK_SIZE) {
            String chunk = content.substring(pos, Math.min(pos + CHUNK_SIZE, content.length()));
            chunks.add(chunk);
        }

        List<List<Float>> vectors = fetchEmbeddings(chunks);
        List<TextEmbedding> result = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            result.add(new TextEmbedding(chunks.get(i), vectors.get(i)));
        }
        return result;
    }

    private List<List<Float>> fetchEmbeddings(List<String> texts) {
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .model(EmbeddingModel.TEXT_EMBEDDING_3_SMALL)
                .input(EmbeddingCreateParams.Input.ofArrayOfStrings(texts))
                .build();

        CreateEmbeddingResponse response = openAIClient.embeddings().create(params);
        List<Embedding> data = response.data();
        if (data.isEmpty()) {
            throw new IllegalStateException("OpenAI вернуло пустой список embedding-data");
        }
        return data.stream()
                .map(Embedding::embedding)
                .toList();
    }

    public record TextEmbedding(String text, List<Float> vector) {}
}
