package be.isach.ultracosmetics.cosmetics.mounts.abstracthorse;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.cosmetics.type.MountType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.Particles;
import be.isach.ultracosmetics.util.PlayerUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

/**
 * @author RadBuilder
 */
public class MountRudolph extends MountAbstractHorse {

    private ArmorStand left, right;

    public MountRudolph(UltraPlayer owner, MountType type, UltraCosmetics ultraCosmetics) {
        super(owner, ultraCosmetics, type);
    }

    @Override
    public void setupEntity() {
        super.setupEntity();
        left = spawnArmorStand(false);
        right = spawnArmorStand(true);
        moveAntlers();
    }

    @SuppressWarnings("deprecation")
    private ArmorStand spawnArmorStand(boolean right) {
        ArmorStand armorStand = getEntity().getWorld().spawn(getEyeLocation(), ArmorStand.class);
        armorStand.setBasePlate(false);
        armorStand.setGravity(false);
        armorStand.setArms(true);
        armorStand.setVisible(false);
        if (!right) {
            armorStand.setRightArmPose(new EulerAngle(Math.PI, Math.PI / 4, -(Math.PI / 4)));
        } else {
            armorStand.setRightArmPose(new EulerAngle(Math.PI, Math.PI / 4 + -(Math.PI / 2), Math.PI / 4));
        }
        armorStand.setItemInHand(new ItemStack(Material.DEAD_BUSH));
        armorStand.setMetadata("C_AD_ArmorStand", new FixedMetadataValue(getUltraCosmetics(), getPlayer().getUniqueId().toString()));
        getUltraCosmetics().getArmorStandManager().makeUcStand(armorStand);
        return armorStand;
    }

    @Override
    public void onUpdate() {
        if (left != null && right != null)
            moveAntlers();
    }

    private void moveAntlers() {
        Location location = getEyeLocation();

        Vector vectorLeft = getLeftVector(location).multiply(0.5).multiply(1.6);
        Vector rightVector = getRightVector(location).multiply(0.5).multiply(0.4);

        Vector playerVector = PlayerUtils.getHorizontalDirection(getPlayer(), 0.75);

        location.add(vectorLeft).add(playerVector).add(0, -1.7, 0);
        left.teleport(location);

        location.subtract(vectorLeft).add(rightVector);

        right.teleport(location);
        location.subtract(rightVector).subtract(playerVector).subtract(0, -1.5, 0);

        double y = location.getY();
        location.add(location.getDirection().multiply(1.15));
        location.setY(y - 0.073);
        Particles.REDSTONE.display(255, 0, 0, location);
        new Thread(() -> {
            for (Player player : getPlayer().getWorld().getPlayers()) {
                UltraCosmeticsData.get().getVersionManager().getEntityUtil().sendTeleportPacket(player, right);
                UltraCosmeticsData.get().getVersionManager().getEntityUtil().sendTeleportPacket(player, left);
            }
        }).start();
    }

    @Override
    public void onClear() {
        super.onClear();

        if (left != null)
            left.remove();
        if (right != null)
            right.remove();
    }

    public static Vector getLeftVector(Location loc) {
        final float newX = (float) (loc.getX() + (1 * Math.cos(Math.toRadians(loc.getYaw() + 0))));
        final float newZ = (float) (loc.getZ() + (1 * Math.sin(Math.toRadians(loc.getYaw() + 0))));

        return new Vector(newX - loc.getX(), 0, newZ - loc.getZ());
    }

    public static Vector getRightVector(Location loc) {
        final float newX = (float) (loc.getX() + (-1 * Math.cos(Math.toRadians(loc.getYaw() + 0))));
        final float newZ = (float) (loc.getZ() + (-1 * Math.sin(Math.toRadians(loc.getYaw() + 0))));

        return new Vector(newX - loc.getX(), 0, newZ - loc.getZ());
    }

    private Location getEyeLocation() {
        return ((Mule)entity).getEyeLocation();
    }
}
