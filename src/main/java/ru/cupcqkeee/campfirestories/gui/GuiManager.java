package ru.cupcqkeee.campfirestories.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.cupcqkeee.campfirestories.CampfireStories;
import ru.cupcqkeee.campfirestories.service.ChatInputManager;
import ru.cupcqkeee.campfirestories.story.Story;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class GuiManager implements Listener {

    private static final int SIZE_MANAGE = 27;
    private static final int SIZE_AUTHORS = 54;

    private final CampfireStories plugin;

    public GuiManager(CampfireStories plugin) {
        this.plugin = plugin;
    }

    public void openManage(Player player, Story story) {
        if (!plugin.storyManager().canManage(story, player)) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }
        GuiHolder holder = new GuiHolder(GuiHolder.Type.MANAGE, story);
        Inventory inv = Bukkit.createInventory(holder, SIZE_MANAGE,
                plugin.messages().format("gui-title-manage", "{title}", story.getTitle()));
        holder.setInventory(inv);

        fillPanes(inv);

        inv.setItem(4, item(Material.WRITABLE_BOOK, plugin.messages().get("gui-edit"),
                plugin.messages().getList("gui-edit-lore")));
        inv.setItem(10, item(Material.NAME_TAG, plugin.messages().get("gui-title-item"),
                plugin.messages().formatList("gui-title-item-lore", "{title}", story.getTitle())));
        inv.setItem(14, publicItem(story));
        inv.setItem(16, item(Material.PLAYER_HEAD, plugin.messages().get("gui-authors"),
                plugin.messages().getList("gui-authors-lore")));
        inv.setItem(22, item(Material.BARRIER, plugin.messages().get("gui-delete"),
                plugin.messages().getList("gui-delete-lore")));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
    }

    public void openAuthors(Player player, Story story) {
        if (!plugin.storyManager().canManage(story, player)) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }
        GuiHolder holder = new GuiHolder(GuiHolder.Type.AUTHORS, story);
        Inventory inv = Bukkit.createInventory(holder, SIZE_AUTHORS,
                plugin.messages().format("gui-title-authors", "{title}", story.getTitle()));
        holder.setInventory(inv);

        fillPanes(inv);

        inv.setItem(0, item(Material.ARROW, plugin.messages().get("gui-back"), (List<String>) null));
        inv.setItem(4, item(Material.PLAYER_HEAD, plugin.messages().get("gui-current-authors"), (List<String>) null));
        inv.setItem(22, item(Material.EMERALD, plugin.messages().get("gui-add-hint"), (List<String>) null));

        int slot = 9;
        for (Map.Entry<UUID, String> entry : story.getAuthors().entrySet()) {
            if (slot > 17) {
                break;
            }
            inv.setItem(slot, head(entry.getKey(), entry.getValue(),
                    List.of(plugin.messages().get("gui-click-remove"))));
            holder.authorSlots.put(slot, entry.getKey());
            slot++;
        }

        int available = 27;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (available > 53) {
                break;
            }
            UUID uuid = online.getUniqueId();
            if (uuid.equals(story.getOwner()) || story.isAuthor(uuid)) {
                continue;
            }
            inv.setItem(available, head(uuid, online.getName(),
                    List.of(plugin.messages().get("gui-click-add"))));
            holder.availableSlots.put(available, uuid);
            available++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof GuiHolder gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Story story = gui.story();
        if (story == null || !plugin.storyManager().canManage(story, player)) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (gui.type() == GuiHolder.Type.MANAGE) {
            handleManageClick(player, story, slot);
        } else {
            handleAuthorsClick(player, story, gui, slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }

    private void handleManageClick(Player player, Story story, int slot) {
        switch (slot) {
            case 4 -> {
                player.closeInventory();
                plugin.bookService().openEditor(player, story);
                plugin.messages().sendPrefixed(player, "editor-opened");
            }
            case 10 -> promptTitle(player, story);
            case 14 -> togglePublic(player, story);
            case 16 -> openAuthors(player, story);
            case 22 -> {
                plugin.storyManager().delete(story);
                player.closeInventory();
                plugin.messages().sendPrefixed(player, "story-deleted");
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.9f, 0.8f);
            }
            default -> {
                // decoration
            }
        }
    }

    private void handleAuthorsClick(Player player, Story story, GuiHolder gui, int slot) {
        if (slot == 0) {
            openManage(player, story);
            return;
        }
        UUID toRemove = gui.authorSlots.get(slot);
        if (toRemove != null) {
            String name = story.getAuthors().getOrDefault(toRemove, "unknown");
            story.removeAuthor(toRemove);
            plugin.storyManager().save(story);
            plugin.messages().sendPrefixed(player, "author-removed", "{player}", name);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.6f);
            openAuthors(player, story);
            return;
        }
        UUID toAdd = gui.availableSlots.get(slot);
        if (toAdd != null) {
            addAuthor(player, story, toAdd);
        }
    }

    private void addAuthor(Player player, Story story, UUID uuid) {
        if (uuid.equals(story.getOwner())) {
            plugin.messages().sendPrefixed(player, "author-owner");
            return;
        }
        if (uuid.equals(player.getUniqueId())) {
            plugin.messages().sendPrefixed(player, "self-target");
            return;
        }
        if (story.isAuthor(uuid)) {
            plugin.messages().sendPrefixed(player, "author-already",
                    "{player}", nameOf(uuid));
            return;
        }
        if (story.getAuthors().size() >= plugin.storyConfig().maxAuthors()) {
            plugin.messages().sendPrefixed(player, "author-limit",
                    "{max}", String.valueOf(plugin.storyConfig().maxAuthors()));
            return;
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online == null) {
            plugin.messages().sendPrefixed(player, "author-not-online", "{player}", nameOf(uuid));
            return;
        }
        story.addAuthor(uuid, online.getName());
        plugin.storyManager().save(story);
        plugin.messages().sendPrefixed(player, "author-added", "{player}", online.getName());
        plugin.messages().sendPrefixed(online, "author-added-notify", "{owner}", player.getName());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);
        openAuthors(player, story);
    }

    private void togglePublic(Player player, Story story) {
        if (!plugin.storyConfig().allowPublicStories() && !story.isPublic()) {
            plugin.messages().sendPrefixed(player, "no-permission");
            return;
        }
        story.setPublic(!story.isPublic());
        plugin.storyManager().save(story);
        plugin.messages().sendPrefixed(player, story.isPublic() ? "public-on" : "public-off");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        openManage(player, story);
    }

    private void promptTitle(Player player, Story story) {
        plugin.chatInputManager().prompt(player, plugin.messages().prefixedFormat("title-prompt"), input -> {
            if (ChatInputManager.isCancel(input)) {
                plugin.messages().sendPrefixed(player, "title-cancelled");
                openManage(player, story);
                return;
            }
            int max = plugin.storyConfig().maxTitleLength();
            if (input.length() > max) {
                plugin.messages().sendPrefixed(player, "title-too-long", "{max}", String.valueOf(max));
                openManage(player, story);
                return;
            }
            story.setTitle(input);
            plugin.storyManager().save(story);
            plugin.messages().sendPrefixed(player, "title-set", "{title}", input);
            openManage(player, story);
        });
    }

    private ItemStack publicItem(Story story) {
        if (story.isPublic()) {
            return item(Material.ENDER_EYE, plugin.messages().get("gui-public-on"),
                    plugin.messages().getList("gui-public-lore"));
        }
        return item(Material.ENDER_PEARL, plugin.messages().get("gui-public-off"),
                plugin.messages().getList("gui-public-lore"));
    }

    private void fillPanes(Inventory inv) {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ", (List<String>) null);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack head(UUID uuid, String name, List<String> lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        if (meta != null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            meta.setOwningPlayer(offline);
            meta.setDisplayName(org.bukkit.ChatColor.AQUA + name);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() == null ? uuid.toString() : offline.getName();
    }

    /**
     * Держатель инвентаря, позволяющий отличать наши GUI.
     */
    public static final class GuiHolder implements InventoryHolder {

        public enum Type {
            MANAGE,
            AUTHORS
        }

        private final Type type;
        private final Story story;
        private Inventory inventory;
        private final Map<Integer, UUID> authorSlots = new java.util.HashMap<>();
        private final Map<Integer, UUID> availableSlots = new java.util.HashMap<>();

        public GuiHolder(Type type, Story story) {
            this.type = type;
            this.story = story;
        }

        public Type type() {
            return type;
        }

        public Story story() {
            return story;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
