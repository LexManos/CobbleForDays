/*
 * Copyright (c) 2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.minecraftforge.lex.cfd;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.biome.Biomes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CobbleForDays.MODID)
public class CobbleForDays {
    public static final String MODID = "cobblefordays";
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MODID);

    private static final Block.Properties blockProps = Block.Properties.create(Material.GLASS).hardnessAndResistance(3.5F).func_235838_a_(state -> 15); // @mcp: func_235838_a_ = lightLevel
    private static final Item.Properties  itemProps  = new Item.Properties().group(ItemGroup.MISC);

    public static final RegistryObject<Block> TIER1_BLOCK = BLOCKS.register("tier_1", () -> new CobbleGenBlock(1, blockProps));
    public static final RegistryObject<Block> TIER2_BLOCK = BLOCKS.register("tier_2", () -> new CobbleGenBlock(2, blockProps));
    public static final RegistryObject<Block> TIER3_BLOCK = BLOCKS.register("tier_3", () -> new CobbleGenBlock(3, blockProps));
    public static final RegistryObject<Block> TIER4_BLOCK = BLOCKS.register("tier_4", () -> new CobbleGenBlock(4, blockProps));
    public static final RegistryObject<Block> TIER5_BLOCK = BLOCKS.register("tier_5", () -> new CobbleGenBlock(5, blockProps));
    public static final RegistryObject<Item>  TIER1_ITEM  = ITEMS .register("tier_1", () -> new BlockItem(TIER1_BLOCK.get(), itemProps));
    public static final RegistryObject<Item>  TIER2_ITEM  = ITEMS .register("tier_2", () -> new BlockItem(TIER2_BLOCK.get(), itemProps));
    public static final RegistryObject<Item>  TIER3_ITEM  = ITEMS .register("tier_3", () -> new BlockItem(TIER3_BLOCK.get(), itemProps));
    public static final RegistryObject<Item>  TIER4_ITEM  = ITEMS .register("tier_4", () -> new BlockItem(TIER4_BLOCK.get(), itemProps));
    public static final RegistryObject<Item>  TIER5_ITEM  = ITEMS .register("tier_5", () -> new BlockItem(TIER5_BLOCK.get(), itemProps));
    public static final RegistryObject<TileEntityType<CobbleGenTile>> TIER1_TILE = TILES.register("tier_1", () -> TileEntityType.Builder.create(() -> CobbleGenTile.create(1), TIER1_BLOCK.get()).build(null));
    public static final RegistryObject<TileEntityType<CobbleGenTile>> TIER2_TILE = TILES.register("tier_2", () -> TileEntityType.Builder.create(() -> CobbleGenTile.create(2), TIER2_BLOCK.get()).build(null));
    public static final RegistryObject<TileEntityType<CobbleGenTile>> TIER3_TILE = TILES.register("tier_3", () -> TileEntityType.Builder.create(() -> CobbleGenTile.create(3), TIER3_BLOCK.get()).build(null));
    public static final RegistryObject<TileEntityType<CobbleGenTile>> TIER4_TILE = TILES.register("tier_4", () -> TileEntityType.Builder.create(() -> CobbleGenTile.create(4), TIER4_BLOCK.get()).build(null));
    public static final RegistryObject<TileEntityType<CobbleGenTile>> TIER5_TILE = TILES.register("tier_5", () -> TileEntityType.Builder.create(() -> CobbleGenTile.create(5), TIER5_BLOCK.get()).build(null));

    public CobbleForDays() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TILES.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupClient);

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::colorGeneratorBlockWater);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::colorGeneratorItemWater);
        });

        MinecraftForge.EVENT_BUS.register(this);

        //ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ForgeConfig.clientSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.serverSpec);
    }

    private void setupClient(final FMLClientSetupEvent event) {
        RenderTypeLookup.setRenderLayer(TIER1_BLOCK.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(TIER2_BLOCK.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(TIER3_BLOCK.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(TIER4_BLOCK.get(), RenderType.getCutout());
        RenderTypeLookup.setRenderLayer(TIER5_BLOCK.get(), RenderType.getCutout());
    }

    private void setup(final FMLCommonSetupEvent event) {}

    public void colorGeneratorBlockWater(ColorHandlerEvent.Block event) {
        event.getBlockColors().register(
                (blockState, environmentBlockReader, pos, index) -> index == 1 ? environmentBlockReader != null && pos != null ? BiomeColors.getWaterColor(environmentBlockReader, pos) : Biomes.PLAINS.getWaterColor() : -1,
                BLOCKS.getEntries().stream().filter(RegistryObject::isPresent).map(RegistryObject::get).toArray(Block[]::new)
        );
    }

    public void colorGeneratorItemWater(ColorHandlerEvent.Item event) {
        event.getItemColors().register(
                (stack, index) -> index == 1 ? Biomes.PLAINS.getWaterColor() : -1,
                ITEMS.getEntries().stream().filter(RegistryObject::isPresent).map(RegistryObject::get).toArray(Item[]::new)
        );
    }
}
