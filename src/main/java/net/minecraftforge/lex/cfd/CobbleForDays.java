/*
 * Copyright (c) LexManos
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.lex.cfd;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CobbleForDays.MODID)
public class CobbleForDays {
    public static final String MODID = "cobblefordays";
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    private static final Block.Properties blockProps = Block.Properties.ofFullCopy(Blocks.GLASS).strength(3.5F).lightLevel(state -> 15);
    private static final Item.Properties  itemProps  = new Item.Properties();

    public record Tier(RegistryObject<? extends Block> block, RegistryObject<? extends Item> item, RegistryObject<BlockEntityType<CobbleGenTile>> tile) {};
    private static Tier createTier(int tier) {
        var block = BLOCKS.register("tier_" + tier, () -> new CobbleGenBlock(tier, blockProps.setId(BLOCKS.key("tier_" + tier))));
        var item = ITEMS.register("tier_" + tier, () -> new BlockItem(block.get(), itemProps.setId(ITEMS.key("tier_" + tier))));
        var tile = TILES.register("tier_" + tier, () ->
            new BlockEntityType<CobbleGenTile>(
                (pos, state) -> CobbleGenTile.create(tier, pos, state),
                Set.of(block.get())
            )
        );
        return new Tier(block, item, tile);
    }

    public static final int TIER_COUNT = 5;
    private static final Tier[] TIERS;
    static {
        TIERS = new Tier[TIER_COUNT];
        for (int x = 0; x < TIER_COUNT; x++) {
            TIERS[x] = createTier(x + 1);
        }
    }

    public static Tier getTier(int tier) {
        if (tier < 1 || tier > TIER_COUNT)
            throw new IllegalArgumentException("Invalid Tier: " + tier);
        return TIERS[tier - 1];
    }

    public static final int PLAINS = 4159204;

    public CobbleForDays(FMLJavaModLoadingContext context) {
        var modBus = context.getModEventBus();
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        TILES.register(modBus);
        modBus.addListener(this::setup);
        modBus.addListener(this::setupClient);
        modBus.addListener(this::addCreative);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(this::colorGeneratorBlockWater);
            ItemTintSources.ID_MAPPER.put(ResourceLocation.fromNamespaceAndPath(MODID, "water"), WaterItemTint.CODEC);
        });

        MinecraftForge.EVENT_BUS.register(this);

        //context.registerConfig(ModConfig.Type.CLIENT, ForgeConfig.clientSpec);
        context.registerConfig(ModConfig.Type.SERVER, Config.serverSpec);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.FUNCTIONAL_BLOCKS)
            return;

        for (var tier : TIERS)
            event.accept(tier.item());
    }

    @SuppressWarnings("removal") // 1.21.3 doesn't load the json render type, so set it using vanilla mechanics.
    private void setupClient(final FMLClientSetupEvent event) {
        for (int x = 1; x <= TIER_COUNT; x++)
            ItemBlockRenderTypes.setRenderLayer(getTier(x).block().get(), RenderType.cutout());
    }

    private void setup(final FMLCommonSetupEvent event) {}

    public void colorGeneratorBlockWater(RegisterColorHandlersEvent.Block event) {
        event.register(
            (state, env, pos, index) -> index == 0 ? env != null && pos != null ? BiomeColors.getAverageWaterColor(env, pos) : PLAINS : -1,
            BLOCKS.getEntries().stream().filter(RegistryObject::isPresent).map(RegistryObject::get).toArray(Block[]::new)
        );
    }
}
