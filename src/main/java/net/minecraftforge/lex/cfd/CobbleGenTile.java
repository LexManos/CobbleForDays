/*
 * Copyright (c) LexManos
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.lex.cfd;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.lex.cfd.Config.Server.Tier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CobbleGenTile extends BlockEntity {
    private final ConfigCache config;
    private final LazyOptional<IItemHandler> inventory = LazyOptional.of(Inventory::new);
    private LazyOptional<IItemHandler> cache = null;
    private int count = 0;
    private int timer = 20;
    private int configTimer = 200;

    public CobbleGenTile(Tier tier, BlockEntityType<?> tileType, BlockPos blockPos, BlockState blockState) {
        super(tileType, blockPos, blockState);
        this.config = new ConfigCache(tier);
        this.timer = tier.interval.get();
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
       if (!this.remove && cap == ForgeCapabilities.ITEM_HANDLER)
          return inventory.cast();
       return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        inventory.invalidate();
        super.setRemoved();
    }


    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider regs) {
        super.loadAdditional(nbt, regs);
        count = nbt.contains("count") ? nbt.getInt("count") : 0;
        timer = nbt.contains("timer") ? nbt.getInt("timer") : 0;
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider regs) {
        super.saveAdditional(nbt, regs);
        nbt.putInt("count", count);
        nbt.putInt("timer", timer);
    }

    public void updateCache() {
        BlockEntity tileEntity = level != null && level.isLoaded(worldPosition.above()) ? level.getBlockEntity(worldPosition.above()) : null;
        if (tileEntity != null){
            LazyOptional<IItemHandler> lazyOptional = tileEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN);
            if (lazyOptional.isPresent()) {
                if (this.cache != lazyOptional) {
                    this.cache = lazyOptional;
                    cache.addListener(l -> updateCache());
                }
            }
            else cache = LazyOptional.empty();
        }
        else cache = LazyOptional.empty();
    }

    private LazyOptional<IItemHandler> getCache() {
        if (cache == null)
            updateCache();
        return cache;
    }

    private void push() {
        ItemStack stack = new ItemStack(Items.COBBLESTONE, count);
        ItemStack result = getCache()
                .map(iItemHandler -> ItemHandlerHelper.insertItemStacked(iItemHandler, stack, false))
                .orElse(stack);

        if (result.isEmpty()) {
            count = 0;
            setChanged();
        } else if (result.getCount() != count) {
            count = result.getCount();
            setChanged();
        }
    }

    public static class Ticker implements BlockEntityTicker<CobbleGenTile> {
        @Override
        public void tick(Level level, BlockPos blockPos, BlockState blockState, CobbleGenTile cobbleGen) {
            if(level.isClientSide) return;
            if(--cobbleGen.timer <= 0) {
                cobbleGen.count += cobbleGen.config.count;
                cobbleGen.timer = cobbleGen.config.interval;

                if(cobbleGen.count > cobbleGen.config.max) cobbleGen.count = cobbleGen.config.max;
                if(cobbleGen.count < 0) cobbleGen.count = 0;

                cobbleGen.setChanged();
            }

            if(cobbleGen.config.pushes && cobbleGen.count > 0 && cobbleGen.getCache().isPresent()) {
                cobbleGen.push();
            }

            if(--cobbleGen.configTimer <= 0) {
                cobbleGen.config.update();
                cobbleGen.configTimer = 200;
            }

        }
    }

    private static class ConfigCache {
        private final Tier tier;
        private int interval;
        private int count;
        private int max;
        private boolean pushes;

        private ConfigCache(Tier tier) {
            this.tier = tier;
            update();
        }

        private void update() {
            this.interval = this.tier.interval.get();
            this.count = this.tier.count.get();
            this.max = this.tier.max.get();
            this.pushes = this.tier.pushes.get();
        }
    }

    private class Inventory implements IItemHandler {
        private final ItemStack stack = new ItemStack(Items.COBBLESTONE, 0);
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            stack.setCount(count);
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (count == 0 || amount == 0)
                return ItemStack.EMPTY;
            int ret = Math.min(count, amount);
            if (!simulate) {
                count -= ret;
                CobbleGenTile.this.setChanged();
            }
            return new ItemStack(Items.COBBLESTONE, ret);
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    public static CobbleGenTile create(int tier, BlockPos blockPos, BlockState blockState) {
        return new CobbleGenTile(Config.SERVER.getTier(tier), CobbleForDays.getTier(tier).tile().get(), blockPos, blockState);
    }
}
