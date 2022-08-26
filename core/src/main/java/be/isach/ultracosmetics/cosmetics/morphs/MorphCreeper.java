package be.isach.ultracosmetics.cosmetics.morphs;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.cosmetics.PlayerAffectingCosmetic;
import be.isach.ultracosmetics.cosmetics.Updatable;
import be.isach.ultracosmetics.cosmetics.type.MorphType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.MathUtils;
import be.isach.ultracosmetics.util.Particles;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.ActionBar;

/**
 * @author iSach
 * @since 08-26-2015
 */
public class MorphCreeper extends Morph implements PlayerAffectingCosmetic, Updatable {
    private int charge = 0;

    public MorphCreeper(UltraPlayer owner, MorphType type, UltraCosmetics ultraCosmetics) {
        super(owner, type, ultraCosmetics);
    }

    @Override
    public void onUpdate() {
        CreeperWatcher creeperWatcher = (CreeperWatcher) disguise.getWatcher();
        if (getPlayer().isSneaking()) {
            creeperWatcher.setIgnited(true);
            if (charge + 4 <= 100)
                charge += 4;
            XSound.ENTITY_CREEPER_PRIMED.play(getPlayer(), 1.4f, 1.5f);
        } else {
            if (creeperWatcher.isIgnited()) {
                disguise = new MobDisguise(getType().getDisguiseType());

                if (!getOwner().canSeeSelfMorph())
                    disguise.setViewSelfDisguise(false);

                DisguiseAPI.disguiseToAll(getPlayer(), disguise);
                //  disguise.setShowName(true);
            }
            if (charge == 100) {
                Particles.EXPLOSION_HUGE.display(getPlayer().getLocation());
                XSound.ENTITY_GENERIC_EXPLODE.play(getPlayer(), 1.4f, 1.5f);

                for (Entity ent : getPlayer().getNearbyEntities(3, 3, 3)) {
                    if (canAffect(ent)) {
                        double dX = getPlayer().getLocation().getX() - ent.getLocation().getX();
                        double dY = getPlayer().getLocation().getY() - ent.getLocation().getY();
                        double dZ = getPlayer().getLocation().getZ() - ent.getLocation().getZ();
                        double yaw = Math.atan2(dZ, dX);
                        double pitch = Math.atan2(Math.sqrt(dZ * dZ + dX * dX), dY) + Math.PI;
                        double X = Math.sin(pitch) * Math.cos(yaw);
                        double Y = Math.sin(pitch) * Math.sin(yaw);
                        double Z = Math.cos(pitch);

                        Vector vector = new Vector(X, Z, Y);
                        MathUtils.applyVelocity(ent, vector.multiply(1.3D).add(new Vector(0, 1.4D, 0)));
                    }
                }
                ActionBar.clearActionBar(getPlayer());
                charge = 0;
                return;
            }
            if (charge > 0)
                charge -= 4;
        }
        if (charge > 0 && charge < 100) {
            if (charge < 5) {
                ActionBar.clearActionBar(getPlayer());
            } else {
                ActionBar.sendActionBar(getPlayer(), MessageManager.getMessage("Morphs.Creeper.charging").replace("%chargelevel%", charge + ""));
            }
        } else if (charge == 100) {
            ActionBar.sendActionBar(getPlayer(), MessageManager.getMessage("Morphs.Creeper.release-to-explode"));
        }
    }
}
