package ru.cupcqkeee.campfirestories.util;

import org.bukkit.block.Block;


public final class Loc {

    private Loc() {
    }

    public static String key(Block block) {
        return key(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static String key(String world, int x, int y, int z) {
        return world + ";" + x + ";" + y + ";" + z;
    }

    public static String worldOf(String key) {
        int i = key.indexOf(';');
        return i < 0 ? key : key.substring(0, i);
    }
}
