package io.github.poisonsheep.wishingfountain;

import io.github.poisonsheep.wishingfountain.block.multiblock.MultiBlockManager;
import io.github.poisonsheep.wishingfountain.command.WishingFountainCommand;
import io.github.poisonsheep.wishingfountain.config.CommonConfigs;
import io.github.poisonsheep.wishingfountain.event.ForgeEvent;
import io.github.poisonsheep.wishingfountain.loot.ModGlobalLootModifiers;
import io.github.poisonsheep.wishingfountain.registry.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(WishingFountain.MODID)
public class WishingFountain {
    public static final IEventBus modBusEvent = FMLJavaModLoadingContext.get().getModEventBus();

    public final static String MODID = "wishing_fountain";

    public WishingFountain(){
        MinecraftForge.EVENT_BUS.register(new ForgeEvent());
        modBusEvent.addListener(this::setup);
        modBusEvent.addListener(this::enqueueIMC);
        modBusEvent.addListener(this::processIMC);
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfigs.SPEC,  MODID + ".toml");
        ItemRegistry.ITEMS.register(modBusEvent);
        BlockRegistry.BLOCKS.register(modBusEvent);
        BlockEntityRegistry.BLOCK_ENTITY.register(modBusEvent);
        SoundRegistry.SOUNDS.register(modBusEvent);
        RecipeRegistry.DEF_REG.register(modBusEvent);
        TabRegistry.TABS.register(modBusEvent);
        ModGlobalLootModifiers.GLM.register(modBusEvent);
    }
    private void setup(final FMLCommonSetupEvent event) {
        AdvancementTriggerRegistry.register();
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {}

    private void processIMC(final InterModProcessEvent event) {}

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MultiBlockManager.init();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        WishingFountainCommand.register(event.getDispatcher());
    }
}
