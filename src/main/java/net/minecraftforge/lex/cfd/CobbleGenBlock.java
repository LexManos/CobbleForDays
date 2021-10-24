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

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import net.minecraft.block.AbstractBlock.Properties;

public class CobbleGenBlock extends Block {
    private static final VoxelShape RENDER_SHAPE = VoxelShapes.join(
            box(0.0D,  0.0D, 0.0D, 16.0D,  4.0D, 16.0D),
            box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            IBooleanFunction.OR);
    private final int tier;
    public CobbleGenBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return CobbleGenTile.create(this.tier);
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean p_220069_6_) {
        if (pos.above().equals(fromPos))
            ((CobbleGenTile)world.getBlockEntity(pos)).updateCache();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, IBlockReader worldIn, BlockPos pos) {
       return RENDER_SHAPE;
    }
}
