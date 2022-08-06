package be.isach.ultracosmetics.v1_8_R3;

import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.treasurechests.TreasureChestDesign;
import be.isach.ultracosmetics.util.MathUtils;
import be.isach.ultracosmetics.util.PacketSender;
import be.isach.ultracosmetics.util.Particles;
import be.isach.ultracosmetics.v1_8_R3.pathfinders.CustomPathFinderGoalPanic;
import be.isach.ultracosmetics.version.IEntityUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftBoat;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEnderDragon;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftWither;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.util.UnsafeList;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EntityBoat;
import net.minecraft.server.v1_8_R3.EntityCreature;
import net.minecraft.server.v1_8_R3.EntityEnderDragon;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_8_R3.PathEntity;
import net.minecraft.server.v1_8_R3.PathfinderGoalSelector;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.Vector3f;
import net.minecraft.server.v1_8_R3.World;

/**
 * Created by Sacha on 14/03/16.
 */
public class EntityUtil implements IEntityUtil {
    private final Random r = new Random();
    private final Map<Player,Set<EntityArmorStand>> fakeArmorStandsMap = new HashMap<>();
    private final Map<Player,Set<Entity>> cooldownJumpMap = new HashMap<>();

    @Override
    public void resetWitherSize(Wither wither) {
        ((CraftWither) wither).getHandle().r(600);
    }

    @Override
    public void sendBlizzard(final Player player, Location loc, Function<Entity,Boolean> canAffectFunc, Vector v) {
        final Set<EntityArmorStand> fakeArmorStands = fakeArmorStandsMap.computeIfAbsent(player, k -> new HashSet<>());
        final Set<Entity> cooldownJump = cooldownJumpMap.computeIfAbsent(player, k -> new HashSet<>());
        final EntityArmorStand as = new EntityArmorStand(((CraftWorld) player.getWorld()).getHandle());
        as.setInvisible(true);
        as.setSmall(true);
        as.setGravity(false);
        as.setArms(true);
        as.setHeadPose(new Vector3f(r.nextInt(360), r.nextInt(360), r.nextInt(360)));
        as.setLocation(loc.getX() + MathUtils.randomDouble(-1.5, 1.5), loc.getY() + MathUtils.randomDouble(0, .5) - 0.75, loc.getZ() + MathUtils.randomDouble(-1.5, 1.5), 0, 0);
        fakeArmorStands.add(as);
        for (Player players : player.getWorld().getPlayers()) {
            PacketSender.send(players, new PacketPlayOutSpawnEntityLiving(as));
            PacketSender.send(players, new PacketPlayOutEntityEquipment(as.getId(), 4, CraftItemStack.asNMSCopy(new ItemStack(org.bukkit.Material.PACKED_ICE))));
        }
        Particles.CLOUD.display(loc.clone().add(MathUtils.randomDouble(-1.5, 1.5), MathUtils.randomDouble(0, .5) - 0.75, MathUtils.randomDouble(-1.5, 1.5)), 2, 0.4f);
        Bukkit.getScheduler().runTaskLater(UltraCosmeticsData.get().getPlugin(), () -> {
            for (Player pl : player.getWorld().getPlayers()) {
                if (as == null) {
                    continue;
                }
                PacketSender.send(pl, new PacketPlayOutEntityDestroy(as.getId()));
            }
            fakeArmorStands.remove(as);
        }, 20);
        for (final Entity ent : as.getBukkitEntity().getNearbyEntities(0.5, 0.5, 0.5)) {
            if (!cooldownJump.contains(ent) && ent != player && canAffectFunc.apply(ent)) {
                MathUtils.applyVelocity(ent, new Vector(0, 1, 0).add(v));
                cooldownJump.add(ent);
                Bukkit.getScheduler().runTaskLater(UltraCosmeticsData.get().getPlugin(), () -> cooldownJump.remove(ent), 20);
            }
        }
    }

    @Override
    public void clearBlizzard(Player player) {
        if (!fakeArmorStandsMap.containsKey(player)) return;

        for (EntityArmorStand as : fakeArmorStandsMap.get(player))
            for (Player pl : player.getWorld().getPlayers())
                PacketSender.send(pl, new PacketPlayOutEntityDestroy(as.getId()));
        fakeArmorStandsMap.remove(player);
        cooldownJumpMap.remove(player);
    }

