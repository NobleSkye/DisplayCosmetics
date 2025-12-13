package net.mrbernie.customCosmetics.cosmetics;

import net.mrbernie.customCosmetics.CustomCosmetics;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerSideCosmeticDisplay {

    private final CustomCosmetics plugin;
    private final Player owner;
    private final Cosmetic cosmetic;
    private final List<BlockDisplay> displayEntities = new ArrayList<>();
    private static final float PIXEL_SCALE = 0.0625f;

    public ServerSideCosmeticDisplay(CustomCosmetics plugin, Player owner, Cosmetic cosmetic) {
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
            
            // Rotate the offset vector so all blocks spawn in correct orientation
            Vector rotatedOffset = rotateAroundY(new Vector(offsetX, offsetY, offsetZ), yaw);
            
            Location displayLoc = headLoc.clone().add(rotatedOffset);
            
            BlockDisplay display = headLoc.getWorld().spawn(displayLoc, BlockDisplay.class, entity -> {
                entity.setBlock(blockData);
                entity.setBrightness(new Display.Brightness(15, 15));
                
                // Set scale to 1 pixel (1/16th of a block)
                Transformation transform = entity.getTransformation();
                transform.getScale().set(PIXEL_SCALE);
                entity.setTransformation(transform);
                
                entity.setViewRange(128.0f);
                entity.setPersistent(false);
            });
            
            displayEntities.add(display);
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
            if (index >= displayEntities.size()) break;
            
            BlockDisplay display = displayEntities.get(index);
            if (display == null || !display.isValid()) {
                continue;
            }
            
            Vector offset = entry.getKey();
            // Scale the offset to pixel size
            float offsetX = (float) offset.getX() * PIXEL_SCALE;
            float offsetY = (float) offset.getY() * PIXEL_SCALE;
            float offsetZ = (float) offset.getZ() * PIXEL_SCALE;
            
            // Rotate the offset vector based on player's yaw so all blocks rotate together
            Vector rotatedOffset = rotateAroundY(new Vector(offsetX, offsetY, offsetZ), yaw);
            
            Location newLoc = headLoc.clone().add(rotatedOffset);
            display.teleport(newLoc);
            
            index++;
        }
    }
    
    private Vector rotateAroundY(Vector vec, float yaw) {
        double angle = Math.toRadians(yaw); // Minecraft yaw increases clockwise (right)
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        
        double newX = vec.getX() * cos - vec.getZ() * sin;
        double newZ = vec.getX() * sin + vec.getZ() * cos;
        
        return new Vector(newX, vec.getY(), newZ);
    }

    public void destroy() {
        displayEntities.forEach(display -> {
            if (display != null && display.isValid()) {
                display.remove();
            }
        });
        displayEntities.clear();
    }

    public boolean isValid() {
        return owner.isOnline() && !displayEntities.isEmpty();
    }

    public Player getOwner() {
        return owner;
    }

    public Cosmetic getCosmetic() {
        return cosmetic;
    }

    public void hideFrom(Player viewer) {
        for (BlockDisplay display : displayEntities) {
            if (display != null && display.isValid()) {
                viewer.hideEntity(plugin, display);
            }
        }
    }

    public void showTo(Player viewer) {
        for (BlockDisplay display : displayEntities) {
            if (display != null && display.isValid()) {
                viewer.showEntity(plugin, display);
            }
        }
    }

    public List<BlockDisplay> getDisplayEntities() {
        return displayEntities;
    }

    private Location getHeadLocation() {
        Location loc;
        
        // HEAD uses eye location (head yaw), BACK and SHOULDER use body location (body yaw)
        if (cosmetic.type() == CosmeticType.HEAD) {
            loc = owner.getEyeLocation().clone();
        } else {
            loc = owner.getLocation().clone();
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
