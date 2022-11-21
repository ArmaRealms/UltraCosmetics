package be.isach.ultracosmetics.menu.menus;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.type.MountType;
import be.isach.ultracosmetics.menu.CosmeticMenu;
import be.isach.ultracosmetics.player.UltraPlayer;

/**
 * Mount {@link be.isach.ultracosmetics.menu.Menu Menu}.
 *
 * @author iSach
 * @since 08-23-2016
 */
public class MenuMounts extends CosmeticMenu<MountType> {

    public MenuMounts(UltraCosmetics ultraCosmetics) {
        super(ultraCosmetics, Category.MOUNTS);
    }

    @Override
    protected void toggleOff(UltraPlayer ultraPlayer, MountType type) {
        if (ultraPlayer.getCurrentMount() == null) {
            return;
        }
        ultraPlayer.getCurrentMount().setBeingRemoved(true);
        ultraPlayer.removeCosmetic(Category.MOUNTS);
    }

    @Override
    protected String getTypeName(MountType cosmeticType, UltraPlayer ultraPlayer) {
        return cosmeticType.getMenuName();
    }
}
