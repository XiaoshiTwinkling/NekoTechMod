package com.nekotech.worldgen.feature;

import com.mojang.serialization.Codec;
import com.nekotech.item.block.ModBlocks;
import com.nekotech.item.block.crops.PetgrassCropBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PetgrassRiverSideFeature extends Feature<DefaultFeatureConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger("nekotech-petgrass-worldgen");

    public PetgrassRiverSideFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();

        return tryPlace(world, origin, context);
    }
    private boolean tryPlace(
            StructureWorldAccess world,
            BlockPos pos,
            FeatureContext<DefaultFeatureConfig> context
    ) {
        BlockState currentState = world.getBlockState(pos);

        if (!currentState.isAir() && !currentState.isReplaceable()) {
            return false;
        }

        BlockPos groundPos = pos.down();
        BlockState groundState = world.getBlockState(groundPos);

        if (!groundState.isIn(BlockTags.DIRT) && !groundState.isOf(Blocks.FARMLAND)) {
            return false;
        }

        if (!hasWaterWithinRange(world, groundPos)) {
            return false;
        }

        int age = context.getRandom().nextInt(PetgrassCropBlock.MAX_AGE + 1);

        BlockState petgrassState = ModBlocks.PETGRASS_CROP
                .getDefaultState()
                .with(PetgrassCropBlock.AGE, age);

        if (!petgrassState.canPlaceAt(world, pos)) {
            return false;
        }

        world.setBlockState(pos, petgrassState, Block.NOTIFY_LISTENERS);

        LOGGER.info(
                "Generated petgrass at x={}, y={}, z={}, age={}",
                pos.getX(), pos.getY(), pos.getZ(), age
        );

        return true;
    }

    private static boolean hasWaterWithinRange(
            StructureWorldAccess world,
            BlockPos groundPos
    ) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int horizontalDistance = Math.max(Math.abs(dx), Math.abs(dz));

                if (horizontalDistance < 1) {
                    continue;
                }

                // 检查同高度、低一格、高一格的水，适配河岸高低差
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos checkPos = groundPos.add(dx, dy, dz);

                    if (world.getFluidState(checkPos).isIn(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
