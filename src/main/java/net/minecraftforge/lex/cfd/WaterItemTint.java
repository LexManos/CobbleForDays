/*
 * Copyright (c) LexManos
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.lex.cfd;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record WaterItemTint() implements ItemTintSource {
    public static final WaterItemTint INSTANCE = new WaterItemTint();
    public static final MapCodec<WaterItemTint> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
        return BiomeColors.getAverageWaterColor(level, entity.blockPosition());
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return CODEC;
    }
}
