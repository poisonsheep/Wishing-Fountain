package io.github.poisonsheep.wishingfountain.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.poisonsheep.wishingfountain.item.WFBiomeMapItem;
import io.github.poisonsheep.wishingfountain.item.WFStructureMap;
import io.github.poisonsheep.wishingfountain.registry.ItemRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WishingFountainCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("WishingFountainCommand");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder_debug =
                Commands.literal("wfdebug")
                        .requires(ctx -> ctx.hasPermission(4));
        final LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder_locate =
                Commands.literal("locate")
                        .requires(ctx -> ctx.hasPermission(4));
        literalArgumentBuilder_locate.then(Commands.literal("biome")
                .then(Commands.argument("biome", ResourceLocationArgument.id())
                        .suggests((context, builder) -> {
                            Registry<Biome> registry = context.getSource().registryAccess().registryOrThrow(Registries.BIOME);
                            return SharedSuggestionProvider.suggestResource(registry.keySet(), builder);
                        })
                        .executes(WishingFountainCommand::searchBiome)));

        literalArgumentBuilder_locate.then(Commands.literal("structure")
                .then(Commands.argument("structure", ResourceLocationArgument.id())
                        .suggests((context, builder) -> {
                            Registry<Structure> registry = context.getSource().registryAccess().registryOrThrow(Registries.STRUCTURE);
                            return SharedSuggestionProvider.suggestResource(registry.keySet(), builder);
                        })
                        .executes(WishingFountainCommand::searchStructure)));
        literalArgumentBuilder_debug.then(literalArgumentBuilder_locate);
        dispatcher.register(literalArgumentBuilder_debug);
    }

    private static int searchBiome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation biomeId = ResourceLocationArgument.getId(context, "biome");

        if (!isBiomeRegistered(player, biomeId)) {
            source.sendFailure(Component.literal("§c未知的群系: " + biomeId));
            return 0;
        }

        ItemStack bottle = new ItemStack(ItemRegistry.WF_BIOME_MAP.get());
        WFBiomeMapItem.setTarget(bottle, biomeId.toString());
        player.getInventory().add(bottle);
        source.sendSuccess(() -> Component.literal("§a已给予群系 §e" + biomeId + " §a的许愿瓶"), false);
        return 1;
    }

    private static int searchStructure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation structureId = ResourceLocationArgument.getId(context, "structure");

        if (!isStructureRegistered(player, structureId)) {
            source.sendFailure(Component.literal("§c未知的结构: " + structureId));
            return 0;
        }

        ItemStack bottle = new ItemStack(ItemRegistry.WF_STRUCTURE_MAP.get());
        WFStructureMap.setTarget(bottle, structureId.toString());
        player.getInventory().add(bottle);
        source.sendSuccess(() -> Component.literal("§a已给予结构 §e" + structureId + " §a的许愿瓶"), false);
        return 1;
    }

    private static boolean isBiomeRegistered(ServerPlayer player, ResourceLocation biomeId) {
        Registry<Biome> registry = player.server.registryAccess().registryOrThrow(Registries.BIOME);
        return registry.containsKey(biomeId);
    }

    private static boolean isStructureRegistered(ServerPlayer player, ResourceLocation structureId) {
        Registry<Structure> registry = player.server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        return registry.containsKey(structureId);
    }
}
