package be.isach.ultracosmetics.cosmetics.mounts.abstracthorse;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.cosmetics.type.MountType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.Particles;

/**
 * @author RadBuilder
 */
public class MountInfernalHorror extends MountAbstractHorse {

    public MountInfernalHorror(UltraPlayer owner, MountType type, UltraCosmetics ultraCosmetics) {
        super(owner, ultraCosmetics, type);
    }

    @Override
    public void onUpdate() {
        Particles.FLAME.display(0.4f, 0.2f, 0.4f, entity.getLocation().clone().add(0, 1, 0), 5);
    }
}
