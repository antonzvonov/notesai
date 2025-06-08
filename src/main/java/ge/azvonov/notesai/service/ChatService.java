package ge.azvonov.notesai.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final OpenAIClient client;

    public ChatService(@Value("${openai.api.key}") String apiKey) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Отправляет в OpenAI Chat API системный и пользовательский промты,
     * где контекст — ровно те 10 фрагментов, что передали.
     * Если ответа нет в контексте — модель вернёт "I don't know."
     */
    public String askWithContext(List<String> contextChunks, String userQuestion) {
        // 1) Формируем «блок» контекста
        String contextBlock = contextChunks.stream()
                .map(chunk -> "- " + chunk)
                .collect(Collectors.joining("\n"));

        // 2) Жёсткий системный промт
        String systemPrompt = """
            You are a helpful assistant.
            Answer the user's question using strictly ONLY the information provided in the context.
            If the answer is not contained in the context or context is empty, reply "Я не знаю."
            """;

        // 3) Промт от пользователя с контекстом и собственно вопросом
        String userPrompt = "Context:\n" + contextBlock +
                "\n\nQuestion: " + userQuestion;

        // 4) Собираем параметры
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_3_5_TURBO)
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt)
                .build();

        // 5) Делаем запрос
        ChatCompletion completion = client
                .chat()
                .completions()
                .create(params);

        // 6) Берём первую choice и возвращаем её .message().content()
        ChatCompletion.Choice first = completion.choices().get(0);
        return first.message().content().get();
    }
}
