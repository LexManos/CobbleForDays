/*
 * Copyright (c) LexManos
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.lex.cfd;

import static net.minecraftforge.lex.cfd.CobbleForDays.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
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
        var regs = event.getLookupProvider();
        var helper = event.getExistingFileHelper();

        gen.addProvider(event.includeServer(), new Recipes.Runner(out, regs));
        gen.addProvider(event.includeServer(), new Loots(out, regs));
        gen.addProvider(event.includeServer(), new TagsProvider(out, regs, helper));

        gen.addProvider(event.includeClient(), new Language(out));
        gen.addProvider(event.includeClient(), new BlockStates(out, helper));
        gen.addProvider(event.includeClient(), new ItemModels(out, helper));
    }

    private static RegistryObject<? extends Block> block(int tier) {
        return CobbleForDays.getTier(tier).block();
    }

    private static class Recipes extends RecipeProvider {
        public Recipes(HolderLookup.Provider registries, RecipeOutput out) {
            super(registries, out);
        }

        @Override
        protected void buildRecipes() {
            getTier(block(1), ItemTags.LOGS).save(output);
            getTier(block(2), Tags.Items.COBBLESTONES).save(output);
            getTier(block(3), Tags.Items.INGOTS_IRON).save(output);
            getTier(block(4), Tags.Items.INGOTS_GOLD).save(output);
            getTier(block(5), Tags.Items.GEMS_DIAMOND).save(output);
        }

        private ShapedRecipeBuilder getTier(RegistryObject<? extends Block> block, TagKey<Item> resource) {
            return shaped(RecipeCategory.MISC, block.get())
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

        private static class Runner extends RecipeProvider.Runner {
            protected Runner(PackOutput output, CompletableFuture<Provider> registries) {
                super(output, registries);
            }

            @Override
            public String getName() {
                return "CobbleForDays Recipes";
            }

            @Override
            protected RecipeProvider createRecipeProvider(Provider registries, RecipeOutput output) {
                return new Recipes(registries, output);
            }

        }
    }

    private static class Loots extends LootTableProvider {
        public Loots(PackOutput out, CompletableFuture<HolderLookup.Provider> registries) {
            super(out, Set.of(), List.of(new SubProviderEntry(Blocks::new, LootContextParamSets.BLOCK)), registries);
        }

        @Override
        protected void validate(net.minecraft.core.Registry<LootTable> map, ValidationContext ctxt, ProblemReporter report) {
           map.listElements().forEach(table -> table.value().validate(
               ctxt.setContextKeySet(table.value().getParamSet())
                   .enterElement("{" + table.key().location() + "}", table.key())
           ));
        }

        private static class Blocks extends BlockLootSubProvider {
            protected Blocks(HolderLookup.Provider registries) {
                super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
            }

            @Override
            protected void generate() {
                for (int x = 1; x <= TIER_COUNT; x++)
                    this.dropSelf(block(x).get());
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
            for (int x = 1; x <= TIER_COUNT; x++) {
                var tier = getTier(x);
                add(tier.block().get(), "Cobble Gen Tier" + x);
                add(tier.item().get(), "Cobble Gen Tier" + x);
            }
        }
    }

    private static class ItemModels extends ItemModelProvider {
        public ItemModels(PackOutput out, ExistingFileHelper helper) {
            super(out, MODID, helper);
        }

        @Override
        protected void registerModels() {
            for (int x = 1; x <= TIER_COUNT; x++)
                makeTier(block(x));
        }

        private void makeTier(RegistryObject<? extends Block> block) {
            String path = block.getKey().location().getPath();
            getBuilder(path)
                .parent(new ModelFile.UncheckedModelFile(modLoc("block/" + path)));
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
            makeTier(block(1), mcLoc("block/acacia_log"));
            makeTier(block(2), mcLoc("block/cobblestone"));
            makeTier(block(3), mcLoc("block/iron_block"));
            makeTier(block(4), mcLoc("block/gold_block"));
            makeTier(block(5), mcLoc("block/diamond_block"));
        }

        private void makeTier(RegistryObject<? extends Block> block, ResourceLocation texture) {
            ModelFile model = models().getBuilder(block.getKey().location().getPath())
                .parent(models().getExistingFile(modLoc("block/generator")))
                .texture("material", texture);
            getVariantBuilder(block.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
        }
    }

    private static class TagsProvider extends BlockTagsProvider {
        public TagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
            super(output, lookupProvider, MODID, existingFileHelper);
        }

        @Override
        protected void addTags(Provider provider) {
            var mineable = this.tag(BlockTags.MINEABLE_WITH_PICKAXE);
            for (int x = 1; x <= TIER_COUNT; x++)
                mineable.add(block(x).get());
        }
    }
}
