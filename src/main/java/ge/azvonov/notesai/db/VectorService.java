package ge.azvonov.notesai.db;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.postgresql.util.PGobject;
import ge.azvonov.notesai.db.FileMetadata;

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
            ps.setObject(3, toPgVector(vector));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения текста", e);
        }
    }

    public void saveAudioCaption(long fileId, double start, double end, String speaker, String text) {
        String sql = "INSERT INTO audio_caption(file_id, start_time, end_time, speaker, text) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, fileId);
            ps.setDouble(2, start);
            ps.setDouble(3, end);
            ps.setString(4, speaker);
            ps.setString(5, text);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения титров", e);
        }
    }

    private PGobject toPgVector(List<Float> vector) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType("vector");
        obj.setValue(toVectorString(vector));
        return obj;
    }

    private String toVectorString(List<Float> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(vector.get(i));
        }
        sb.append(']');
        return sb.toString();
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
        String sql = "SELECT fc.id, fc.text, 1 - (fc.vector <=> ?::vector) AS score " +
                "FROM file_chunks fc " +
                "JOIN file_metadata fm ON fc.file_id = fm.id " +
                "WHERE fm.project_id = ? " +
                "ORDER BY fc.vector <=> ?::vector " +
                "LIMIT 10";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            PGobject vec = toPgVector(queryVector);
            ps.setObject(1, vec);
            ps.setLong(2, projectId);
            ps.setObject(3, vec);
            try (ResultSet rs = ps.executeQuery()) {
                List<SearchResult> result = new ArrayList<>();
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String text = rs.getString("text");
                    double score = rs.getDouble("score");
                    result.add(new SearchResult(id, text, score));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска по косинусу", e);
        }
    }

    /**
     * Возвращает список файлов проекта.
     */
    public List<FileMetadata> listFiles(long projectId) {
        String sql = "SELECT id, name, extension FROM file_metadata WHERE project_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                List<FileMetadata> files = new ArrayList<>();
                while (rs.next()) {
                    files.add(new FileMetadata(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("extension")));
                }
                return files;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения файлов", e);
        }
    }

    /**
     * Удаляет файл и его фрагменты.
     */
    public void deleteFile(long fileId) {
        try (PreparedStatement ps1 = connection.prepareStatement("DELETE FROM file_chunks WHERE file_id = ?");
             PreparedStatement ps2 = connection.prepareStatement("DELETE FROM file_metadata WHERE id = ?")) {
            ps1.setLong(1, fileId);
            ps1.executeUpdate();
            ps2.setLong(1, fileId);
            ps2.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления файла", e);
        }
    }

}
