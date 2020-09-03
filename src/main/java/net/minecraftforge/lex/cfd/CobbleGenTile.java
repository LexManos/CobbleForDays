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

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.lex.cfd.Config.Server.Tier;

import static net.minecraftforge.lex.cfd.CobbleForDays.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CobbleGenTile extends TileEntity implements ITickableTileEntity {
    private final ConfigCache config;
    private final LazyOptional<IItemHandler> inventory = LazyOptional.of(Inventory::new);
    private LazyOptional<IItemHandler> cache = null;
    private int count = 0;
    private int timer = 20;
    private int configTimer = 200;

    public CobbleGenTile(Tier tier, TileEntityType<?> tileType) {
        super(tileType);
        this.config = new ConfigCache(tier);
        this.timer = tier.interval.get();
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
       if (!this.removed && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY )
          return inventory.cast();
       return super.getCapability(cap, side);
    }

    @Override
    public void remove() {
        inventory.invalidate();
        super.remove();
    }

    // @mcp: func_230337_a_ = read
    @Override
    public void func_230337_a_(BlockState state, CompoundNBT nbt) {
        super.func_230337_a_(state, nbt);

        count = nbt.getInt("count");
        timer = nbt.getInt("timer");
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        super.write(nbt);
        nbt.putInt("count", count);
        nbt.putInt("timer", timer);
        return nbt;
    }

    @Override
    public void tick() {
        if (getWorld() != null && getWorld().isRemote) return;
        if (timer-- <= 0) {
            count += config.count;
            this.timer = config.interval;

            if (count > config.max)
                count = config.max;
            if (count < 0)
                count = 0;

            markDirty();
        }

        if (config.pushes && count > 0 && getCache().isPresent()) {
            push();
        }

        if (configTimer-- <= 0) {
            config.update();
            configTimer = 200;
        }
    }

    public void updateCache() {
        TileEntity tileEntity = world != null ? world.getTileEntity(pos.up()) : null;
        if (tileEntity != null){
            LazyOptional<IItemHandler> lazyOptional = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN);
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
            markDirty();
        } else if (result.getCount() != count) {
            count = result.getCount();
            markDirty();
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
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (count == 0 || amount == 0)
                return ItemStack.EMPTY;
            int ret = Math.min(count, amount);
            if (!simulate) {
                count -= ret;
                CobbleGenTile.this.markDirty();
            }
            return new ItemStack(Items.COBBLESTONE, ret);
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    public static CobbleGenTile create(int tier) {
        switch (tier) {
            case 1: return new CobbleGenTile(Config.SERVER.tier1, TIER1_TILE.get());
            case 2: return new CobbleGenTile(Config.SERVER.tier2, TIER2_TILE.get());
            case 3: return new CobbleGenTile(Config.SERVER.tier3, TIER3_TILE.get());
            case 4: return new CobbleGenTile(Config.SERVER.tier4, TIER4_TILE.get());
            case 5: return new CobbleGenTile(Config.SERVER.tier5, TIER5_TILE.get());
            default: throw new IllegalArgumentException("Unknown Tier: " + tier);
        }
    }
}
