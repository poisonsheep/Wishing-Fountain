package io.github.poisonsheep.wishingfountain.block.multiblock;

import io.github.poisonsheep.wishingfountain.WishingFountain;
import io.github.poisonsheep.wishingfountain.block.WFBlock;
import io.github.poisonsheep.wishingfountain.registry.SoundRegistry;
import io.github.poisonsheep.wishingfountain.tileentity.WFEntity;
import io.github.poisonsheep.wishingfountain.util.PosListData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Arrays;
import java.util.List;

public class WFMultiBlock implements IMultiBlock {

    private static final ResourceLocation WISHING_FOUNTAIN_TARGET = ResourceLocation.parse(WishingFountain.MODID + ":wishing_fountain_target");
    private static final ResourceLocation WISHING_FOUNTAIN_STRUCTURE = ResourceLocation.parse(WishingFountain.MODID + ":wishing_fountain_structure");

    //喷泉中心相对于初始位置的坐标
    private static final BlockPos CENTER = new BlockPos(2, 2 ,2);
    //列出喷泉底部八个方块相对坐标列表
    private static final List<BlockPos> BOTTOM = Arrays.asList(
            new BlockPos(1, 0, 1),
            new BlockPos(2, 0, 1),
            new BlockPos(3, 0, 1),
            new BlockPos(3, 0, 2),
            new BlockPos(3, 0, 3),
            new BlockPos(2, 0, 3),
            new BlockPos(1, 0, 3),
            new BlockPos(1, 0, 2)
    );

    @Override
    public boolean isCoreBlock(BlockState blockState) {
        return blockState.is(Blocks.SMOOTH_QUARTZ_SLAB);
    }

    @Override
    public boolean isMatch(Level world, BlockPos posStart, StructureTemplate target) {
        StructureTemplate.Palette palette = target.palettes.get(0);
        for (StructureTemplate.StructureBlockInfo blockInfo : palette.blocks()) {
            BlockState worldState = world.getBlockState(posStart.offset(blockInfo.pos()));
            BlockState infoState = blockInfo.state();
            if (!worldState.equals(infoState)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void build(Level worldIn, BlockPos posStart, StructureTemplate wishingFountain, Direction direction) {
        StructureTemplate.Palette palette = wishingFountain.palettes.get(0);
        PosListData posList = new PosListData();
        BlockPos center = posStart.offset(CENTER);
        //记录之前方块的位置
        for (StructureTemplate.StructureBlockInfo blockInfo : palette.blocks()) {
            posList.add(posStart.offset(blockInfo.pos()));
        }
        for (StructureTemplate.StructureBlockInfo blockInfo : palette.blocks()) {
            BlockPos currentPos = posStart.offset(blockInfo.pos());
            BlockState currentState = worldIn.getBlockState(currentPos);
            BlockState targetState = blockInfo.state();
            worldIn.setBlock(currentPos, targetState, Block.UPDATE_ALL);
            BlockEntity entity = worldIn.getBlockEntity(currentPos);
            if (entity instanceof WFEntity) {
                boolean isRender = currentPos.equals(center);
                boolean cnPlaceItem = targetState.getValue(WFBlock.FOUNTAIN).equals(2);
                ((WFEntity) entity).setForgeData(isRender, cnPlaceItem, currentState, direction, posList);
            }
        }
        worldIn.playSound(null, posStart, SoundRegistry.BUILD.get(), SoundSource.AMBIENT, 1F, 1F);
        for (BlockPos bottomPos : getBottomPos()) {
            BlockPos worldPos = posStart.offset(bottomPos);
            ((ServerLevel) worldIn).sendParticles(ParticleTypes.CLOUD, worldPos.getX() + 0.5, worldPos.getY() + 1, worldPos.getZ() + 0.5, 10, 0, 0, 0, 0.1);
        }
    }

    @Override
    public List<BlockPos> getBottomPos() {
        return BOTTOM;
    }

    @Override
    public StructureTemplate getTemplateStructure(ServerLevel world) {
        return getWishingFountainTemplate(world, WISHING_FOUNTAIN_STRUCTURE);
    }

    @Override
    public StructureTemplate getTemplateTarget(ServerLevel world) {
        return getWishingFountainTemplate(world, WISHING_FOUNTAIN_TARGET);
    }

    private StructureTemplate getWishingFountainTemplate(ServerLevel world, ResourceLocation location) {
        return world.getStructureManager().getOrCreate(location);
    }

}
