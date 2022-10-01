package be.isach.ultracosmetics.cosmetics.pets;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.cosmetics.type.PetType;
import be.isach.ultracosmetics.player.UltraPlayer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;

/**
 * Represents an instance of a warden pet summoned by a player.
 *
 * @author Chris6ix
 * @since 08-06-2022
 */
public class PetWarden extends Pet {
    public PetWarden(UltraPlayer owner, PetType type, UltraCosmetics ultraCosmetics) {
        super(owner, type, ultraCosmetics);
    }

    @EventHandler
    public void onDarkness(EntityPotionEffectEvent event) {
        if (event.getEntity() == getPlayer() && event.getCause() == Cause.WARDEN
                && SettingsManager.getConfig().getBoolean(getOptionPath("Block-Effect"))) {
            event.setCancelled(true);
        }
    }
}
