package be.isach.ultracosmetics.v1_12_R1.mount;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.cosmetics.type.MountType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.v1_12_R1.customentities.CustomSlime;
import net.minecraft.server.v1_12_R1.EntityLiving;

import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;

/**
 * @author RadBuilder
 */
public class MountSlime extends MountCustomEntity {
    public MountSlime(UltraPlayer owner, MountType type, UltraCosmetics ultraCosmetics) {
        super(owner, type, ultraCosmetics);
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public EntityLiving getNewEntity() {
        return new CustomSlime(((CraftPlayer) getPlayer()).getHandle().getWorld());
    }
}
