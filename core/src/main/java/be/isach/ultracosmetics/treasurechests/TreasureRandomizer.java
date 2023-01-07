package be.isach.ultracosmetics.treasurechests;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.CustomConfiguration;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.type.CosmeticType;
import be.isach.ultracosmetics.cosmetics.type.GadgetType;
import be.isach.ultracosmetics.permissions.PermissionManager;
import be.isach.ultracosmetics.util.EntitySpawner;
import be.isach.ultracosmetics.util.WeightedSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by sacha on 19/08/15.
 */
public class TreasureRandomizer {
    private static final int MONEY_CHANCE = SettingsManager.getConfig().getInt("TreasureChests.Loots.Money.Chance");
    private static final int AMMO_CHANCE = SettingsManager.getConfig().getInt("TreasureChests.Loots.Gadgets-Ammo.Chance");
    private static final Random random = new Random();
    private final WeightedSet<ResultType> basicResultTypes = new WeightedSet<>();
    private final List<CommandReward> commandRewardList = new ArrayList<>();
    private final Map<ResultType,WeightedSet<CosmeticType<?>>> cosmetics = new HashMap<>();
    private final PermissionManager permissionManager = UltraCosmeticsData.get().getPlugin().getPermissionManager();
    private Location loc;
    private final Player player;
    private final boolean forceMessageToOwner;
    private final PermissionManager pm = UltraCosmeticsData.get().getPlugin().getPermissionManager();
    private ItemStack itemStack;
    private String[] name;

    public TreasureRandomizer(final Player player, Location location, boolean forceMessageToOwner) {
        this.loc = location.add(0.5, 0, 0.5);
        this.player = player;
        this.forceMessageToOwner = forceMessageToOwner;
        // add ammo.
        if (Category.GADGETS.isEnabled() && UltraCosmeticsData.get().isAmmoEnabled()
                && SettingsManager.getConfig().getBoolean("TreasureChests.Loots.Gadgets-Ammo.Enabled")) {
            for (CosmeticType<?> type : CosmeticType.enabledOf(Category.GADGETS)) {
                if (type.isEnabled() && ((GadgetType) type).requiresAmmo() && type.canBeFound() && pm.hasPermission(player, type)) {
                    cosmetics.computeIfAbsent(ResultType.AMMO, k -> new WeightedSet<>()).add(type, type.getChestWeight());
                }
            }
        }
        CustomConfiguration config = UltraCosmeticsData.get().getPlugin().getConfig();
        for (String key : config.getConfigurationSection("TreasureChests.Loots.Commands").getKeys(false)) {
            String path = "TreasureChests.Loots.Commands." + key;
            if (config.getBoolean(path + ".Enabled")) {
                String cancelPermission = config.getString(path + ".Cancel-If-Permission");
                if (cancelPermission.equals("no") || !player.hasPermission(cancelPermission)) {
                    commandRewardList.add(new CommandReward(path));
                }
            }
        }

        for (Category cat : Category.values()) {
            setupChance(cat);
        }

        if (UltraCosmeticsData.get().useMoneyTreasureLoot()) {
            addWeightedResult(MONEY_CHANCE, ResultType.MONEY);
        }

        for (CommandReward commandReward : commandRewardList) {
            addWeightedResult(commandReward.getChance(), ResultType.COMMAND);
        }
    }

    public TreasureRandomizer(final Player player, Location location) {
        this(player, location, false);
    }

    private void setupChance(Category category) {
        // word case, like "Pets" rather than "PETS"
        String configPath = "TreasureChests.Loots." + category.getMessagesName();
        if (!SettingsManager.getConfig().getBoolean(configPath + ".Enabled")) return;
        if (!category.isEnabled()) return;
        ResultType result = ResultType.fromCategory(category);
        for (CosmeticType<?> type : category.getEnabled()) {
            if (!type.isEnabled() || type.getChestWeight() < 1 || pm.hasPermission(player, type)) continue;
            cosmetics.computeIfAbsent(result, k -> new WeightedSet<>()).add(type, type.getChestWeight());
        }
    }

    private void addWeightedResult(int weight, ResultType type) {
        basicResultTypes.add(type, weight);
    }

    private static Color randomColor() {
        Random r = ThreadLocalRandom.current();
        return Color.fromRGB(r.nextInt(256), r.nextInt(256), r.nextInt(256));
    }

