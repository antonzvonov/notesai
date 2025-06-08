package ge.azvonov.notesai.db;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class VectorService {
    private final DataSource dataSource;
    private Connection connection;

    @Autowired
    public VectorService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось открыть соединение", e);
        }
    }

    @PreDestroy
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    public long saveFileMetadata(long projectId, String name, String extension) {
        String sql = "INSERT INTO file_metadata(project_id, name, extension) VALUES(?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, projectId);
            ps.setString(2, name);
            ps.setString(3, extension);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("ID not returned");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения метаданных", e);
        }
    }

    public void saveTextChunk(long fileId, String text, List<Float> vector) {
        String sql = "INSERT INTO file_chunks(file_id, text, vector) VALUES(?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, fileId);
            ps.setString(2, text);
            ps.setBytes(3, toByteArray(vector));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения текста", e);
        }
    }

    private byte[] toByteArray(List<Float> vector) {
        ByteBuffer buffer = ByteBuffer.allocate(4 * vector.size());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : vector) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    /**
     * Результат поиска ближайших фрагментов.
     */
    public record SearchResult(long id, String text, double score) {}

    /**
     * Ищет топ-10 фрагментов в таблице file_chunks по косинусному сходству
     * к переданному вектору queryVector.
     */
    public List<SearchResult> findTop10ByCosine(long projectId, List<Float> queryVector) {
        // вычисляем норму вектора запроса
        double normQ = Math.sqrt(queryVector.stream()
                .mapToDouble(f -> f * f)
                .sum());

        String sql = "SELECT fc.id, fc.text, fc.vector FROM file_chunks fc " +
                "JOIN file_metadata fm ON fc.file_id = fm.id " +
                "WHERE fm.project_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                // используем минимальную кучу на 10 элементов
                PriorityQueue<SearchResult> pq = new PriorityQueue<>(
                        Comparator.comparingDouble(SearchResult::score)
                );

                while (rs.next()) {
                    long id      = rs.getLong("id");
                    String text  = rs.getString("text");
                    byte[] blob  = rs.getBytes("vector");
                    // парсим BLOB обратно в List<Float>
                    List<Float> vec = toFloatList(blob);

                    // скалярное произведение и норма вектора из БД
                    double dot = 0, normV = 0;
                    for (int i = 0; i < vec.size(); i++) {
                        float v = vec.get(i);
                        dot     += queryVector.get(i) * v;
                        normV   += v * v;
                    }
                    normV = Math.sqrt(normV);

                    double cosine = dot / (normQ * normV);
                    SearchResult res = new SearchResult(id, text, cosine);

                    pq.add(res);
                    if (pq.size() > 10) {
                        pq.poll(); // удаляем наименьший
                    }
                }

                // собираем из кучи список в порядке убывания score
                List<SearchResult> top10 = new ArrayList<>(pq);
                top10.sort(Comparator.comparingDouble(SearchResult::score).reversed());
                return top10;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска по косинусу", e);
        }
    }

    /**
     * Парсит BLOB little‐endian обратно в List<Float>.
     */
    private List<Float> toFloatList(byte[] blob) {
        var buf = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
        List<Float> list = new ArrayList<>(blob.length / 4);
        while (buf.remaining() >= 4) {
            list.add(buf.getFloat());
        }
        return list;
    }

}
