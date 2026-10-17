package ru.cupcqkeee.campfirestories.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.cupcqkeee.campfirestories.CampfireStories;
import ru.cupcqkeee.campfirestories.story.Story;
import ru.cupcqkeee.campfirestories.util.Loc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class CampfireCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "help", "info", "create", "edit", "read", "manage",
            "title", "public", "author", "delete", "list", "reload");

    private final CampfireStories plugin;

    public CampfireCommand(CampfireStories plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sendHelp(sender, label);
                return true;
            }
            Story target = target(player);
            if (target != null) {
                plugin.guiManager().openManage(player, target);
            } else {
                sendHelp(sender, label);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(sender, label);
            case "reload" -> reload(sender);
            case "list" -> list(sender);
            default -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().sendPrefixed(sender, "player-only");
                    return true;
                }
                switch (sub) {
                    case "info" -> info(player);
                    case "create" -> create(player);
                    case "edit" -> edit(player);
                    case "read" -> read(player);
                    case "manage" -> manage(player);
                    case "title" -> title(player, args);
                    case "public" -> publicState(player, args);
                    case "author" -> author(player, args);
                    case "delete" -> delete(player);
                    default -> sendHelp(sender, label);
                }
            }
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("campfirestories.reload") && !sender.hasPermission("campfirestories.admin")) {
            plugin.messages().sendPrefixed(sender, "no-permission");
            return;
        }
        plugin.reloadAll();
        plugin.messages().sendPrefixed(sender, "reload-success");
    }

    private void list(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().sendPrefixed(sender, "player-only");
            return;
        }
        if (!player.hasPermission("campfirestories.use")) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }
        List<Story> stories = plugin.storyManager().storiesFor(player);
        if (stories.isEmpty()) {
            plugin.messages().sendPrefixed(player, "no-stories");
            return;
        }
        plugin.messages().sendPrefixed(player, "list-header", "{count}", String.valueOf(stories.size()));
        for (Story story : stories) {
            player.sendMessage(plugin.messages().format("list-entry",
                    "{title}", story.getTitle(),
                    "{world}", story.getWorldName(),
                    "{x}", String.valueOf(story.getX()),
                    "{y}", String.valueOf(story.getY()),
                    "{z}", String.valueOf(story.getZ())));
        }
    }

    private void info(Player player) {
        Story story = target(player);
        if (story == null) {
            plugin.messages().sendPrefixed(player, "no-target");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + story.getTitle());
        player.sendMessage(ChatColor.YELLOW + "Владелец: " + ChatColor.WHITE + story.getOwnerName());
        player.sendMessage(ChatColor.YELLOW + "Соавторов: " + ChatColor.WHITE + story.getAuthors().size());
        player.sendMessage(ChatColor.YELLOW + "Публичная: " + ChatColor.WHITE + (story.isPublic() ? "да" : "нет"));
        player.sendMessage(ChatColor.YELLOW + "Страниц: " + ChatColor.WHITE + story.getPages().size());
        player.sendMessage(ChatColor.YELLOW + "Мир: " + ChatColor.WHITE + story.getWorldName()
                + ChatColor.GRAY + " " + story.getX() + ", " + story.getY() + ", " + story.getZ());
    }

    private void create(Player player) {
        if (!player.hasPermission("campfirestories.use")) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }
        Block block = player.getTargetBlockExact(6);
        if (block == null || !isCampfire(block.getType())) {
            plugin.messages().sendPrefixed(player, "no-target");
            return;
        }
        if (block.getType() == Material.SOUL_CAMPFIRE && !plugin.storyConfig().allowSoulCampfire()) {
            plugin.messages().sendPrefixed(player, "campfire-not-story");
            return;
        }
        if (plugin.storyManager().get(block) != null) {
            plugin.messages().sendPrefixed(player, "campfire-not-story");
            return;
        }
        plugin.storyManager().create(block, player);
        plugin.messages().sendPrefixed(player, "campfire-created");
    }

    private void edit(Player player) {
        Story story = target(player);
        if (story == null) {
            plugin.messages().sendPrefixed(player, "no-target");
            return;
        }
        if (!plugin.storyManager().canEdit(story, player)) {
            plugin.messages().sendPrefixed(player, "not-editable");
            return;
        }
        plugin.bookService().openEditor(player, story);
        plugin.messages().sendPrefixed(player, "editor-opened");
    }

    private void read(Player player) {
        Story story = target(player);
        if (story == null) {
            plugin.messages().sendPrefixed(player, "no-target");
            return;
        }
        if (!plugin.storyManager().canRead(story, player)) {
            plugin.messages().sendPrefixed(player, "not-editable");
            return;
        }
        plugin.bookService().openReader(player, story);
        plugin.messages().sendPrefixed(player, "reader-opened", "{title}", story.getTitle());
    }

    private void manage(Player player) {
        Story story = target(player);
        if (story == null) {
            plugin.messages().sendPrefixed(player, "no-target");
            return;
        }
        plugin.guiManager().openManage(player, story);
    }

    private void title(Player player, String[] args) {
        if (args.length < 2) {
            plugin.messages().sendPrefixed(player, "title-prompt");
            return;
        }
        Story story = target(player);
        if (story == null) {
            plugin.messages().sendPrefixed(player, "no-target");
            return;
        }
        if (!plugin.storyManager().canManage(story, player)) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }
        String title = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        int max = plugin.storyConfig().maxTitleLength();
        if (title.length() > max) {
            plugin.messages().sendPrefixed(player, "title-too-long", "{max}", String.valueOf(max));
            return;
        }
        story.setTitle(title);
        plugin.storyManager().save(story);
        plugin.messages().sendPrefixed(player, "title-set", "{title}", title);
    }

    private void publicState(Player player, String[] args) {
        Story story = target(player);
        if (story == null) {
            plugin.messages().sendPrefixed(player, "no-target");
            return;
        }
        if (!plugin.storyManager().canManage(story, player)) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }
        boolean value;
        if (args.length >= 2) {
            value = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true")
                    || args[1].equalsIgnoreCase("вкл") || args[1].equalsIgnoreCase("да");
        } else {
            value = !story.isPublic();
        }
        if (value && !plugin.storyConfig().allowPublicStories()) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }
        story.setPublic(value);
        plugin.storyManager().save(story);
        plugin.messages().sendPrefixed(player, value ? "public-on" : "public-off");
    }

    private void author(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "/" + "campfire author <add|remove> <игрок>");
            return;
        }
        Story story = target(player);
        if (story == null) {
            plugin.messages().sendPrefixed(player, "no-target");
            return;
        }
        if (!plugin.storyManager().canManage(story, player)) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        Player target = Bukkit.getPlayerExact(args[2]);

        if (action.equals("add")) {
            if (target == null) {
                plugin.messages().sendPrefixed(player, "author-not-online", "{player}", args[2]);
                return;
            }
            if (target.getUniqueId().equals(story.getOwner())) {
                plugin.messages().sendPrefixed(player, "author-owner");
                return;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                plugin.messages().sendPrefixed(player, "self-target");
                return;
            }
            if (story.isAuthor(target.getUniqueId())) {
                plugin.messages().sendPrefixed(player, "author-already", "{player}", target.getName());
                return;
            }
            if (story.getAuthors().size() >= plugin.storyConfig().maxAuthors()) {
                plugin.messages().sendPrefixed(player, "author-limit",
                        "{max}", String.valueOf(plugin.storyConfig().maxAuthors()));
                return;
            }
            story.addAuthor(target.getUniqueId(), target.getName());
            plugin.storyManager().save(story);
            plugin.messages().sendPrefixed(player, "author-added", "{player}", target.getName());
            plugin.messages().sendPrefixed(target, "author-added-notify", "{owner}", player.getName());
        } else if (action.equals("remove")) {
            if (target == null) {
                plugin.messages().sendPrefixed(player, "author-not-online", "{player}", args[2]);
                return;
            }
            if (!story.isAuthor(target.getUniqueId())) {
                plugin.messages().sendPrefixed(player, "author-not-author", "{player}", target.getName());
                return;
            }
            story.removeAuthor(target.getUniqueId());
            plugin.storyManager().save(story);
            plugin.messages().sendPrefixed(player, "author-removed", "{player}", target.getName());
        } else {
            player.sendMessage(ChatColor.YELLOW + "/campfire author <add|remove> <игрок>");
        }
    }

    private void delete(Player player) {
        Story story = target(player);
        if (story == null) {
            plugin.messages().sendPrefixed(player, "no-target");
            return;
        }
        if (!plugin.storyManager().canManage(story, player)) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }
        plugin.storyManager().delete(story);
        plugin.messages().sendPrefixed(player, "story-deleted");
    }

    private Story target(Player player) {
        Block block = player.getTargetBlockExact(6);
        if (block != null && isCampfire(block.getType())) {
            Story story = plugin.storyManager().get(block);
            if (story != null) {
                return story;
            }
        }
        return plugin.storyManager().findNearest(player);
    }

    private boolean isCampfire(Material material) {
        return material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "CampfireStories " + ChatColor.GRAY + "| " + ChatColor.YELLOW + "by CUPcqkeee");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " " + ChatColor.WHITE + "manage " + ChatColor.GRAY + "- открыть меню истории");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " " + ChatColor.WHITE + "edit/read " + ChatColor.GRAY + "- писать/читать историю");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " " + ChatColor.WHITE + "title <текст> " + ChatColor.GRAY + "- изменить название");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " " + ChatColor.WHITE + "public <on|off> " + ChatColor.GRAY + "- публичность");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " " + ChatColor.WHITE + "author <add|remove> <игрок>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " " + ChatColor.WHITE + "create " + ChatColor.GRAY + "- привязать костёр к истории");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " " + ChatColor.WHITE + "info/list/delete/reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(prefix)) {
                    result.add(sub);
                }
            }
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("author")) {
            for (String option : Arrays.asList("add", "remove")) {
                if (option.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(option);
                }
            }
            return result;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("author")) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    result.add(online.getName());
                }
            }
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("public")) {
            for (String option : Arrays.asList("on", "off")) {
                if (option.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(option);
                }
            }
        }
        return result;
    }
}