    private String getConfigMessage(String s) {
        String message = SettingsManager.getConfig().getString(s);
        if (message == null) {
            return ChatColor.RED.toString() + ChatColor.BOLD.toString() + "Error";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void giveRandomThing() {
        XSound.BLOCK_CHEST_OPEN.play(loc, 1.4f, 1.5f);
        WeightedSet<ResultType> choices = new WeightedSet<>(basicResultTypes);
        for (ResultType result : cosmetics.keySet()) {
            // categories with no unlockables will not appear as keys at all
            int weight;
            if (result.category == null) {
                // ammo
                weight = AMMO_CHANCE;
            } else {
                weight = SettingsManager.getConfig().getInt("TreasureChests.Loots." + result.category.getMessagesName() + ".Chance");
            }
            choices.add(result, weight);
        }
        if (choices.size() == 0) {
            giveFallback();
            return;
        }
        ResultType type = choices.getRandom();

        switch (type) {
        case MONEY:
            giveMoney();
            break;
        case AMMO:
            giveAmmo();
            break;
        case MOUNT:
            giveRandomCosmetic(type, "Mount", "Mounts");
            break;
        case MORPH:
            giveRandomCosmetic(type, "Morph", "Morphs");
            break;
        case PET:
            giveRandomCosmetic(type, "Pet", "Pets");
            break;
        case EFFECT:
            giveRandomCosmetic(type, "Effect", "Effects");
            break;
        case HAT:
            giveRandomCosmetic(type, "Hat", "Hats");
            break;
        case GADGET:
            giveRandomCosmetic(type, "Gadget", "Gadgets");
            break;
        case SUIT_HELMET:
        case SUIT_CHESTPLATE:
        case SUIT_LEGGINGS:
        case SUIT_BOOTS:
            giveRandomCosmetic(type, "Suit", "Suits");
            break;
        case EMOTE:
            giveRandomCosmetic(type, "Emote", "Emotes");
            break;
        case COMMAND:
            giveRandomCommandReward();
            break;
        }
    }

    public String[] getName() {
        return name;
    }

    public void giveFallback() {
        if (UltraCosmeticsData.get().getPlugin().getEconomyHandler().isUsingEconomy()) {
            giveMoney();
        } else {
            giveNothing();
        }
    }

    public void giveNothing() {
        name = MessageManager.getMessage("Treasure-Chests-Loot.Nothing").split("\n");
        itemStack = new ItemStack(Material.BARRIER);
    }

    private int randomInRange(int min, int max) {
        if (min < max) {
            return random.nextInt(max - min) + min;
        }
        return min;
    }

    public void giveMoney() {
        int min = SettingsManager.getConfig().getInt("TreasureChests.Loots.Money.Min");
        int max = SettingsManager.getConfig().getInt("TreasureChests.Loots.Money.Max");
        int money = randomInRange(min, max);
        name = MessageManager.getMessage("Treasure-Chests-Loot.Money").replace("%money%", money + "").split("\n");
        UltraCosmeticsData.get().getPlugin().getEconomyHandler().getHook().deposit(player, money);
        itemStack = XMaterial.SUNFLOWER.parseItem();
        if (money > 3 * SettingsManager.getConfig().getInt("TreasureChests.Loots.Money.Max") / 4) {
            spawnRandomFirework(loc);
        }
        boolean toOthers = SettingsManager.getConfig().getBoolean("TreasureChests.Loots.Money.Message.enabled");
        broadcast(getConfigMessage("TreasureChests.Loots.Money.Message.message").replace("%money%", String.valueOf(money)), toOthers);

    }

    public void giveAmmo() {
        GadgetType g = (GadgetType) cosmetics.get(ResultType.AMMO).getRandom();
        int ammoMin = SettingsManager.getConfig().getInt("TreasureChests.Loots.Gadgets-Ammo.Min");
        int ammoMax = SettingsManager.getConfig().getInt("TreasureChests.Loots.Gadgets-Ammo.Max");
        int ammo = randomInRange(ammoMin, ammoMax);
        name = MessageManager.getMessage("Treasure-Chests-Loot.Ammo").replace("%name%", g.getName()).replace("%ammo%", String.valueOf(ammo)).split("\n");

        UltraCosmeticsData.get().getPlugin().getPlayerManager().getUltraPlayer(player).addAmmo(g, ammo);
        itemStack = g.getMaterial().parseItem();
        // if the player received more than half of what they could have, send a firework
        if (ammo > (ammoMax - ammoMin) / 2 + ammoMin) {
            spawnRandomFirework(loc);
        }
        boolean toOthers = SettingsManager.getConfig().getBoolean("TreasureChests.Loots.Gadgets-Ammo.Message.enabled");
        broadcast(getConfigMessage("TreasureChests.Loots.Gadgets-Ammo.Message.message").replace("%ammo%", String.valueOf(ammo)).replace("%gadget%", (UltraCosmeticsData.get().arePlaceholdersColored()) ? g.getName() : ChatColor.stripColor(g.getName())), toOthers);
    }

    private CosmeticType<?> getRandomCosmetic(ResultType result) {
        CosmeticType<?> cosmetic = cosmetics.get(result).removeRandom();
        if (cosmetics.get(result).size() == 0) {
            cosmetics.remove(result);
        }
        return cosmetic;
    }

    public void giveRandomCosmetic(ResultType result, String lang, String configName) {
        CosmeticType<?> cosmetic = getRandomCosmetic(result);
        name = MessageManager.getMessage("Treasure-Chests-Loot." + lang).replace("%" + lang.toLowerCase() + "%", cosmetic.getName()).split("\n");
        permissionManager.setPermission(player, cosmetic);
        spawnRandomFirework(loc);
        itemStack = cosmetic.getItemStack();
        boolean toOthers = SettingsManager.getConfig().getBoolean("TreasureChests.Loots." + configName + ".Message.enabled");
        String message = getConfigMessage("TreasureChests.Loots." + configName + ".Message.message")
                .replace("%" + lang.toLowerCase() + "%", UltraCosmeticsData.get().arePlaceholdersColored() ? cosmetic.getName() : ChatColor.stripColor(cosmetic.getName()));
        broadcast(message, toOthers);
    }

    public void giveRandomCommandReward() {
        WeightedSet<CommandReward> rewards = new WeightedSet<>();
        for (CommandReward commandReward : commandRewardList) {
            rewards.add(commandReward, commandReward.getChance());
        }
        CommandReward reward = rewards.getRandom();
        rewards.clear();
        for (String command : reward.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%name%", player.getName()));
        }
        name = new String[] { reward.getName().replace("%name%", player.getName()) };
        itemStack = reward.getItemStack();
        spawnRandomFirework(loc);

        broadcast(ChatColor.translateAlternateColorCodes('&', reward.getMessage().replace("%prefix%", MessageManager.getMessage("Prefix"))), reward.getMessageEnabled());
    }

    public void spawnRandomFirework(Location location) {
        EntitySpawner.spawnFireworks(location.clone().add(0.5, 0, 0.5), randomColor(), randomColor(), FireworkEffect.Type.BALL);
    }

    private void broadcast(String message, boolean toOthers) {
        UltraCosmetics ultraCosmetics = UltraCosmeticsData.get().getPlugin();
        message = message.replace("%name%", player.getName());
        if (ultraCosmetics.getDiscordHook() != null) {
            ultraCosmetics.getDiscordHook().sendLootMessage(player, message.replace("%prefix%", ""));
        }
        message = message.replace("%prefix%", MessageManager.getMessage("Prefix"));
        if (!toOthers) {
            if (forceMessageToOwner) {
                player.sendMessage(message);
            }
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == this.player || (SettingsManager.isAllowedWorld(player.getWorld())
                    && ultraCosmetics.getPlayerManager().getUltraPlayer(player).isTreasureNotifying())) {
                player.sendMessage(message);
            }
        }
    }

    public void setLocation(Location newLoc) {
        loc = newLoc;
    }

    public enum ResultType {
        AMMO,
        GADGET(Category.GADGETS),
        MONEY,
        MORPH(Category.MORPHS),
        MOUNT(Category.MOUNTS),
        EFFECT(Category.EFFECTS),
        PET(Category.PETS),
        HAT(Category.HATS),
        SUIT_HELMET(Category.SUITS_HELMET),
        SUIT_CHESTPLATE(Category.SUITS_CHESTPLATE),
        SUIT_LEGGINGS(Category.SUITS_LEGGINGS),
        SUIT_BOOTS(Category.SUITS_BOOTS),
        EMOTE(Category.EMOTES),
        COMMAND,
        ;

        private final Category category;

        private ResultType() {
            this(null);
        }

        private ResultType(Category category) {
            this.category = category;
        }

        private static ResultType fromCategory(Category cat) {
            for (ResultType type : values()) {
                if (type.category == cat) {
                    return type;
                }
            }
            return null;
        }
    }
}
