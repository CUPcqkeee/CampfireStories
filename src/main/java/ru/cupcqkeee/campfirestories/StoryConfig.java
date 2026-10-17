package ru.cupcqkeee.campfirestories;

import org.bukkit.configuration.file.FileConfiguration;

public class StoryConfig {

    private final boolean allowSoulCampfire;
    private final boolean allowPublicStories;
    private final boolean deleteOnBreak;
    private final boolean protectCampfire;
    private final String defaultTitle;
    private final int maxTitleLength;
    private final int maxPages;
    private final int maxAuthors;
    private final int manageRadius;
    private final int notifyRadius;
    private final long cooldownMillis;
    private final String listVisibility;

    public StoryConfig(CampfireStories plugin) {
        FileConfiguration c = plugin.getConfig();
        this.allowSoulCampfire = c.getBoolean("settings.allow-soul-campfire", true);
        this.allowPublicStories = c.getBoolean("settings.allow-public-stories", true);
        this.deleteOnBreak = c.getBoolean("settings.delete-story-on-break", true);
        this.protectCampfire = c.getBoolean("settings.protect-campfire", false);
        this.defaultTitle = c.getString("settings.default-title", "Новая история");
        this.maxTitleLength = Math.max(1, c.getInt("settings.max-title-length", 40));
        this.maxPages = Math.min(50, Math.max(1, c.getInt("settings.max-pages", 50)));
        this.maxAuthors = Math.max(1, c.getInt("settings.max-authors", 8));
        this.manageRadius = Math.max(2, c.getInt("settings.manage-radius", 10));
        this.notifyRadius = Math.max(0, c.getInt("settings.read-notify-radius", 16));
        this.cooldownMillis = Math.max(0, c.getLong("settings.edit-cooldown-ms", 1500));
        this.listVisibility = c.getString("settings.list-visibility", "author");
    }

    public boolean allowSoulCampfire() {
        return allowSoulCampfire;
    }

    public boolean allowPublicStories() {
        return allowPublicStories;
    }

    public boolean deleteOnBreak() {
        return deleteOnBreak;
    }

    public boolean protectCampfire() {
        return protectCampfire;
    }

    public String defaultTitle() {
        return defaultTitle;
    }

    public int maxTitleLength() {
        return maxTitleLength;
    }

    public int maxPages() {
        return maxPages;
    }

    public int maxAuthors() {
        return maxAuthors;
    }

    public int manageRadius() {
        return manageRadius;
    }

    public int notifyRadius() {
        return notifyRadius;
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }

    public String listVisibility() {
        return listVisibility;
    }
}
