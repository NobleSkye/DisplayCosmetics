package net.mrbernie.customCosmetics.creation;

import net.mrbernie.customCosmetics.cosmetics.CosmeticType;
import org.bukkit.Location;

public record CreationSession(
        String cosmeticName,
        CosmeticType cosmeticType,
        Location originalLocation,
        Location buildAreaCorner
) {}