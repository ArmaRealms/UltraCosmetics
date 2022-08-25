package be.isach.ultracosmetics.v1_12_R1;

import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.treasurechests.TreasureChestDesign;
import be.isach.ultracosmetics.util.MathUtils;
import be.isach.ultracosmetics.util.Particles;
import be.isach.ultracosmetics.v1_12_R1.pathfinders.CustomPathFinderGoalPanic;
import be.isach.ultracosmetics.version.IEntityUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftBoat;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEnderDragon;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftWither;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.util.Vector;

import com.google.common.collect.Sets;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityArmorStand;
import net.minecraft.server.v1_12_R1.EntityBoat;
import net.minecraft.server.v1_12_R1.EntityCreature;
import net.minecraft.server.v1_12_R1.EntityEnderDragon;
import net.minecraft.server.v1_12_R1.EntityInsentient;
import net.minecraft.server.v1_12_R1.EnumItemSlot;
import net.minecraft.server.v1_12_R1.EnumMoveType;
import net.minecraft.server.v1_12_R1.Packet;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_12_R1.PathEntity;
import net.minecraft.server.v1_12_R1.PathfinderGoalSelector;
import net.minecraft.server.v1_12_R1.TileEntity;
import net.minecraft.server.v1_12_R1.Vector3f;
import net.minecraft.server.v1_12_R1.World;

/**
 * @author RadBuilder
 */
public class EntityUtil implements IEntityUtil {

    private Random r = new Random();
    private Map<Player,Set<EntityArmorStand>> fakeArmorStandsMap = new HashMap<>();
    private Map<Player,Set<org.bukkit.entity.Entity>> cooldownJumpMap = new HashMap<>();

    @Override
    public void resetWitherSize(Wither wither) {
        ((CraftWither) wither).getHandle().g(600);
    }

