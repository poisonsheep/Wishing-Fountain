package io.github.poisonsheep.wishingfountain.item;

import io.github.poisonsheep.wishingfountain.config.CommonConfigs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

abstract class WFMapItem extends Item {

    protected static final Direction[] DIRECTIONS = new Direction[] { Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH };
    protected static final String IS_SEARCHING = "isSearching";
    protected static final String SOURCE_X = "searchSourceX";
    protected static final String SOURCE_Z = "searchSourceZ";
    protected static final String POS_X = "searchPosX";
    protected static final String POS_Z = "searchPosZ";
    protected static final String POS_LEG = "searchPosLeg";
    protected static final String POS_LEG_INDEX = "searchPosLegIndex";
    protected static final String DISTANCE = "distance";
    protected static final String TARGET = "target";
    protected String type;

    public WFMapItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player player, InteractionHand hand) {
        ItemStack handStack = player.getItemInHand(hand);
        ItemStack searchingMap = getSearchingMap(player);
        if(searchingMap != null) {
            player.displayClientMessage(Component.translatable("wishing_fountain.misc.only_one_map_searching"), true);
            return InteractionResultHolder.fail(handStack);
        }
        Vec3 pos = player.getPosition(1F);
        handStack.getOrCreateTag().putBoolean(IS_SEARCHING, true);
        handStack.getOrCreateTag().putInt(SOURCE_X, (int) pos.x);
        handStack.getOrCreateTag().putInt(SOURCE_Z, (int) pos.z);
        handStack.getOrCreateTag().putDouble(DISTANCE, Double.MAX_VALUE);
        playSound(worldIn, player);
        return InteractionResultHolder.sidedSuccess(handStack, worldIn.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entity, int slot, boolean hand) {
        if (stack.getTag() != null && stack.getTag().getBoolean(IS_SEARCHING) && entity instanceof Player player) {
            if(worldIn instanceof ServerLevel server) {
                ItemStack runningStack = search(stack, server, player, slot);
                if(player.getOffhandItem() == stack){
                    player.setItemInHand(InteractionHand.OFF_HAND, runningStack);
                } else {
                    player.getInventory().setItem(slot, runningStack);
                }
            }
            if((stack.equals(player.getMainHandItem()) || stack.equals(player.getOffhandItem())) && worldIn.getGameTime() % 20 == 0 ) {
                worldIn.playSound(null, player.getOnPos(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 1F, 1F);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable("wishing_fountain.misc.map_description"));
        if(stack.getOrCreateTag().getBoolean(IS_SEARCHING)) {
            list.add(Component.translatable("wishing_fountain.misc.is_searching"));
        } else if(getTarget(stack) != null) {
            if(Screen.hasShiftDown()) {
                list.add(Component.translatable(getType() + "." + getTarget(stack).toString().replace(":", ".")));
            } else {
                list.add(Component.translatable("wishing_fountain.misc.shift_up"));
            }
        }
    }

    protected static BlockPos nextPos(ItemStack stack, int step) {
        int sourceX = (int) stack.getOrCreateTag().getDouble(SOURCE_X);
        int sourceZ = (int) stack.getOrCreateTag().getDouble(SOURCE_Z);
        int x = stack.getOrCreateTag().getInt(POS_X);
        int z = stack.getOrCreateTag().getInt(POS_Z);
        int leg = stack.getOrCreateTag().getInt(POS_LEG);
        int legIndex = stack.getOrCreateTag().getInt(POS_LEG_INDEX);
        BlockPos cursor = new BlockPos(x, 0, z).relative(DIRECTIONS[(leg + 4) % 4]);
        int newX = cursor.getX();
        int newZ = cursor.getZ();
        int legSize = leg / 2 + 1;
        int maxLegs = 4 * Math.floorDiv(CommonConfigs.SEARCHING_RADIUS.get(), step);
        if(legIndex >= legSize) {
            if(leg > maxLegs)
                return null;
            leg++;
            legIndex = 0;
        }
        legIndex++;
        stack.getOrCreateTag().putInt(POS_X, newX);
        stack.getOrCreateTag().putInt(POS_Z, newZ);
        stack.getOrCreateTag().putInt(POS_LEG, leg);
        stack.getOrCreateTag().putInt(POS_LEG_INDEX, legIndex);
        int retX = sourceX + newX * step;
        int retZ = sourceZ + newZ * step;
        return new BlockPos(retX, 0, retZ);
    }

    private ItemStack getSearchingMap(Player player) {
        for(ItemStack stack : player.getInventory().items) {
            if(stack.getItem() instanceof WFMapItem) {
                boolean searching = stack.getOrCreateTag().getBoolean(IS_SEARCHING);
                if(searching)
                    return stack;
            }
        }
        return null;
    }

    public ItemStack createMap(ServerLevel level, BlockPos targetPos, ResourceLocation target) {
        ItemStack stack = MapItem.create(level, targetPos.getX(), targetPos.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, stack);
        MapItemSavedData.addTargetDecoration(stack, targetPos, "+", MapDecoration.Type.RED_X);
        // 这里不能用getTarget而是用参数传入，是因为生成的地图并没有标签
        stack.setHoverName(Component.translatable("wishing_fountain.misc.map", Component.translatable(getType() + "." + target.toString().replace(":", "."))));
        stack.getOrCreateTag().putBoolean(IS_SEARCHING, true);
        return stack;
    }

    protected abstract ItemStack search(ItemStack stack, ServerLevel worldIn, Player player, int slot);

    private void playSound(Level wordIn, Player player) {
        wordIn.playSound(null, player.getOnPos(), SoundEvents.PLAYER_SPLASH, SoundSource.AMBIENT, 1F, 1F);
    }

    protected ResourceLocation getTarget(ItemStack stack) {
        String tag = stack.getOrCreateTag().getString(TARGET);
        if (tag.isEmpty()) {
            return getDefaultTarget();
        }
        return ResourceLocation.tryParse(tag);
    }

    protected abstract ResourceLocation getDefaultTarget();

    protected String getType() {
        return type;
    }
}
