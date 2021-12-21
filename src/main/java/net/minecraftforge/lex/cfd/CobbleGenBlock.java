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

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class CobbleGenBlock extends Block implements EntityBlock {
	private static final VoxelShape RENDER_SHAPE = Shapes.join(box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D), BooleanOp.OR);
	private final int tier;
	
	public CobbleGenBlock(int tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}
	
	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean p_220069_6_) {
		if(pos.above().equals(fromPos)) ((CobbleGenTile) level.getBlockEntity(pos)).updateCache();
	}
	
	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return RENDER_SHAPE;
	}
	
	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return CobbleGenTile.create(this.tier, blockPos, blockState);
	}
	
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
		return blockEntityType == CobbleForDays.TIER1_TILE.get() || blockEntityType == CobbleForDays.TIER2_TILE.get() || blockEntityType == CobbleForDays.TIER3_TILE.get() ||
		       blockEntityType == CobbleForDays.TIER4_TILE.get() || blockEntityType == CobbleForDays.TIER5_TILE.get() ? (BlockEntityTicker<T>) new CobbleGenTile.Ticker() : null;
	}
}