    @Override
    public void sendBlizzard(final Player player, Location loc, Function<org.bukkit.entity.Entity,Boolean> canAffectFunc, Vector v) {
        final Set<EntityArmorStand> fakeArmorStands = fakeArmorStandsMap.computeIfAbsent(player, k -> new HashSet<>());
        final Set<org.bukkit.entity.Entity> cooldownJump = cooldownJumpMap.computeIfAbsent(player, k -> new HashSet<>());
        final EntityArmorStand as = new EntityArmorStand(((CraftWorld) player.getWorld()).getHandle());
        as.setInvisible(true);
        as.setSmall(true);
        as.setNoGravity(true);
        as.setArms(true);
        as.setHeadPose(new Vector3f(r.nextInt(360), r.nextInt(360), r.nextInt(360)));
        as.setLocation(loc.getX() + MathUtils.randomDouble(-1.5, 1.5), loc.getY() + MathUtils.randomDouble(0, .5) - 0.75, loc.getZ() + MathUtils.randomDouble(-1.5, 1.5), 0, 0);
        fakeArmorStands.add(as);
        for (Player players : player.getWorld().getPlayers()) {
            sendPacket(players, new PacketPlayOutSpawnEntityLiving(as));
            sendPacket(players, new PacketPlayOutEntityEquipment(as.getId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(org.bukkit.Material.PACKED_ICE))));
        }
        Particles.CLOUD.display(loc.clone().add(MathUtils.randomDouble(-1.5, 1.5), MathUtils.randomDouble(0, .5) - 0.75, MathUtils.randomDouble(-1.5, 1.5)), 2, 0.4f);
        Bukkit.getScheduler().runTaskLater(UltraCosmeticsData.get().getPlugin(), () -> {
            for (Player pl : player.getWorld().getPlayers()) {
                sendPacket(pl, new PacketPlayOutEntityDestroy(as.getId()));
            }
            fakeArmorStands.remove(as);
        }, 20);
        as.getBukkitEntity().getNearbyEntities(0.5, 0.5, 0.5).stream().filter(ent -> !cooldownJump.contains(ent) && ent != player && canAffectFunc.apply(ent)).forEachOrdered(ent -> {
            MathUtils.applyVelocity(ent, new Vector(0, 1, 0).add(v));
            cooldownJump.add(ent);
            Bukkit.getScheduler().runTaskLater(UltraCosmeticsData.get().getPlugin(), () -> cooldownJump.remove(ent), 20);
        });
    }

    @Override
    public void clearBlizzard(Player player) {
        if (!fakeArmorStandsMap.containsKey(player)) return;

        for (EntityArmorStand as : fakeArmorStandsMap.get(player)) {
            if (as == null) {
                continue;
            }
            for (Player pl : player.getWorld().getPlayers()) {
                sendPacket(pl, new PacketPlayOutEntityDestroy(as.getId()));
            }
        }

        fakeArmorStandsMap.remove(player);
        cooldownJumpMap.remove(player);
    }

    @Override
    public void clearPathfinders(org.bukkit.entity.Entity entity) {
        Entity nmsEntity = ((CraftEntity) entity).getHandle();
        try {
            Field bField = PathfinderGoalSelector.class.getDeclaredField("b");
            bField.setAccessible(true);
            Field cField = PathfinderGoalSelector.class.getDeclaredField("c");
            cField.setAccessible(true);
            bField.set(((EntityInsentient) nmsEntity).goalSelector, Sets.newLinkedHashSet());
            bField.set(((EntityInsentient) nmsEntity).targetSelector, Sets.newLinkedHashSet());
            cField.set(((EntityInsentient) nmsEntity).goalSelector, Sets.newLinkedHashSet());
            cField.set(((EntityInsentient) nmsEntity).targetSelector, Sets.newLinkedHashSet());
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    @Override
    public void makePanic(org.bukkit.entity.Entity entity) {
        EntityInsentient insentient = (EntityInsentient) ((CraftEntity) entity).getHandle();
        insentient.goalSelector.a(3, new CustomPathFinderGoalPanic((EntityCreature) insentient));
    }

    @Override
    public void sendDestroyPacket(Player player, org.bukkit.entity.Entity entity) {
        sendPacket(player, new PacketPlayOutEntityDestroy(((CraftEntity) entity).getHandle().getId()));
    }

    @Override
    public void move(Creature creature, Location loc) {
        EntityCreature ec = ((CraftCreature) creature).getHandle();
        setStepHeight(creature);

        if (loc == null) return;

        ec.getNavigation().a(loc.getX(), loc.getY(), loc.getZ(), (1.0D + 2.0D * 0.5d) * 1.0D);
    }

    @Override
    public void moveDragon(Player player, Vector vector, org.bukkit.entity.Entity entity) {
        EntityEnderDragon ec = ((CraftEnderDragon) entity).getHandle();

        ec.hurtTicks = -1;

        ec.getBukkitEntity().setVelocity(vector);

        ec.pitch = player.getLocation().getPitch();
        ec.yaw = player.getLocation().getYaw() - 180;

        Vector v = ec.getBukkitEntity().getLocation().getDirection();
        ec.move(EnumMoveType.SELF, -v.getX(), v.getY(), -v.getZ());
    }

    @Override
    public void setStepHeight(org.bukkit.entity.Entity entity) {
        ((CraftEntity) entity).getHandle().P = 1;
    }

    @Override
    public void moveShip(Player player, org.bukkit.entity.Entity entity, Vector vector) {
        EntityBoat ec = ((CraftBoat) entity).getHandle();

        ec.getBukkitEntity().setVelocity(vector);

        ec.pitch = player.getLocation().getPitch();
        ec.yaw = player.getLocation().getYaw() - 180;

        ec.move(EnumMoveType.SELF, 1, 0, 0);
    }

    @Override
    public void playChestAnimation(Block b, boolean open, TreasureChestDesign design) {
        Location location = b.getLocation();
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition position = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntity tileChest = world.getTileEntity(position);
        world.playBlockAction(position, tileChest.getBlock(), 1, open ? 1 : 0);
    }

    @Override
    public void follow(org.bukkit.entity.Entity toFollow, org.bukkit.entity.Entity follower) {
        Entity pett = ((CraftEntity) follower).getHandle();
        ((EntityInsentient) pett).getNavigation().a(2);
        Object petf = ((CraftEntity) follower).getHandle();
        Location targetLocation = toFollow.getLocation();
        PathEntity path;
        path = ((EntityInsentient) petf).getNavigation().a(targetLocation.getX() + 1, targetLocation.getY(), targetLocation.getZ() + 1);
        if (path != null) {
            ((EntityInsentient) petf).getNavigation().a(path, 1.05D);
            ((EntityInsentient) petf).getNavigation().a(1.05D);
        }
    }

    @Override
    public void sendTeleportPacket(Player player, org.bukkit.entity.Entity entity) {
        sendPacket(player, new PacketPlayOutEntityTeleport(((CraftEntity) entity).getHandle()));
    }

    private void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
}
