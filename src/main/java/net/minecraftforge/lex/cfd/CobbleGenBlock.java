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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

public class CobbleGenBlock extends Block implements EntityBlock {
    private static final VoxelShape RENDER_SHAPE = Shapes.join(
            box(0.0D,  0.0D, 0.0D, 16.0D,  4.0D, 16.0D),
            box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            BooleanOp.OR);
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
        return blockEntityType == CobbleForDays.TIER1_TILE.get() || blockEntityType == CobbleForDays.TIER2_TILE.get() || blockEntityType == CobbleForDays.TIER3_TILE.get() ||
               blockEntityType == CobbleForDays.TIER4_TILE.get() || blockEntityType == CobbleForDays.TIER5_TILE.get() ? (BlockEntityTicker<T>) new CobbleGenTile.Ticker() : null;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean p_220069_6_) {
        if (pos.above().equals(fromPos))
            ((CobbleGenTile)level.getBlockEntity(pos)).updateCache();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter worldIn, BlockPos pos) {
       return RENDER_SHAPE;
    }
}
