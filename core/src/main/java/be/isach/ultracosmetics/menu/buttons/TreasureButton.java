package be.isach.ultracosmetics.menu.buttons;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.menu.Button;

public abstract class TreasureButton implements Button {
    protected final boolean canBuyKeys;
    protected final String buyKeyMessage;

    public TreasureButton(UltraCosmetics ultraCosmetics) {
        canBuyKeys = ultraCosmetics.getEconomyHandler().isUsingEconomy() && SettingsManager.getConfig().getInt("TreasureChests.Key-Price") > 0;
        if (canBuyKeys) {
            buyKeyMessage = "\n" + MessageManager.getMessage("Click-Buy-Key") + "\n";
        } else {
            buyKeyMessage = "";
        }
    }
}
