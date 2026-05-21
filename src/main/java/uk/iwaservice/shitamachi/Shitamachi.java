package uk.iwaservice.shitamachi;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import uk.iwaservice.shitamachi.block.CrossArmBlock;
import uk.iwaservice.shitamachi.block.DenpoleBlock;
import uk.iwaservice.shitamachi.block.DenpoleBlockEntity;
import uk.iwaservice.shitamachi.block.DenpoleBlockEntityRenderer;
import uk.iwaservice.shitamachi.item.CableItem;
import uk.iwaservice.shitamachi.item.WarningCoverItem;

@Mod(Shitamachi.MODID)
public class Shitamachi {
    public static final String MODID = "shitamachi";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredBlock<DenpoleBlock> DENPOLE = BLOCKS.register("denpole",
            () -> new DenpoleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> DENPOLE_ITEM = ITEMS.registerSimpleBlockItem("denpole", DENPOLE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DenpoleBlockEntity>> DENPOLE_BE =
            BLOCK_ENTITY_TYPES.register("denpole", () ->
                    BlockEntityType.Builder.of(DenpoleBlockEntity::new, DENPOLE.get()).build(null));

    public static final DeferredBlock<CrossArmBlock> CROSS_ARM = BLOCKS.register("cross_arm",
            () -> new CrossArmBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> CROSS_ARM_ITEM = ITEMS.registerSimpleBlockItem("cross_arm", CROSS_ARM);

    public static final DeferredItem<WarningCoverItem> WARNING_COVER_ITEM = ITEMS.register("warning_cover",
            () -> new WarningCoverItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<CableItem> CABLE_ITEM = ITEMS.register("cable",
            () -> new CableItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.shitamachi"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> DENPOLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(DENPOLE_ITEM.get());
                output.accept(CROSS_ARM_ITEM.get());
                output.accept(WARNING_COVER_ITEM.get());
                output.accept(CABLE_ITEM.get());
            })
            .build());

    public Shitamachi(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
            event.accept(DENPOLE_ITEM);
            event.accept(CABLE_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
            BlockEntityRenderers.register(Shitamachi.DENPOLE_BE.get(), DenpoleBlockEntityRenderer::new);
        }
    }
}
