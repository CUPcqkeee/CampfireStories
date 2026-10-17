package ru.cupcqkeee.campfirestories.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import ru.cupcqkeee.campfirestories.CampfireStories;
import ru.cupcqkeee.campfirestories.story.Story;


public class InteractListener implements Listener {

    private final CampfireStories plugin;

    public InteractListener(CampfireStories plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !isCampfire(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        Story story = plugin.storyManager().get(block);

        boolean sneaking = player.isSneaking();
        boolean emptyHand = event.getItem() == null || event.getItem().getType() == Material.AIR;

        if (sneaking) {
            event.setCancelled(true);
            openStory(player, story, true);
        } else if (emptyHand) {
            event.setCancelled(true);
            openStory(player, story, false);
        }
    }

    private void openStory(Player player, Story story, boolean viaShift) {
        if (story == null) {
            plugin.messages().sendPrefixed(player, "campfire-not-story");
            return;
        }
        if (!plugin.storyManager().canRead(story, player)) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }
        if (plugin.onCooldown(player)) {
            plugin.messages().sendPrefixed(player, "cooldown");
            return;
        }

        if (viaShift && plugin.storyManager().canEdit(story, player)) {
            plugin.bookService().openEditor(player, story);
            plugin.messages().sendPrefixed(player, "editor-opened");
            player.playSound(player.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 1.0f, 1.2f);
            return;
        }

        plugin.bookService().openReader(player, story);
        plugin.messages().sendPrefixed(player, "reader-opened", "{title}", story.getTitle());
        notifyNearby(player, story);
    }

    private void notifyNearby(Player reader, Story story) {
        int radius = plugin.storyConfig().notifyRadius();
        if (radius <= 0) {
            return;
        }
        double radiusSquared = (double) radius * radius;
        Location origin = reader.getLocation();
        for (Player other : origin.getWorld().getPlayers()) {
            if (other.equals(reader)) {
                continue;
            }
            if (other.getLocation().distanceSquared(origin) <= radiusSquared) {
                other.sendActionBar(plugin.messages().component("read-notify",
                        "{player}", reader.getName(),
                        "{title}", story.getTitle()));
            }
        }
    }

    private boolean isCampfire(Material material) {
        return material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE;
    }
}
