/*
 * Copyright (c) LexManos
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.lex.cfd;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CobbleGenBlock extends Block implements EntityBlock {
    private static final VoxelShape RENDER_SHAPE = Shapes.or(
        box(0.0D,  0.0D, 0.0D, 16.0D,  4.0D, 16.0D),
        box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D)
    );
    private final int tier;
    public CobbleGenBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return CobbleGenTile.create(this.tier, blockPos, blockState);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        for (int i = 1; i <= CobbleForDays.TIER_COUNT; i++) {
            if (blockEntityType == CobbleForDays.getTier(i).tile().get())
                return (BlockEntityTicker<T>) new CobbleGenTile.Ticker();
        }
        return null;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean unknown) {
        ((CobbleGenTile)level.getBlockEntity(pos)).updateCache();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state) {
       return RENDER_SHAPE;
    }
}
