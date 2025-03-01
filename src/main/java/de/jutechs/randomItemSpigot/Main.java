package de.jutechs.randomItemSpigot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.EventHandler;

import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    private int TICKS_PER_INTERVAL; // loaded from config (minutes -> ticks)
    private static final String BYPASS_PERMISSION = "cantplaceblacklisted.bypass";
    private final Random random = new Random();

    // Our blacklisted materials
    private final Set<Material> blacklistedItems = new HashSet<>();

    // Manager for storing all player data in memory and reading/writing JSON
    private PlayerDataManager dataManager;

    @Override
    public void onEnable() {
        // 1. Create default config (only once if it doesn't exist).
        saveDefaultConfig();

        // 2. Load config values
        loadConfigValues();

        // 3. Initialize data manager
        // Pass the plugin's data folder for storing the JSON file inside /plugins/<PluginName>
        dataManager = new PlayerDataManager(getDataFolder());

        // 4. Register event listeners
        getServer().getPluginManager().registerEvents(new EventListener(), this);

        // 5. Schedule a repeating task to track player playtime every tick
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(Main.this::trackPlaytime);
            }
        }.runTaskTimer(this, 0, 1); // every tick (20 ticks = 1 second)
    }

    @Override
    public void onDisable() {
        // Save all data to file on server/plugin shutdown
        dataManager.saveData();
    }

    /**
     * Load (or reload) config values.
     */
    private void loadConfigValues() {
        // 1. Time (in minutes) before awarding a voucher
        int intervalMinutes = getConfig().getInt("time-interval-minutes", 15);
        TICKS_PER_INTERVAL = intervalMinutes * 20 * 60;

        // 2. Blacklisted items
        blacklistedItems.clear();
        for (String itemName : getConfig().getStringList("blacklisted-items")) {
            Material mat = Material.matchMaterial(itemName);
            if (mat != null) {
                blacklistedItems.add(mat);
            } else {
                getLogger().warning("Invalid material in config: " + itemName);
            }
        }
    }

    /**
     * Increments a player's "playtimeTicks" and awards vouchers if threshold is met.
     */
    private void trackPlaytime(Player player) {
        PlayerDataManager.PlayerData data = dataManager.getPlayerData(player);

        // increment tick counter
        data.playtimeTicks++;

        // if threshold reached, award a voucher
        if (data.playtimeTicks >= TICKS_PER_INTERVAL) {
            data.playtimeTicks = 0;
            data.vouchers++;
            player.sendMessage(color(getConfig().getString(
                    "messages.voucher-received", "&aYou've received a voucher!"
            )));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /redeem command
        if (label.equalsIgnoreCase("redeem") && sender instanceof Player) {
            Player player = (Player) sender;
            redeemVouchers(player);
            return true;
        }
        // Optional: /randomitemsreload to reload config
        if (label.equalsIgnoreCase("randomitemsreload")) {
            if (sender.hasPermission("randomitems.reload")) {
                reloadConfig();
                loadConfigValues();
                sender.sendMessage(ChatColor.GREEN + "RandomItems config reloaded!");
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have permission to reload this plugin's config.");
            }
            return true;
        }
        return false;
    }

    /**
     * Redeems all vouchers for the player, giving random items for each voucher.
     */
    private void redeemVouchers(Player player) {
        PlayerDataManager.PlayerData data = dataManager.getPlayerData(player);
        if (data.vouchers > 0) {
            int redeemedCount = 0;
            while (data.vouchers > 0) {
                data.vouchers--;
                redeemedCount++;

                ItemStack reward = getRandomItem();
                if (reward != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), reward);
                } else {
                    player.sendMessage(ChatColor.RED + "No valid item found. Please report this issue.");
                    return;
                }
            }
            // message for redeemed vouchers
            String redeemedMsg = getConfig().getString("messages.redeemed", "&aYou've redeemed %count% voucher(s)!");
            redeemedMsg = redeemedMsg.replace("%count%", String.valueOf(redeemedCount));
            player.sendMessage(color(redeemedMsg));
        } else {
            // if no vouchers, show how long until the next voucher
            int remainingTicks = TICKS_PER_INTERVAL - data.playtimeTicks;
            int remainingMinutes = remainingTicks / (20 * 60);
            int remainingSeconds = (remainingTicks / 20) % 60;

            player.sendMessage(color(getConfig().getString("messages.no-vouchers", "&cYou don't have any vouchers to redeem!")));
            String nextVoucherMsg = getConfig().getString("messages.next-voucher", "&eNext voucher in: %minutes%m %seconds%s.");
            nextVoucherMsg = nextVoucherMsg
                    .replace("%minutes%", String.valueOf(remainingMinutes))
                    .replace("%seconds%", String.valueOf(remainingSeconds));
            player.sendMessage(color(nextVoucherMsg));
        }
    }

    /**
     * Attempts to return a random Material that's not blacklisted and is an actual placeable item.
     */
    private ItemStack getRandomItem() {
        Material[] materials = Material.values();
        for (int attempts = 0; attempts < 10; attempts++) {
            Material material = materials[random.nextInt(materials.length)];
            if (!blacklistedItems.contains(material) && material.isItem()) {
                return new ItemStack(material);
            }
        }
        return null; // fallback if none found in 10 tries
    }

    /**
     * Translates '&' color codes.
     */
    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /**
     * Inner Event Listener class for blocking blacklisted item placement.
     */
    public class EventListener implements Listener {
        @EventHandler
        public void onBlockPlace(BlockPlaceEvent event) {
            Player player = event.getPlayer();
            if (player.hasPermission(BYPASS_PERMISSION)) {
                return; // allow if they have a bypass permission
            }
            if (blacklistedItems.contains(event.getBlock().getType())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.DARK_RED + "You are not allowed to place "
                        + ChatColor.RED + event.getBlock().getType()
                        + ChatColor.DARK_RED + "!");
            }
        }
    }
}
