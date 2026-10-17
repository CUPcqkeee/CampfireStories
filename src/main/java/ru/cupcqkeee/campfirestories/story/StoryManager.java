package ru.cupcqkeee.campfirestories.story;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import ru.cupcqkeee.campfirestories.CampfireStories;
import ru.cupcqkeee.campfirestories.storage.StoryDatabase;
import ru.cupcqkeee.campfirestories.util.Loc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class StoryManager {

    private final CampfireStories plugin;
    private final StoryDatabase database;
    private final Map<String, Story> byKey = new HashMap<>();

    public StoryManager(CampfireStories plugin, StoryDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void loadAll() {
        byKey.clear();
        for (Story story : database.loadAll()) {
            byKey.put(story.getKey(), story);
        }
    }

    public Collection<Story> all() {
        return byKey.values();
    }

    public Story get(String key) {
        return byKey.get(key);
    }

    public Story get(Block block) {
        return byKey.get(Loc.key(block));
    }

    public Story get(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return byKey.get(Loc.key(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    public Story create(Block block, Player owner) {
        String key = Loc.key(block);
        Story existing = byKey.get(key);
        if (existing != null) {
            return existing;
        }
        long now = System.currentTimeMillis();
        Story story = new Story(key, block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                owner.getUniqueId(), owner.getName(), plugin.storyConfig().defaultTitle(),
                new ArrayList<>(), false, now, now, new HashMap<>());
        byKey.put(key, story);
        database.save(story.copy());
        return story;
    }

    public void save(Story story) {
        database.save(story.copy());
    }

    public void delete(Story story) {
        if (story == null) {
            return;
        }
        byKey.remove(story.getKey());
        database.delete(story.getKey());
    }

    public boolean isOwner(Story story, Player player) {
        return story.getOwner().equals(player.getUniqueId());
    }

    public boolean canEdit(Story story, Player player) {
        return isOwner(story, player)
                || story.isAuthor(player.getUniqueId())
                || player.hasPermission("campfirestories.admin");
    }

    public boolean canManage(Story story, Player player) {
        return isOwner(story, player) || player.hasPermission("campfirestories.admin");
    }

    public boolean canRead(Story story, Player player) {
        return story.isPublic() || canEdit(story, player) || player.hasPermission("campfirestories.admin");
    }

    public List<Story> storiesFor(Player player) {
        List<Story> result = new ArrayList<>();
        boolean authorsToo = !"owner".equalsIgnoreCase(plugin.storyConfig().listVisibility());
        for (Story story : byKey.values()) {
            if (isOwner(story, player)
                    || (authorsToo && story.isAuthor(player.getUniqueId()))
                    || player.hasPermission("campfirestories.admin")) {
                result.add(story);
            }
        }
        return result;
    }

    public Story findNearest(Player player) {
        Location loc = player.getLocation();
        double radius = plugin.storyConfig().manageRadius();
        double bestDistance = radius * radius;
        Story best = null;
        for (Story story : byKey.values()) {
            if (!story.getWorldName().equals(loc.getWorld().getName())) {
                continue;
            }
            Location storyLoc = story.getLocation();
            if (storyLoc == null) {
                continue;
            }
            double distance = storyLoc.distanceSquared(loc);
            if (distance <= bestDistance && canManage(story, player)) {
                bestDistance = distance;
                best = story;
            }
        }
        return best;
    }
}
