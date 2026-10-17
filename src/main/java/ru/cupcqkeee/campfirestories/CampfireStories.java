package ru.cupcqkeee.campfirestories;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cupcqkeee.campfirestories.command.CampfireCommand;
import ru.cupcqkeee.campfirestories.gui.GuiManager;
import ru.cupcqkeee.campfirestories.listener.CampfireListener;
import ru.cupcqkeee.campfirestories.listener.InteractListener;
import ru.cupcqkeee.campfirestories.service.BookService;
import ru.cupcqkeee.campfirestories.service.ChatInputManager;
import ru.cupcqkeee.campfirestories.storage.StoryDatabase;
import ru.cupcqkeee.campfirestories.story.StoryManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public final class CampfireStories extends JavaPlugin {

    private StoryConfig storyConfig;
    private Messages messages;
    private StoryDatabase database;
    private StoryManager storyManager;
    private BookService bookService;
    private ChatInputManager chatInputManager;
    private GuiManager guiManager;

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.storyConfig = new StoryConfig(this);
        this.messages = new Messages(this);
        this.database = new StoryDatabase(this);

        try {
            database.init();
        } catch (Exception ex) {
            getLogger().severe("Не удалось инициализировать базу данных SQLite: " + ex.getMessage());
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.storyManager = new StoryManager(this, database);
        this.storyManager.loadAll();

        this.bookService = new BookService(this);
        this.chatInputManager = new ChatInputManager(this);
        this.guiManager = new GuiManager(this);

        getServer().getPluginManager().registerEvents(new CampfireListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(bookService, this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        CampfireCommand command = new CampfireCommand(this);
        if (getCommand("campfire") != null) {
            getCommand("campfire").setExecutor(command);
            getCommand("campfire").setTabCompleter(command);
        }

        getLogger().info("==============================================");
        getLogger().info("  CampfireStories v" + getDescription().getVersion());
        getLogger().info("  Автор: CUPcqkeee | https://t.me/CUPcqkeee");
        getLogger().info("  Загружено историй: " + storyManager.all().size());
        getLogger().info("==============================================");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }

    public void reloadAll() {
        reloadConfig();
        this.storyConfig = new StoryConfig(this);
        this.messages.reload();
    }

    public boolean onCooldown(Player player) {
        long now = System.currentTimeMillis();
        long until = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (until > now) {
            return true;
        }
        cooldowns.put(player.getUniqueId(), now + storyConfig.cooldownMillis());
        return false;
    }

    public StoryConfig storyConfig() {
        return storyConfig;
    }

    public Messages messages() {
        return messages;
    }

    public StoryManager storyManager() {
        return storyManager;
    }

    public BookService bookService() {
        return bookService;
    }

    public ChatInputManager chatInputManager() {
        return chatInputManager;
    }

    public GuiManager guiManager() {
        return guiManager;
    }
}
