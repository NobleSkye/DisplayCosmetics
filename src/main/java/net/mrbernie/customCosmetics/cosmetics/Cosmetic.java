package net.mrbernie.customCosmetics.cosmetics;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.Map;


public record Cosmetic(
        String id,
        String displayName,
        CosmeticType type,
        Map<Vector, BlockData> blocks
) {}