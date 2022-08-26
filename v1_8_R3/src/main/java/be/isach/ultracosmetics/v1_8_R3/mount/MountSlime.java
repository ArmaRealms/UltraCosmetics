package be.isach.ultracosmetics.v1_8_R3.mount;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.cosmetics.type.MountType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.v1_8_R3.customentities.CustomSlime;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;

/**
 * Created by Sacha on 17/10/15.
 */
public class MountSlime extends MountCustomEntity {
    public MountSlime(UltraPlayer owner, MountType type, UltraCosmetics ultraCosmetics) {
        super(owner, type, ultraCosmetics);
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public EntityInsentient getNewEntity() {
        return new CustomSlime(((CraftPlayer) getPlayer()).getHandle().getWorld());
    }
}
