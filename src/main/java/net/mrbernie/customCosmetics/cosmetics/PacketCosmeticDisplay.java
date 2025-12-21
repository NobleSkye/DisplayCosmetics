package net.mrbernie.customCosmetics.cosmetics;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.mrbernie.customCosmetics.CustomCosmetics;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PacketCosmeticDisplay {

    private final CustomCosmetics plugin;
    private final Player owner;
    private final Cosmetic cosmetic;
    private final List<Integer> entityIds = new ArrayList<>();
    private final Map<Integer, BlockData> entityBlocks = new HashMap<>();
    private static final float PIXEL_SCALE = 0.0625f;
    private final Set<UUID> viewers = new HashSet<>();

    public PacketCosmeticDisplay(CustomCosmetics plugin, Player owner, Cosmetic cosmetic) {
        this.plugin = plugin;
        this.owner = owner;
        this.cosmetic = cosmetic;
    }

    public void spawn() {
        Location headLoc = getHeadLocation();
        float yaw = headLoc.getYaw();
        
        for (Map.Entry<Vector, BlockData> entry : cosmetic.blocks().entrySet()) {
            Vector offset = entry.getKey();
            BlockData blockData = entry.getValue();
            
            // Calculate display position with rotation
            float offsetX = (float) offset.getX() * PIXEL_SCALE;
            float offsetY = (float) offset.getY() * PIXEL_SCALE;
            float offsetZ = (float) offset.getZ() * PIXEL_SCALE;
            
            // Rotate the offset vector
            Vector rotatedOffset = rotateAroundY(new Vector(offsetX, offsetY, offsetZ), yaw);
            Location displayLoc = headLoc.clone().add(rotatedOffset);
            
            // Generate unique entity ID
            int entityId = ThreadLocalRandom.current().nextInt(100000, Integer.MAX_VALUE);
            entityIds.add(entityId);
            entityBlocks.put(entityId, blockData);
            
            // Send spawn packet to all nearby players
            for (Player viewer : owner.getWorld().getPlayers()) {
                if (shouldShowTo(viewer)) {
                    sendSpawnPacket(viewer, entityId, displayLoc, blockData);
                    viewers.add(viewer.getUniqueId());
                }
            }
        }
    }

    private void sendSpawnPacket(Player viewer, int entityId, Location loc, BlockData blockData) {
        try {
            // Spawn block display entity packet
            PacketContainer spawnPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawnPacket.getIntegers()
                .write(0, entityId) // Entity ID
                .write(1, 0) // Velocity X
                .write(2, 0) // Velocity Y
                .write(3, 0); // Velocity Z
            
            spawnPacket.getUUIDs().write(0, UUID.randomUUID()); // Entity UUID
            spawnPacket.getEntityTypeModifier().write(0, EntityType.BLOCK_DISPLAY);
            
            spawnPacket.getDoubles()
                .write(0, loc.getX())
                .write(1, loc.getY())
                .write(2, loc.getZ());
            
            spawnPacket.getBytes()
                .write(0, (byte) 0) // Pitch
                .write(1, (byte) 0); // Yaw
            
            spawnPacket.getIntegers().write(4, 0); // Data (0 for block display)
            
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, spawnPacket);
            
            // Send metadata packet for block display
            sendMetadataPacket(viewer, entityId, blockData);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send spawn packet: " + e.getMessage());
        }
    }

    private void sendMetadataPacket(Player viewer, int entityId, BlockData blockData) {
        try {
            PacketContainer metadataPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);
            
            List<WrappedDataValue> metadata = new ArrayList<>();
            
            // Block state (index 23 for BlockDisplay)
            WrappedDataWatcher.Serializer blockStateSerializer = WrappedDataWatcher.Registry.getBlockDataSerializer(false);
            metadata.add(new WrappedDataValue(23, blockStateSerializer, blockData));
            
            // Transformation (index 11 for Display entities)
            Transformation transform = new Transformation(
                new Vector3f(0, 0, 0), // Translation
                new Quaternionf(0, 0, 0, 1), // Left rotation
                new Vector3f(PIXEL_SCALE, PIXEL_SCALE, PIXEL_SCALE), // Scale
                new Quaternionf(0, 0, 0, 1) // Right rotation
            );
            
            WrappedDataWatcher.Serializer transformSerializer = WrappedDataWatcher.Registry.get(com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry.getChatComponentSerializer(false).getClass());
            // Note: Transformation serialization may need adjustment based on ProtocolLib version
            
            // Brightness (index 16 for Display entities) - full bright
            WrappedDataWatcher.Serializer intSerializer = WrappedDataWatcher.Registry.get(Integer.class);
            metadata.add(new WrappedDataValue(16, intSerializer, (15 << 20) | (15 << 4))); // Sky light 15, Block light 15
            
            metadataPacket.getDataValueCollectionModifier().write(0, metadata);
            
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, metadataPacket);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send metadata packet: " + e.getMessage());
        }
    }

    public void update() {
        if (!owner.isOnline()) {
            destroy();
            return;
        }
        
        Location headLoc = getHeadLocation();
        float yaw = headLoc.getYaw();
        int index = 0;
        
        for (Map.Entry<Vector, BlockData> entry : cosmetic.blocks().entrySet()) {
            if (index >= entityIds.size()) break;
            
            int entityId = entityIds.get(index);
            Vector offset = entry.getKey();
            
            // Scale the offset to pixel size
            float offsetX = (float) offset.getX() * PIXEL_SCALE;
            float offsetY = (float) offset.getY() * PIXEL_SCALE;
            float offsetZ = (float) offset.getZ() * PIXEL_SCALE;
            
            // Rotate the offset vector
            Vector rotatedOffset = rotateAroundY(new Vector(offsetX, offsetY, offsetZ), yaw);
            Location newLoc = headLoc.clone().add(rotatedOffset);
            
            // Send teleport packet to all viewers
            for (Player viewer : owner.getWorld().getPlayers()) {
                if (viewers.contains(viewer.getUniqueId()) && shouldShowTo(viewer)) {
                    sendTeleportPacket(viewer, entityId, newLoc);
                }
            }
            
            index++;
        }
    }

    private void sendTeleportPacket(Player viewer, int entityId, Location loc) {
        try {
            PacketContainer teleportPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
            
            // Set entity ID using modifier
            teleportPacket.getModifier().write(0, entityId);
            
            // Set position (X, Y, Z)
            teleportPacket.getDoubles()
                .write(0, loc.getX())
                .write(1, loc.getY())
                .write(2, loc.getZ());
            
            // Yaw and pitch (convert to byte representation: degrees * 256 / 360)
            byte yaw = (byte) ((loc.getYaw() * 256.0F) / 360.0F);
            byte pitch = (byte) ((loc.getPitch() * 256.0F) / 360.0F);
            teleportPacket.getBytes()
                .write(0, yaw)
                .write(1, pitch);
            
            // On ground flag
            teleportPacket.getBooleans().write(0, true);
            
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, teleportPacket);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send teleport packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Vector rotateAroundY(Vector vec, float yaw) {
        double angle = Math.toRadians(yaw);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        
        double newX = vec.getX() * cos - vec.getZ() * sin;
        double newZ = vec.getX() * sin + vec.getZ() * cos;
        
        return new Vector(newX, vec.getY(), newZ);
    }

    public void destroy() {
        for (Player viewer : owner.getWorld().getPlayers()) {
            if (viewers.contains(viewer.getUniqueId())) {
                sendDestroyPackets(viewer);
            }
        }
        entityIds.clear();
        entityBlocks.clear();
        viewers.clear();
    }

    private void sendDestroyPackets(Player viewer) {
        try {
            PacketContainer destroyPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getIntLists().write(0, new ArrayList<>(entityIds));
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, destroyPacket);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send destroy packet: " + e.getMessage());
        }
    }

    public boolean isValid() {
        return owner.isOnline() && !entityIds.isEmpty();
    }

    public Player getOwner() {
        return owner;
    }

    public Cosmetic getCosmetic() {
        return cosmetic;
    }

    public void hideFrom(Player viewer) {
        if (viewers.contains(viewer.getUniqueId())) {
            sendDestroyPackets(viewer);
            viewers.remove(viewer.getUniqueId());
        }
    }

    public void showTo(Player viewer) {
        if (!viewers.contains(viewer.getUniqueId())) {
            // Respawn all entities for this viewer
            Location headLoc = getHeadLocation();
            float yaw = headLoc.getYaw();
            int index = 0;
            
            for (Map.Entry<Vector, BlockData> entry : cosmetic.blocks().entrySet()) {
                if (index >= entityIds.size()) break;
                
                int entityId = entityIds.get(index);
                BlockData blockData = entry.getValue();
                Vector offset = entry.getKey();
                
                float offsetX = (float) offset.getX() * PIXEL_SCALE;
                float offsetY = (float) offset.getY() * PIXEL_SCALE;
                float offsetZ = (float) offset.getZ() * PIXEL_SCALE;
                
                Vector rotatedOffset = rotateAroundY(new Vector(offsetX, offsetY, offsetZ), yaw);
                Location displayLoc = headLoc.clone().add(rotatedOffset);
                
                sendSpawnPacket(viewer, entityId, displayLoc, blockData);
                index++;
            }
            
            viewers.add(viewer.getUniqueId());
        }
    }

    private boolean shouldShowTo(Player viewer) {
        if (viewer.equals(owner)) {
            return plugin.getVisibilityManager().canSeeSelf(owner);
        } else {
            return plugin.getVisibilityManager().canSeeOthers(viewer);
        }
    }

    public List<Integer> getEntityIds() {
        return entityIds;
    }

    private Location getHeadLocation() {
        Location loc;
        
        // HEAD uses eye location (head yaw), BACK and SHOULDER use body location (body yaw)
        if (cosmetic.type() == CosmeticType.HEAD) {
            loc = owner.getEyeLocation().clone();
        } else {
            // For BACK and SHOULDER, use body location with body yaw
            loc = owner.getLocation().clone();
            loc.setYaw(owner.getBodyYaw()); // Use actual body yaw instead of head yaw
        }
        
        loc.setPitch(0); // Remove pitch so cosmetics don't tilt
        
        switch (cosmetic.type()) {
            case HEAD:
                loc.add(0, 0.25, 0);
                break;
            case BACK:
                loc.add(0, owner.getEyeHeight() - 0.5, 0);
                // Use yaw-only direction for back cosmetics
                double yaw = Math.toRadians(loc.getYaw() + 90);
                double x = Math.cos(yaw) * -0.3;
                double z = Math.sin(yaw) * -0.3;
                loc.add(x, 0, z);
                break;
            case SHOULDER:
                loc.add(0, owner.getEyeHeight(), 0);
                break;
        }
        
        return loc;
    }
}