    @Override
    public void clearPathfinders(Entity entity) {
        EntityInsentient entitySheep = (EntityInsentient) ((CraftEntity) entity).getHandle();

        try {
            Field bField = PathfinderGoalSelector.class.getDeclaredField("b");
            bField.setAccessible(true);
            Field cField = PathfinderGoalSelector.class.getDeclaredField("c");
            cField.setAccessible(true);
            bField.set(entitySheep.goalSelector, new UnsafeList<PathfinderGoalSelector>());
            bField.set(entitySheep.targetSelector, new UnsafeList<PathfinderGoalSelector>());
            cField.set(entitySheep.goalSelector, new UnsafeList<PathfinderGoalSelector>());
            cField.set(entitySheep.targetSelector, new UnsafeList<PathfinderGoalSelector>());
        } catch (ReflectiveOperationException exc) {
            exc.printStackTrace();
        }
    }

    @Override
    public void makePanic(Entity entity) {
        EntityInsentient insentient = (EntityInsentient) ((CraftEntity) entity).getHandle();
        insentient.goalSelector.a(3, new CustomPathFinderGoalPanic((EntityCreature) insentient, 0.4d));
    }

    @Override
    public void sendDestroyPacket(Player player, Entity entity) {
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(((CraftEntity) entity).getHandle().getId());
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    @Override
    public void move(Creature creature, Location loc) {
        EntityCreature ec = ((CraftCreature) creature).getHandle();
        ec.S = 1;

        if (loc == null) return;

        ec.getNavigation().a(loc.getX(), loc.getY(), loc.getZ(), (1.0D + 2.0D * 0.5d) * 1.0D);
    }

    @Override
    public void moveDragon(Player player, Vector vector, Entity entity) {
        EntityEnderDragon ec = ((CraftEnderDragon) entity).getHandle();

        ec.hurtTicks = -1;

        ec.getBukkitEntity().setVelocity(vector);

        ec.pitch = player.getLocation().getPitch();
        ec.yaw = player.getLocation().getYaw() - 180;
    }

    @Override
    public void setClimb(Entity entity) {
        ((CraftEntity) entity).getHandle().S = 1;
    }

    @Override
    public void moveShip(Player player, Entity entity, Vector vector) {
        EntityBoat ec = ((CraftBoat) entity).getHandle();

        ec.getBukkitEntity().setVelocity(vector);

        ec.pitch = player.getLocation().getPitch();
        ec.yaw = player.getLocation().getYaw() - 180;
    }

    @Override
    public void playChestAnimation(Block b, boolean open, TreasureChestDesign design) {
        Location location = b.getLocation();
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition position = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntity tileChest = world.getTileEntity(position);
        world.playBlockAction(position, tileChest.w(), 1, open ? 1 : 0);
    }

    @Override
    public void follow(Entity toFollow, Entity follower) {
        net.minecraft.server.v1_8_R3.Entity pett = ((CraftEntity) follower).getHandle();
        ((EntityInsentient) pett).getNavigation().a(2);
        Object petf = ((CraftEntity) follower).getHandle();
        Location targetLocation = toFollow.getLocation();
        PathEntity path = ((EntityInsentient) petf).getNavigation().a(targetLocation.getX() + 1, targetLocation.getY(), targetLocation.getZ() + 1);
        if (path != null) {
            ((EntityInsentient) petf).getNavigation().a(path, 1.05D);
            ((EntityInsentient) petf).getNavigation().a(1.05D);
        }
    }

    @Override
    public void chickenFall(Player player) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        if (!entityPlayer.onGround && entityPlayer.motY < 0.0D) {
            Vector v = player.getVelocity();
            player.setVelocity(v);
            entityPlayer.motY *= 0.85;
        }
    }

    @Override
    public void sendTeleportPacket(Player player, Entity entity) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityTeleport(((CraftEntity) entity).getHandle()));
    }

    @Override
    public boolean isMoving(org.bukkit.entity.Player entity) {
        EntityPlayer ent = ((CraftPlayer) entity).getHandle();
        long time = System.currentTimeMillis() - ent.D();
        if (time > 0.001) {
            return true;
        }
        return false;
    }
}
