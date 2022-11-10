package be.isach.ultracosmetics.menu.menus;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.economy.EconomyHandler;
import be.isach.ultracosmetics.menu.ClickRunnable;
import be.isach.ultracosmetics.menu.Menu;
import be.isach.ultracosmetics.menu.PurchaseData;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.ItemFactory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

/**
 * Created by sacha on 04/04/2017.
 */
public class MenuPurchase extends Menu {

    private String name;
    private PurchaseData purchaseData;

    public MenuPurchase(UltraCosmetics ultraCosmetics, String name, PurchaseData purchaseData) {
        super(ultraCosmetics);
        this.name = name;
        this.purchaseData = purchaseData;
    }

    @Override
    protected void putItems(Inventory inventory, UltraPlayer player) {
        // Showcase Item
        putItem(inventory, 13, purchaseData.getShowcaseItem());

        // Purchase Item
        ItemStack purchaseItem = ItemFactory.create(XMaterial.EMERALD_BLOCK, MessageManager.getMessage("Purchase"));
        ClickRunnable purchaseClickRunnable = data -> {
            player.getBukkitPlayer().closeInventory();
            EconomyHandler eh = getUltraCosmetics().getEconomyHandler();
            eh.getHook().withdraw(player.getBukkitPlayer(), purchaseData.getPrice(), () -> {
                player.sendMessage(MessageManager.getMessage("Successful-Purchase"));
                purchaseData.getOnPurchase().run();
            }, () -> {
                player.sendMessage(MessageManager.getMessage("Not-Enough-Money"));
            });

        };
        for (int i = 27; i < 30; i++) {
            for (int j = i; j <= i + 18; j += 9) {
                putItem(inventory, j, purchaseItem, purchaseClickRunnable);
            }
        }

        // Cancel Item
        ItemStack cancelItem = ItemFactory.create(XMaterial.REDSTONE_BLOCK, MessageManager.getMessage("Cancel"));
        ClickRunnable cancelClickRunnable = data -> purchaseData.getOnCancel().run();
        for (int i = 33; i < 36; i++) {
            for (int j = i; j <= i + 18; j += 9) {
                putItem(inventory, j, cancelItem, cancelClickRunnable);
            }
        }
    }

    @Override
    protected int getSize() {
        return 54;
    }

    @Override
    protected String getName() {
        return name == null ? "Purchase" : name;
    }
}
