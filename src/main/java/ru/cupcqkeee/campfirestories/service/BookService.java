package ru.cupcqkeee.campfirestories.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import ru.cupcqkeee.campfirestories.CampfireStories;
import ru.cupcqkeee.campfirestories.story.Story;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class BookService implements Listener {

    private final CampfireStories plugin;
    private final Map<UUID, EditSession> sessions = new HashMap<>();

    public BookService(CampfireStories plugin) {
        this.plugin = plugin;
    }

    public void openEditor(Player player, Story story) {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            List<String> pages = story.getPages();
            if (pages.isEmpty()) {
                meta.addPage("");
            } else {
                meta.setPages(pages);
            }
            book.setItemMeta(meta);
        }

        PlayerInventory inventory = player.getInventory();
        int slot = inventory.getHeldItemSlot();
        ItemStack previous = inventory.getItem(slot);

        inventory.setItem(slot, book);
        sessions.put(player.getUniqueId(), new EditSession(story.getKey(), previous, slot));
        player.openBook(book);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f);
    }

    public void openReader(Player player, Story story) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle(story.getTitle());
            meta.setAuthor(story.getOwnerName());
            List<String> pages = story.getPages();
            if (!story.hasContent()) {
                meta.addPage(plugin.messages().get("story-empty"));
            } else {
                meta.setPages(pages);
            }
            book.setItemMeta(meta);
        }
        player.openBook(book);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        EditSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        event.setCancelled(true);

        Story story = plugin.storyManager().get(session.storyKey);
        if (story != null) {
            List<String> pages = new ArrayList<>(event.getNewBookMeta().getPages());
            int max = plugin.storyConfig().maxPages();
            if (pages.size() > max) {
                pages = new ArrayList<>(pages.subList(0, max));
            }
            story.setPages(pages);
            plugin.storyManager().save(story);
            plugin.messages().sendPrefixed(player, "story-saved", "{count}", String.valueOf(countNonEmpty(pages)));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
        }

        restoreItem(player, session, true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        EditSession session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            restoreItem(event.getPlayer(), session, false);
        }
    }

    private void restoreItem(Player player, EditSession session, boolean delayed) {
        Runnable task = () -> {
            PlayerInventory inventory = player.getInventory();
            if (session.slot < 0 || session.slot >= inventory.getSize()) {
                return;
            }
            inventory.setItem(session.slot, session.previous);
        };
        if (delayed && player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, task);
        } else {
            task.run();
        }
    }

    private int countNonEmpty(List<String> pages) {
        int count = 0;
        for (String page : pages) {
            if (page != null && !page.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private record EditSession(String storyKey, ItemStack previous, int slot) {
    }
}
