package ru.cupcqkeee.campfirestories;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Messages {

    private final CampfireStories plugin;
    private final Map<String, String> cache = new HashMap<>();

    public Messages(CampfireStories plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        cache.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("messages");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(true)) {
            if (section.isConfigurationSection(key)) {
                continue;
            }
            String value = section.getString(key);
            if (value != null) {
                cache.put(key, color(value));
            }
        }
    }

    public String get(String key) {
        return cache.getOrDefault(key, color("&cОтсутствует сообщение: " + key));
    }

    public String format(String key, String... replacements) {
        String message = get(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }

    public String prefixedFormat(String key, String... replacements) {
        return get("prefix") + format(key, replacements);
    }

    public void send(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(format(key, replacements));
    }

    public void sendPrefixed(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(prefixedFormat(key, replacements));
    }

    public @NotNull TextComponent component(String key, String... replacements) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(format(key, replacements));
    }

    public List<String> getList(String key) {
        List<String> raw = plugin.getConfig().getStringList("messages." + key);
        List<String> result = new ArrayList<>();
        for (String line : raw) {
            result.add(color(line));
        }
        return result;
    }

    public List<String> formatList(String key, String... replacements) {
        List<String> result = new ArrayList<>();
        for (String line : getList(key)) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                line = line.replace(replacements[i], replacements[i + 1]);
            }
            result.add(line);
        }
        return result;
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
