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
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.ItemLike;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = MODID, bus = Bus.MOD)
public class DataCreator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var gen = event.getGenerator();
        var out = gen.getPackOutput();
        var helper = event.getExistingFileHelper();

        gen.addProvider(event.includeServer(), new Recipes(out));
        gen.addProvider(event.includeServer(), new Loots(out));

        gen.addProvider(event.includeClient(), new Language(out));
        gen.addProvider(event.includeClient(), new BlockStates(out, helper));
        gen.addProvider(event.includeClient(), new ItemModels(out, helper));
    }

    private static class Recipes extends RecipeProvider {
        public Recipes(PackOutput out) {
            super(out);
        }

        @Override
        protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
            getTier(TIER1_BLOCK.get(), ItemTags.LOGS).save(consumer);
            getTier(TIER2_BLOCK.get(), Tags.Items.COBBLESTONE).save(consumer);
            getTier(TIER3_BLOCK.get(), Tags.Items.INGOTS_IRON).save(consumer);
            getTier(TIER4_BLOCK.get(), Tags.Items.INGOTS_GOLD).save(consumer);
            getTier(TIER5_BLOCK.get(), Tags.Items.GEMS_DIAMOND).save(consumer);
        }

        private ShapedRecipeBuilder getTier(ItemLike item, TagKey<Item> resource) {
            return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item)
                .define('W', Items.WATER_BUCKET)
                .define('L', Items.LAVA_BUCKET)
                .define('G', Blocks.GLASS)
                .define('R', resource)
                .pattern("RRR")
                .pattern("WGL")
                .pattern("RRR")
                .unlockedBy("has_lava", has(Items.LAVA_BUCKET))
                .unlockedBy("has_water", has(Items.WATER_BUCKET));
        }
    }

    private static class Loots extends LootTableProvider {
        public Loots(PackOutput out) {
            super(out, Set.of(), List.of(new SubProviderEntry(Blocks::new, LootContextParamSets.BLOCK)));
        }

        @Override
        protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext validationResults) {
           map.forEach((name, table) -> LootTables.validate(validationResults, name, table));
        }

        private static class Blocks extends BlockLootSubProvider {
            protected Blocks() {
                super(Set.of(), FeatureFlags.REGISTRY.allFlags());
            }

            @Override
            protected void generate() {
                this.dropSelf(TIER1_BLOCK.get());
                this.dropSelf(TIER2_BLOCK.get());
                this.dropSelf(TIER3_BLOCK.get());
                this.dropSelf(TIER4_BLOCK.get());
                this.dropSelf(TIER5_BLOCK.get());
            }

            @Override
            protected Iterable<Block> getKnownBlocks() {
                return BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
            }
        }
    }

    private static class Language extends LanguageProvider {
        public Language(PackOutput out) {
            super(out, MODID, "en_us");
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
        public ItemModels(PackOutput out, ExistingFileHelper helper) {
            super(out, MODID, helper);
        }

        @Override
        protected void registerModels() {
            makeTier(TIER1_BLOCK);
            makeTier(TIER2_BLOCK);
            makeTier(TIER3_BLOCK);
            makeTier(TIER4_BLOCK);
            makeTier(TIER5_BLOCK);
        }

        private void makeTier(RegistryObject<Block> block) {
            String path = block.getKey().location().getPath();
            getBuilder(path)
            .parent(new ModelFile.UncheckedModelFile(modLoc("block/" + path))); //TODO: Ask tterrag about this...
        }

        @Override
        public String getName() {
            return "Item Models";
        }
    }

    private static class BlockStates extends BlockStateProvider {

        public BlockStates(PackOutput out, ExistingFileHelper helper) {
            super(out, MODID, helper);
        }

        @Override
        protected void registerStatesAndModels() {
            makeTier(TIER1_BLOCK, mcLoc("block/acacia_log"));
            makeTier(TIER2_BLOCK, mcLoc("block/cobblestone"));
            makeTier(TIER3_BLOCK, mcLoc("block/iron_block"));
            makeTier(TIER4_BLOCK, mcLoc("block/gold_block"));
            makeTier(TIER5_BLOCK, mcLoc("block/diamond_block"));
        }

        private void makeTier(RegistryObject<Block> block, ResourceLocation texture) {
            ModelFile model = models().getBuilder(block.getKey().location().getPath())
                .parent(models().getExistingFile(modLoc("block/generator")))
                .texture("material", texture);
            getVariantBuilder(block.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
        }
    }
}
