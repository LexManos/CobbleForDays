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

import static net.minecraftforge.lex.cfd.CobbleForDays.*;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.LootTableProvider;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.data.loot.BlockLootTables;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootParameterSet;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableManager;
import net.minecraft.loot.ValidationTracker;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@EventBusSubscriber(modid = MODID, bus = Bus.MOD)
public class DataCreator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        ExistingFileHelper helper = event.getExistingFileHelper();

        if (event.includeServer()) {
            gen.addProvider(new Recipes(gen));
            gen.addProvider(new Loots(gen));
        }
        if (event.includeClient()) {
            gen.addProvider(new Language(gen));
            gen.addProvider(new BlockStates(gen, helper));
            gen.addProvider(new ItemModels(gen, helper));
        }
    }

    private static class Recipes extends RecipeProvider {
        public Recipes(DataGenerator gen) {
            super(gen);
        }

        @Override
        protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
            getTier(TIER1_BLOCK.get(), ItemTags.LOGS).build(consumer);
            getTier(TIER2_BLOCK.get(), Tags.Items.COBBLESTONE).build(consumer);
            getTier(TIER3_BLOCK.get(), Tags.Items.INGOTS_IRON).build(consumer);
            getTier(TIER4_BLOCK.get(), Tags.Items.INGOTS_GOLD).build(consumer);
            getTier(TIER5_BLOCK.get(), Tags.Items.GEMS_DIAMOND).build(consumer);
        }

        private ShapedRecipeBuilder getTier(IItemProvider item, ITag<Item> resource) {
            return ShapedRecipeBuilder.shapedRecipe(item)
                .key('W', Items.WATER_BUCKET)
                .key('L', Items.LAVA_BUCKET)
                .key('G', Blocks.GLASS)
                .key('R', resource)
                .patternLine("RRR")
                .patternLine("WGL")
                .patternLine("RRR")
                .addCriterion("has_lava", hasItem(Items.LAVA_BUCKET))
                .addCriterion("has_water", hasItem(Items.WATER_BUCKET));
        }
    }

    private static class Loots extends LootTableProvider {
        public Loots(DataGenerator gen) {
            super(gen);
        }

        @Override
        protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables() {
            return ImmutableList.of(
                Pair.of(Blocks::new, LootParameterSets.BLOCK)
            );
        }

        @Override
        protected void validate(Map<ResourceLocation, LootTable> map, ValidationTracker validationresults) {
           map.forEach((name, table) -> LootTableManager.func_227508_a_(validationresults, name, table));
        }

        private class Blocks extends BlockLootTables {
            @Override
            protected void addTables() {
                this.registerDropSelfLootTable(TIER1_BLOCK.get());
                this.registerDropSelfLootTable(TIER2_BLOCK.get());
                this.registerDropSelfLootTable(TIER3_BLOCK.get());
                this.registerDropSelfLootTable(TIER4_BLOCK.get());
                this.registerDropSelfLootTable(TIER5_BLOCK.get());
            }

            @Override
            protected Iterable<Block> getKnownBlocks() {
                return (Iterable<Block>)BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
            }
        }
    }

    private static class Language extends LanguageProvider {
        public Language(DataGenerator gen) {
            super(gen, MODID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add(TIER1_BLOCK.get(), "Cobble Gen Tier1");
            add(TIER2_BLOCK.get(), "Cobble Gen Tier2");
            add(TIER3_BLOCK.get(), "Cobble Gen Tier3");
            add(TIER4_BLOCK.get(), "Cobble Gen Tier4");
            add(TIER5_BLOCK.get(), "Cobble Gen Tier5");
        }
    }

    private static class ItemModels extends ItemModelProvider {
        public ItemModels(DataGenerator gen, ExistingFileHelper helper) {
            super(gen, MODID, helper);
        }

        @Override
        protected void registerModels() {
            makeTier(TIER1_BLOCK.get());
            makeTier(TIER2_BLOCK.get());
            makeTier(TIER3_BLOCK.get());
            makeTier(TIER4_BLOCK.get());
            makeTier(TIER5_BLOCK.get());
        }

        private void makeTier(Block block) {
            String path = block.getRegistryName().getPath();
            getBuilder(path)
            .parent(new ModelFile.UncheckedModelFile(modLoc("block/" + path))); //TODO: Ask tterrag about this...
        }

        @Override
        public String getName() {
            return "Item Models";
        }
    }

    private static class BlockStates extends BlockStateProvider {

        public BlockStates(DataGenerator gen, ExistingFileHelper helper) {
            super(gen, MODID, helper);
        }

        @Override
        protected void registerStatesAndModels() {
            makeTier(TIER1_BLOCK.get(), mcLoc("block/acacia_log"));
            makeTier(TIER2_BLOCK.get(), mcLoc("block/cobblestone"));
            makeTier(TIER3_BLOCK.get(), mcLoc("block/iron_block"));
            makeTier(TIER4_BLOCK.get(), mcLoc("block/gold_block"));
            makeTier(TIER5_BLOCK.get(), mcLoc("block/diamond_block"));
        }

        private void makeTier(Block block, ResourceLocation texture) {
            ModelFile model = models().getBuilder(block.getRegistryName().getPath())
                .parent(models().getExistingFile(modLoc("block/generator")))
                .texture("material", texture);
            getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
        }
    }
}
