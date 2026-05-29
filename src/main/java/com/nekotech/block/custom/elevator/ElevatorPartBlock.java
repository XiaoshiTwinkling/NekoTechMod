package com.nekotech.block.custom.elevator;

import com.nekotech.block.entity.ElevatorCoreBlockEntity;
import com.nekotech.block.custom.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ElevatorPartBlock extends Block {
    public static final EnumProperty<ElevatorPartSection> SECTION =
            EnumProperty.of("section", ElevatorPartSection.class);

    public ElevatorPartBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(SECTION, ElevatorPartSection.MIDDLE));
    }

    public static BlockState getStateForFloor(int floor, int height) {
        /*
         * floor 从 0 开始。
         * floor == 0 是 core，不会用到这里。
         * floor == height - 1 是最顶端 part。
         * 其他 part 都是 middle。
         */
        ElevatorPartSection section = floor == height - 1
                ? ElevatorPartSection.TOP
                : ElevatorPartSection.MIDDLE;

        return ModBlocks.ELEVATOR_PART_BLOCK.getDefaultState().with(SECTION, section);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SECTION);
    }

    @Nullable
    public static ElevatorCoreBlockEntity findCoreBelow(World world, BlockPos from) {
        for (int dy = 0; dy <= ElevatorCoreBlockEntity.MAX_VERTICAL_DISTANCE; dy++) {
            BlockPos check = from.down(dy);

            if (world.getBlockState(check).isOf(ModBlocks.ELEVATOR_CORE_BLOCK)
                    && world.getBlockEntity(check) instanceof ElevatorCoreBlockEntity be) {
                return be;
            }
        }

        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        ElevatorCoreBlockEntity be = findCoreBelow(world, pos);

        if (be == null) {
            return ActionResult.PASS;
        }

        int clickedFloor = pos.getY() - be.getPos().getY();
        be.handleFloorClick(player, clickedFloor);

        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (!world.isClient) {
                ElevatorCoreBlockEntity be = findCoreBelow(world, pos);

                if (be != null && !be.isDestroying()) {
                    be.destroyWholeStructure(true, true);
                }
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
