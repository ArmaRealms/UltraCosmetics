package be.isach.ultracosmetics.cosmetics.pets;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.cosmetics.type.PetType;
import be.isach.ultracosmetics.player.UltraPlayer;

/**
 * Represents an instance of a zombified piglin pet summoned by a player.
 *
 * @author Chris6ix
 * @since 15-09-2022
 */
public class PetZombifiedPiglin extends Pet {
    public PetZombifiedPiglin(UltraPlayer owner, PetType type, UltraCosmetics ultraCosmetics) {
        super(owner, type, ultraCosmetics);
    }
}
