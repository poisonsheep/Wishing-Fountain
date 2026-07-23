package io.github.poisonsheep.wishingfountain.item;

import io.github.poisonsheep.wishingfountain.config.CommonConfigs;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class WFStructureMap extends WFMapItem{
    private final ResourceLocation target_structure = BuiltinStructures.END_CITY.location();

    public WFStructureMap() {
        super();
        type = "structure";
    }

    @Override
    protected ResourceLocation getDefaultTarget() {
        return target_structure;
    }

    @Override
    protected ItemStack search(ItemStack stack, ServerLevel worldIn, Player player, int slot) {
        ResourceLocation target = getTarget(stack);
        if(target == null) {
            return ItemStack.EMPTY;
        }
        int centerX = (int) stack.getOrCreateTag().getDouble(SOURCE_X);
        int centerZ = (int) stack.getOrCreateTag().getDouble(SOURCE_Z);
        BlockPos centerPos = new BlockPos(centerX, 64, centerZ);
        SearchKey key = new SearchKey(GlobalPos.of(worldIn.dimension(), centerPos), target.toString());

        if(COMPUTING.contains(key)) {
            return stack;
        }

        InteractionResultHolder<BlockPos> result = RESULTS.get(key);
        if(result != null) {
            if(result.getResult() == InteractionResult.FAIL) {
                worldIn.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1F, 1F);
                return ItemStack.EMPTY;
            }
            BlockPos found = result.getObject();
            worldIn.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1F, 1F);
            return createMap(worldIn, found, target);
        }

        COMPUTING.add(key);
        EXECUTORS.submit(() -> {
            try {
                InteractionResultHolder<BlockPos> locateResult = searchStructure(worldIn, centerPos, target);
                RESULTS.put(key, locateResult);
            } finally {
                COMPUTING.remove(key);
            }
        });
        return stack;
    }

    public static Structure getStructureForKey(ServerLevel level, ResourceLocation key) {
        return getStructureRegistry(level).get(key);
    }

    private static Registry<Structure> getStructureRegistry(ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE);
    }

    public static Holder<Structure> getHolderForStructure(ServerLevel level, Structure structure) {
        Optional<ResourceKey<Structure>> optional = getStructureRegistry(level).getResourceKey(structure);
        return optional.map(structureResourceKey -> getStructureRegistry(level).getHolderOrThrow(structureResourceKey)).orElse(null);
    }

    public static InteractionResultHolder<BlockPos> searchStructure(ServerLevel worldIn, BlockPos startPos, ResourceLocation targetStructure) {
        Structure structure = getStructureForKey(worldIn, targetStructure);
        if (structure == null) {
            return InteractionResultHolder.fail(BlockPos.ZERO);
        }

        Holder<Structure> holder = getHolderForStructure(worldIn, structure);
        if (holder == null) {
            return InteractionResultHolder.fail(BlockPos.ZERO);
        }

        int radius = CommonConfigs.SEARCHING_RADIUS.get();
        Pair<BlockPos, Holder<Structure>> pair = worldIn.getChunkSource().getGenerator()
                .findNearestMapStructure(worldIn, HolderSet.direct(List.of(holder)), startPos, radius, false);
        if (pair != null) {
            return InteractionResultHolder.success(pair.getFirst());
        }
        return InteractionResultHolder.fail(BlockPos.ZERO);
    }


    public static void searchStructureAsync(ServerLevel worldIn, BlockPos startPos, ResourceLocation targetStructure, Consumer<InteractionResultHolder<BlockPos>> callback) {
        EXECUTORS.submit(() -> {
            try {
                InteractionResultHolder<BlockPos> result = searchStructure(worldIn, startPos, targetStructure);
                callback.accept(result);
            } catch (Exception e) {
                callback.accept(InteractionResultHolder.fail(BlockPos.ZERO));
            }
        });
    }

    public static void setTarget(ItemStack itemStack, String target) {
        itemStack.getOrCreateTag().putString(TARGET, target);
    }

    private record SearchKey(GlobalPos pos, String target) {}
    private static final Set<SearchKey> COMPUTING = ConcurrentHashMap.newKeySet();
    private static final Map<SearchKey, InteractionResultHolder<BlockPos>> RESULTS = new ConcurrentHashMap<>();
    protected static final ExecutorService EXECUTORS = Executors.newFixedThreadPool(2);
}
