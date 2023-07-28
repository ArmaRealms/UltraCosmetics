package be.isach.ultracosmetics.cosmetics.gadgets;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.cosmetics.PlayerAffectingCosmetic;
import be.isach.ultracosmetics.cosmetics.Updatable;
import be.isach.ultracosmetics.cosmetics.type.GadgetType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.Area;
import be.isach.ultracosmetics.util.BlockUtils;
import be.isach.ultracosmetics.util.ItemFactory;
import be.isach.ultracosmetics.util.MathUtils;
import be.isach.ultracosmetics.util.Particles;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents an instance of a discoball gadget summoned by a player.
 *
 * @author iSach
 * @since 08-03-2015
 */
public class GadgetDiscoBall extends Gadget implements PlayerAffectingCosmetic, Updatable {

    private static final Set<GadgetDiscoBall> DISCO_BALLS = new HashSet<>();

    private int i = 0;
    private double i2 = 0;
    private ArmorStand armorStand;
    private boolean running = false;

    public GadgetDiscoBall(UltraPlayer owner, GadgetType type, UltraCosmetics ultraCosmetics) {
        super(owner, type, ultraCosmetics);
    }

    @Override
    protected void onRightClick() {
        armorStand = (ArmorStand) getPlayer().getWorld().spawnEntity(getPlayer().getLocation().add(0, 3, 0), EntityType.ARMOR_STAND);
        armorStand.setMetadata("NO_INTER", new FixedMetadataValue(getUltraCosmetics(), ""));
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setSmall(false);
        setHelmet(armorStand, ItemFactory.rename(XMaterial.LIGHT_BLUE_STAINED_GLASS.parseItem(), " "));
        running = true;
        DISCO_BALLS.add(this);
        Bukkit.getScheduler().runTaskLater(getUltraCosmetics(), this::clean, 400);
    }

    @Override
    protected boolean checkRequirements(PlayerInteractEvent event) {
        if (GadgetDiscoBall.DISCO_BALLS.size() > 0) {
            MessageManager.send(getPlayer(), "Gadgets.DiscoBall.Already-Active");
            return false;
        }
        Area area = new Area(getPlayer().getLocation(), 0, 4);
        if (!area.isEmpty()) {
            MessageManager.send(getPlayer(), "Gadgets.DiscoBall.Not-Space-Above");
            return false;
        }
        return true;
    }

    @Override
    public void onUpdate() {
        if (armorStand == null) {
            return;
        }
        if (!armorStand.isValid() || !running) {
            i = 0;
            i2 = 0;
            clean();
            return;
        }
        armorStand.setHeadPose(armorStand.getHeadPose().add(0, 0.2, 0));

        setHelmet(armorStand, ItemFactory.getRandomStainedGlass());

        Particles.SPELL.display(armorStand.getEyeLocation(), 1, 1f);
        Particles.SPELL_INSTANT.display(armorStand.getEyeLocation(), 1, 1f);
        Location loc = armorStand.getEyeLocation().add(MathUtils.randomDouble(-4, 4), MathUtils.randomDouble(-3, 3), MathUtils.randomDouble(-4, 4));
        Particles.NOTE.display(new Particles.NoteColor(RANDOM.nextInt(25)), loc, 128);
        double angle, angle2, x, x2, z, z2;
        angle = 2 * Math.PI * i / 100;
        x = Math.cos(angle) * 4;
        z = Math.sin(angle) * 4;

        drawParticleLine(armorStand.getEyeLocation().add(-.5d, -.5d, -.5d).clone().add(0.5, 0.5, 0.5).clone().add(x, 0, z), armorStand.getEyeLocation().add(-.5d, -.5d, -.5d).clone().add(0.5, 0.5, 0.5), false, 20);

        i += 6;
        angle2 = 2 * Math.PI * i2 / 100;
        x2 = Math.cos(angle2) * 4;
        z2 = Math.sin(angle2) * 4;
        drawParticleLine(armorStand.getEyeLocation().add(-.5d, -.5d, -.5d).clone().add(0.5, 0.5, 0.5), armorStand.getEyeLocation().add(-.5d, -.5d, -.5d).clone().add(0.5, 0.5, 0.5).add(x2, 0, z2), true, 50);
        i2 += 0.4;

        XTag<XMaterial> tag = null;
        Map<Block, XMaterial> updates = new HashMap<>();
        for (Block b : BlockUtils.getBlocksInRadius(armorStand.getEyeLocation().add(-.5d, -.5d, -.5d), 10, false)) {
            XMaterial mat = XMaterial.matchXMaterial(b.getType());
            if (XTag.WOOL.isTagged(mat)) {
                tag = XTag.WOOL;
            } else if (XTag.CARPETS.isTagged(mat)) {
                tag = XTag.CARPETS;
            }

            if (tag != null) {
                updates.put(b, ItemFactory.randomFromTag(tag));
                tag = null;
            }
        }

        BlockUtils.setToRestore(updates, 4);

        Player player = getPlayer();
        for (Entity ent : loc.getWorld().getNearbyEntities(armorStand.getEyeLocation().add(-.5d, -.5d, -.5d), 7.5, 7.5, 7.5)) {
            if (ent.isOnGround() && canAffect(ent, player)) {
                MathUtils.applyVelocity(ent, new Vector(0, 0.3, 0));
            }
        }
    }

    private void clean() {
        running = false;
        if (armorStand != null) {
            armorStand.remove();
            armorStand = null;
        }
        DISCO_BALLS.remove(this);
    }

    @Override
    public void onClear() {
        clean();
    }

    public void drawParticleLine(Location a, Location b, boolean dust, int particles) {
        Location location = a.clone();
        Location target = b.clone();
        Vector link = target.toVector().subtract(location.toVector());
        float length = (float) link.length();
        link.normalize();

        float ratio = length / particles;
        Vector v = link.multiply(ratio);
        if (!dust) {
            MathUtils.rotateAroundAxisX(v, i);
        } else {
            MathUtils.rotateAroundAxisZ(v, i2 / 5);
            MathUtils.rotateAroundAxisX(v, i2 / 5);
        }
        Location loc = location.clone().subtract(v);
        int step = 0;
        for (int i = 0; i < particles; i++) {
            if (step >= (double) particles) step = 0;
            step++;
            loc.add(v);
            if (dust) {
                Particles.REDSTONE.display(MathUtils.random(255), MathUtils.random(255), MathUtils.random(255), loc);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void setHelmet(ArmorStand stand, ItemStack itemStack) {
        stand.setHelmet(itemStack);
    }
}
