package io.github.poisonsheep.wishingfountain.tileentity;

import io.github.poisonsheep.wishingfountain.registry.BlockRegistry;
import io.github.poisonsheep.wishingfountain.util.PosListData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class WFEntity extends BlockEntity {

    public static final BlockEntityType<WFEntity> TYPE = BlockEntityType.Builder.of(WFEntity::new, BlockRegistry.WISHING_FOUNTAIN.get()).build(null);
    private static final String STORAGE_ITEM = "StorageItem";
    private static final String STORAGE_STATE_ID = "StorageBlockStateId";
    private static final String STORAGE_BLOCK_LIST = "StorageBlockList";
    private static final String IS_RENDER = "IsRender";
    private static final String DIRECTION = "Direction";
    private static final String CAN_PLACE_ITEM = "CanPlaceItem";
    private static final String TEXTURE_INDEX = "TextureIndex";
    private BlockState storageState = Blocks.AIR.defaultBlockState();
    private PosListData blockPosList = new PosListData();
    private boolean isRender = false;
    private Direction direction = Direction.SOUTH;
    private boolean canPlaceItem = false;
    private int textureIndex = 0;
    public final ItemStackHandler handler = new ItemStackHandler(1);

    public WFEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    public BlockState getStorageState() {
        return storageState;
    }

    public PosListData getBlockPosList() {
        return blockPosList;
    }

    public void setForgeData(boolean isRender, boolean canPlaceItem, BlockState storageState, Direction direction, PosListData blockPosList) {
        this.isRender = isRender;
        this.canPlaceItem = canPlaceItem;
        this.storageState = storageState;
        this.direction = direction;
        this.blockPosList = blockPosList;
        refresh();
    }

    public void addItem(ItemEntity entity) {
        ItemStack outside = entity.getItem().copy();
        ItemStack inside = this.getStorageItem();
        if((inside.isEmpty() || inside.getItem().equals(outside.getItem())) && inside.getCount() < 8 && inside.getCount() < outside.getMaxStackSize()) {
            ItemStack singleItemStack = outside.split(1);
            this.handler.insertItem(0, singleItemStack, false);
        }
        entity.setItem(outside);
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        getPersistentData().putBoolean(IS_RENDER, isRender);
        getPersistentData().putBoolean(CAN_PLACE_ITEM, canPlaceItem);
        getPersistentData().putInt(STORAGE_STATE_ID, Block.getId(storageState));
        getPersistentData().putInt(TEXTURE_INDEX, textureIndex);
        getPersistentData().putString(DIRECTION, direction.getSerializedName());
        getPersistentData().put(STORAGE_BLOCK_LIST, blockPosList.serialize());
        getPersistentData().put(STORAGE_ITEM, handler.serializeNBT());
        super.saveAdditional(compound);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        isRender = getPersistentData().getBoolean(IS_RENDER);
        canPlaceItem = getPersistentData().getBoolean(CAN_PLACE_ITEM);
        storageState = Block.stateById(getPersistentData().getInt(STORAGE_STATE_ID));
        textureIndex = getPersistentData().getInt(TEXTURE_INDEX);
        direction = Direction.byName(getPersistentData().getString(DIRECTION));
        if (direction == null) {
            direction = Direction.SOUTH;
        }
        blockPosList.deserialize(getPersistentData().getList(STORAGE_BLOCK_LIST, Tag.TAG_COMPOUND));
        handler.deserializeNBT(getPersistentData().getCompound(STORAGE_ITEM));
    }

    public ItemStack getStorageItem() {
        return handler.getStackInSlot(0);
    }

    public void refresh() {
        this.setChanged();
        if(level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }
    public boolean isRender() {
        return isRender;
    }

    public boolean isCanPlaceItem() {
        return canPlaceItem;
    }

    public Direction getDirection() {return direction;}

    public int getTextureIndex() {return textureIndex;}

    public void setTextureIndex(int textureIndex) {
        this.textureIndex = textureIndex;
        refresh();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-3, -2, -3), worldPosition.offset(3, 2, 3));
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
