package ge.azvonov.notesai.db;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;
import java.util.List;

@Service
public class SQLiteVectorService {
    private Connection connection;

    @PostConstruct
    public void init() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:notes.db");
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS file_metadata (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "extension TEXT NOT NULL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS file_chunks (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "file_id INTEGER NOT NULL, " +
                        "text TEXT NOT NULL, " +
                        "vector BLOB NOT NULL, " +
                        "FOREIGN KEY(file_id) REFERENCES file_metadata(id))");
                try {
                    st.execute("SELECT load_extension('vector0')");
                } catch (SQLException ignore) {
                    // Расширение может быть уже загружено или отсутствовать
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось инициализировать SQLite", e);
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

    public long saveFileMetadata(String name, String extension) {
        String sql = "INSERT INTO file_metadata(name, extension) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, extension);
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
}
