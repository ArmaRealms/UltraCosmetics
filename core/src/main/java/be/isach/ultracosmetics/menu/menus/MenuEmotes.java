package be.isach.ultracosmetics.menu.menus;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.type.EmoteType;
import be.isach.ultracosmetics.menu.CosmeticMenu;
import be.isach.ultracosmetics.player.UltraPlayer;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Emote {@link be.isach.ultracosmetics.menu.Menu Menu}.
 *
 * @author iSach
 * @since 08-23-2016
 */
public class MenuEmotes extends CosmeticMenu<EmoteType> {

    public MenuEmotes(UltraCosmetics ultraCosmetics) {
        super(ultraCosmetics, Category.EMOTES);
    }

    @Override
    protected void filterItem(ItemStack itemStack, EmoteType cosmeticType, UltraPlayer player) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        ItemMeta emoteMeta = cosmeticType.getFrames().get(cosmeticType.getMaxFrames() - 1).getItemMeta();
        emoteMeta.setDisplayName(itemMeta.getDisplayName());
        emoteMeta.setLore(itemMeta.getLore());
        itemStack.setItemMeta(emoteMeta);
    }

    @Override
    protected void toggleOn(UltraPlayer ultraPlayer, EmoteType emoteType, UltraCosmetics ultraCosmetics) {
        emoteType.equip(ultraPlayer, ultraCosmetics);
    }
}
