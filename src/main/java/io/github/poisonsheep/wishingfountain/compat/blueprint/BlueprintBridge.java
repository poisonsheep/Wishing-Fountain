package io.github.poisonsheep.wishingfountain.compat.blueprint;

import io.github.poisonsheep.thearbiter.capability.PlayerBlueprintProvider;
import net.minecraft.world.entity.player.Player;

/**
 * Bridge class for optional Blueprint mod integration.
 * This class is only loaded via reflection when Blueprint is present,
 * preventing NoClassDefFoundError when Blueprint is absent.
 */
public class BlueprintBridge {

    public static boolean hasBlueprint(Player player, String blueprintId) {
        return player.getCapability(PlayerBlueprintProvider.PLAYER_BLUEPRINT_CAPABILITY)
            .map(pb -> pb.getBlueprints().contains(blueprintId))
            .orElse(true);
    }
}
