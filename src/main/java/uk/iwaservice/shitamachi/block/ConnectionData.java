package uk.iwaservice.shitamachi.block;

import net.minecraft.core.BlockPos;

public record ConnectionData(BlockPos pos, float thickness, float sag) {
    public static final float DEFAULT_THICKNESS = 0.025f;
    public static final float DEFAULT_SAG = 0.05f;
}
