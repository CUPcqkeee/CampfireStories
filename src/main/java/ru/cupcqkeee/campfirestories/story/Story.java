package ru.cupcqkeee.campfirestories.story;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class Story {

    private final String key;
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    private final UUID owner;
    private String ownerName;

    private String title;
    private List<String> pages;
    private boolean publicStory;

    private final long createdAt;
    private long updatedAt;

    /** UUID соавтора -> последнее известное имя. */
    private final Map<UUID, String> authors;

    public Story(String key, String world, int x, int y, int z,
                 UUID owner, String ownerName, String title,
                 List<String> pages, boolean publicStory,
                 long createdAt, long updatedAt, Map<UUID, String> authors) {
        this.key = key;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.owner = owner;
        this.ownerName = ownerName == null ? "unknown" : ownerName;
        this.title = title == null || title.isEmpty() ? "История" : title;
        this.pages = pages == null ? new ArrayList<>() : new ArrayList<>(pages);
        this.publicStory = publicStory;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.authors = authors == null ? new LinkedHashMap<>() : new LinkedHashMap<>(authors);
    }

    public String getKey() {
        return key;
    }

    public String getWorldName() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Location getLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) {
            return null;
        }
        return new Location(w, x, y, z);
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        touch();
    }

    public List<String> getPages() {
        return pages;
    }

    public void setPages(List<String> pages) {
        this.pages = pages == null ? new ArrayList<>() : new ArrayList<>(pages);
        touch();
    }

    public boolean isPublic() {
        return publicStory;
    }

    public void setPublic(boolean publicStory) {
        this.publicStory = publicStory;
        touch();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public Map<UUID, String> getAuthors() {
        return authors;
    }

    public boolean isAuthor(UUID uuid) {
        return authors.containsKey(uuid);
    }

    public void addAuthor(UUID uuid, String name) {
        authors.put(uuid, name);
        touch();
    }

    public void removeAuthor(UUID uuid) {
        authors.remove(uuid);
        touch();
    }

    private void touch() {
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean hasContent() {
        for (String page : pages) {
            if (page != null && !page.isBlank()) {
                return true;
            }
        }
        return false;
    }

    public Story copy() {
        return new Story(key, world, x, y, z, owner, ownerName, title,
                new ArrayList<>(pages), publicStory, createdAt, updatedAt,
                new LinkedHashMap<>(authors));
    }
}
