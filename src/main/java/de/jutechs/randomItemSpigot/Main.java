package de.jutechs.randomItemSpigot;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin {

    private static final int TICKS_PER_HOUR = 20 * 60 * 15; // 15 minutes for demo; adjust for 1 hour
    private static final Set<Material> BLACKLISTED_ITEMS = new HashSet<>();
    private final Random random = new Random();
    private PlayerDataManager dataManager;

    @Override
    public void onEnable() {
        dataManager = new PlayerDataManager();
        setupBlacklistedItems();

        // Run a task every tick to track player playtime
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(Main.this::trackPlaytime);
            }
        }.runTaskTimer(this, 0, 1); // Schedule every tick
    }

    @Override
    public void onDisable() {
        dataManager.saveData(); // Save all player data on plugin disable
    }

    private void setupBlacklistedItems() {
        BLACKLISTED_ITEMS.add(Material.BARRIER);
        BLACKLISTED_ITEMS.add(Material.BEDROCK);
        BLACKLISTED_ITEMS.add(Material.COMMAND_BLOCK);
        BLACKLISTED_ITEMS.add(Material.CHAIN_COMMAND_BLOCK);
        BLACKLISTED_ITEMS.add(Material.REPEATING_COMMAND_BLOCK);
        BLACKLISTED_ITEMS.add(Material.STRUCTURE_BLOCK);
        BLACKLISTED_ITEMS.add(Material.STRUCTURE_VOID);
        BLACKLISTED_ITEMS.add(Material.JIGSAW);
        BLACKLISTED_ITEMS.add(Material.DEBUG_STICK);
        BLACKLISTED_ITEMS.add(Material.COMMAND_BLOCK_MINECART);
        BLACKLISTED_ITEMS.add(Material.KNOWLEDGE_BOOK);
        BLACKLISTED_ITEMS.add(Material.WRITTEN_BOOK);
        BLACKLISTED_ITEMS.add(Material.END_PORTAL_FRAME);
        BLACKLISTED_ITEMS.add(Material.END_PORTAL);
        BLACKLISTED_ITEMS.add(Material.NETHER_PORTAL);
    }

    private void trackPlaytime(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerDataManager.PlayerData data = dataManager.getPlayerData(player);

        // Increment playtime ticks
        data.playtimeTicks++;

        // Award a voucher if playtime has passed the threshold
        if (data.playtimeTicks >= TICKS_PER_HOUR) {
            data.playtimeTicks = 0;
            data.vouchers++;
            player.sendMessage(ChatColor.GREEN + "You've received a voucher!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("redeem") && sender instanceof Player) {
            Player player = (Player) sender;
            redeemVoucher(player);
            return true;
        }
        return false;
    }

    private void redeemVoucher(Player player) {
        PlayerDataManager.PlayerData data = dataManager.getPlayerData(player);

        if (data.vouchers > 0) {
            int redeemedCount = 0;

            while (data.vouchers > 0) {
                data.vouchers--;
                redeemedCount++;

                // Select a random item
                ItemStack reward = getRandomItem();
                if (reward != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), reward);
                } else {
                    player.sendMessage(ChatColor.RED + "No valid item found. Please report this issue.");
                    return;
                }
            }

            player.sendMessage(ChatColor.GREEN + "You've redeemed " + redeemedCount + " voucher(s)!");
        } else {
            int remainingTicks = TICKS_PER_HOUR - data.playtimeTicks;
            int remainingMinutes = remainingTicks / (20 * 60);
            int remainingSeconds = (remainingTicks / 20) % 60;

            player.sendMessage(ChatColor.RED + "You don't have any vouchers to redeem!");
            player.sendMessage(ChatColor.YELLOW + "Next voucher in: " + remainingMinutes + "m " + remainingSeconds + "s.");
        }
    }

    private ItemStack getRandomItem() {
        Material[] materials = Material.values();
        for (int attempts = 0; attempts < 10; attempts++) {
            Material material = materials[random.nextInt(materials.length)];
            if (!BLACKLISTED_ITEMS.contains(material) && material.isItem()) {
                return new ItemStack(material);
            }
        }
        return null; // No valid item found after attempts
    }
}

