package ru.cupcqkeee.campfirestories.storage;

import ru.cupcqkeee.campfirestories.CampfireStories;
import ru.cupcqkeee.campfirestories.story.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class StoryDatabase {

    private static final String PAGE_DELIMITER = "\u0001";

    private final CampfireStories plugin;
    private final File file;
    private Connection connection;

    public StoryDatabase(CampfireStories plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stories.db");
    }

    public synchronized void init() throws Exception {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Не удалось создать папку плагина");
        }
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS stories ("
                    + "location TEXT PRIMARY KEY,"
                    + "world TEXT NOT NULL,"
                    + "x INTEGER NOT NULL,"
                    + "y INTEGER NOT NULL,"
                    + "z INTEGER NOT NULL,"
                    + "owner TEXT NOT NULL,"
                    + "owner_name TEXT,"
                    + "title TEXT,"
                    + "pages TEXT,"
                    + "is_public INTEGER NOT NULL DEFAULT 0,"
                    + "created_at INTEGER NOT NULL,"
                    + "updated_at INTEGER NOT NULL"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS story_authors ("
                    + "location TEXT NOT NULL,"
                    + "uuid TEXT NOT NULL,"
                    + "name TEXT,"
                    + "PRIMARY KEY (location, uuid)"
                    + ")");
        }
    }

    public synchronized List<Story> loadAll() {
        Map<String, Story> stories = new LinkedHashMap<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM stories")) {
            while (rs.next()) {
                String key = rs.getString("location");
                List<String> pages = decodePages(rs.getString("pages"));
                Story story = new Story(
                        key,
                        rs.getString("world"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        parseUuid(rs.getString("owner")),
                        rs.getString("owner_name"),
                        rs.getString("title"),
                        pages,
                        rs.getInt("is_public") != 0,
                        rs.getLong("created_at"),
                        rs.getLong("updated_at"),
                        new LinkedHashMap<>()
                );
                stories.put(key, story);
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Ошибка чтения историй: " + ex.getMessage());
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT location, uuid, name FROM story_authors")) {
            while (rs.next()) {
                Story story = stories.get(rs.getString("location"));
                if (story == null) {
                    continue;
                }
                story.getAuthors().put(parseUuid(rs.getString("uuid")), rs.getString("name"));
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Ошибка чтения соавторов: " + ex.getMessage());
        }

        return new ArrayList<>(stories.values());
    }

    public synchronized void save(Story story) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO stories (location, world, x, y, z, owner, owner_name, title, pages, is_public, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, story.getKey());
                ps.setString(2, story.getWorldName());
                ps.setInt(3, story.getX());
                ps.setInt(4, story.getY());
                ps.setInt(5, story.getZ());
                ps.setString(6, story.getOwner().toString());
                ps.setString(7, story.getOwnerName());
                ps.setString(8, story.getTitle());
                ps.setString(9, encodePages(story.getPages()));
                ps.setInt(10, story.isPublic() ? 1 : 0);
                ps.setLong(11, story.getCreatedAt());
                ps.setLong(12, story.getUpdatedAt());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM story_authors WHERE location = ?")) {
                ps.setString(1, story.getKey());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO story_authors (location, uuid, name) VALUES (?, ?, ?)")) {
                for (Map.Entry<UUID, String> entry : story.getAuthors().entrySet()) {
                    ps.setString(1, story.getKey());
                    ps.setString(2, entry.getKey().toString());
                    ps.setString(3, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Ошибка сохранения истории: " + ex.getMessage());
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // ignore
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // ignore
            }
        }
    }

    public synchronized void delete(String key) {
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM stories WHERE location = ?")) {
                ps.setString(1, key);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM story_authors WHERE location = ?")) {
                ps.setString(1, key);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Ошибка удаления истории: " + ex.getMessage());
        }
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Ошибка закрытия базы данных: " + ex.getMessage());
        }
        connection = null;
    }

    private String encodePages(List<String> pages) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            if (i > 0) {
                sb.append(PAGE_DELIMITER);
            }
            String page = pages.get(i) == null ? "" : pages.get(i);
            sb.append(Base64.getEncoder().encodeToString(page.getBytes(StandardCharsets.UTF_8)));
        }
        return sb.toString();
    }

    private List<String> decodePages(String encoded) {
        List<String> pages = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return pages;
        }
        for (String part : encoded.split(PAGE_DELIMITER, -1)) {
            pages.add(new String(Base64.getDecoder().decode(part), StandardCharsets.UTF_8));
        }
        return pages;
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (Exception ex) {
            return new UUID(0L, 0L);
        }
    }
}
