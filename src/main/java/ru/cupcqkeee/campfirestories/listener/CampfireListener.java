package ru.cupcqkeee.campfirestories.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import ru.cupcqkeee.campfirestories.CampfireStories;
import ru.cupcqkeee.campfirestories.story.Story;


public class CampfireListener implements Listener {

    private final CampfireStories plugin;

    public CampfireListener(CampfireStories plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!isCampfire(block.getType())) {
            return;
        }
        if (block.getType() == Material.SOUL_CAMPFIRE && !plugin.storyConfig().allowSoulCampfire()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("campfirestories.use")) {
            return;
        }
        Story existing = plugin.storyManager().get(block);
        if (existing != null) {
            return;
        }
        plugin.storyManager().create(block, player);
        plugin.messages().sendPrefixed(player, "campfire-created");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isCampfire(block.getType())) {
            return;
        }
        Story story = plugin.storyManager().get(block);
        if (story == null) {
            return;
        }
        Player player = event.getPlayer();

        if (plugin.storyConfig().protectCampfire() && !plugin.storyManager().canManage(story, player)) {
            event.setCancelled(true);
            plugin.messages().sendPrefixed(player, "campfire-protected");
            return;
        }

        if (plugin.storyConfig().deleteOnBreak()) {
            plugin.storyManager().delete(story);
            if (!story.getOwner().equals(player.getUniqueId())) {
                plugin.messages().sendPrefixed(player, "campfire-broken");
            } else {
                plugin.messages().sendPrefixed(player, "campfire-removed");
            }
        }
    }

    private boolean isCampfire(Material material) {
        return material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE;
    }
}
