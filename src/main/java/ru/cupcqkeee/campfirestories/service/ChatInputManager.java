package ru.cupcqkeee.campfirestories.service;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.cupcqkeee.campfirestories.CampfireStories;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;


public class ChatInputManager implements Listener {

    private static final String CANCEL_WORD = "отмена";

    private final CampfireStories plugin;
    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public ChatInputManager(CampfireStories plugin) {
        this.plugin = plugin;
    }

    public void prompt(Player player, String message, Consumer<String> callback) {
        pending.put(player.getUniqueId(), callback);
        player.closeInventory();
        player.sendMessage(message);
    }

    public boolean isWaiting(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public void cancel(Player player) {
        pending.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = pending.remove(player.getUniqueId());
        if (callback == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(text));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

    public static boolean isCancel(String input) {
        return input == null || input.isBlank() || input.equalsIgnoreCase(CANCEL_WORD);
    }
}
