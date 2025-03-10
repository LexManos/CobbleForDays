/*
 * Copyright (c) LexManos
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.lex.cfd;

import static net.minecraftforge.lex.cfd.CobbleForDays.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
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
        gen.addProvider(event.includeClient(), new Models(out));
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

    private static class Models extends ModelProvider {
        public Models(PackOutput out) {
            super(out);
        }

        @Override
        protected Stream<Block> getKnownBlocks() {
            return BLOCKS.getEntries().stream().map(RegistryObject::get);
        }

        @Override
        protected Stream<Item> getKnownItems() {
            return ITEMS.getEntries().stream().map(RegistryObject::get);
        }

        @Override
        protected BlockModelGenerators getBlockModelGenerators(BlockStateGeneratorCollector blocks, ItemInfoCollector items, SimpleModelCollector models) {
            var SLOT = TextureSlot.create("material");
            var TEMPLATE = new ModelTemplate(Optional.of(modLoc("block/generator")), Optional.empty(), SLOT);

            return new BlockModelGenerators(blocks, items, models) {
                @Override
                public void run() {
                    makeTier(getTier(1), mcLoc("block/acacia_log"));
                    makeTier(getTier(2), mcLoc("block/cobblestone"));
                    makeTier(getTier(3), mcLoc("block/iron_block"));
                    makeTier(getTier(4), mcLoc("block/gold_block"));
                    makeTier(getTier(5), mcLoc("block/diamond_block"));
                }

                private void makeTier(Tier tier, ResourceLocation texture) {
                    var baked = TEMPLATE.create(tier.block().get(), TextureMapping.singleSlot(SLOT, texture), this.modelOutput::accept);
                    this.blockStateOutput.accept(createSimpleBlock(tier.block().get(), baked));
                    this.itemModelOutput.accept(tier.item().get(), ItemModelUtils.tintedModel(baked, WaterItemTint.INSTANCE));
                }
            };
        }

        @Override
        protected ItemModelGenerators getItemModelGenerators(ItemInfoCollector items, SimpleModelCollector models) {
            return new ItemModelGenerators(items, models) {
                @Override
                public void run() {
                }
            };
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

    private static ResourceLocation modLoc(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name);
    }

    private static ResourceLocation mcLoc(String name) {
        return ResourceLocation.parse(name);
    }
}
