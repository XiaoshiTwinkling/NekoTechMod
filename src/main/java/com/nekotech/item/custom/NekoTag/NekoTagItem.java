package com.nekotech.item.custom.NekoTag;

import com.nekotech.screen.NekoTag.NekoTagScreenHandler;
import com.nekotech.item.ModItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import com.nekotech.data.worlddata.NekoTagWorldState;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class NekoTagItem extends ModItem {

    public NekoTagItem(Settings settings, String id) {
        super(settings, id);

    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        /*
         * 非 shift，打开配置 GUI。
         * shift，交给 useOnBlock 处理。
         */
        if (user.isSneaking()) {
            return TypedActionResult.pass(stack);
        }

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, player) ->
                            new NekoTagScreenHandler(syncId, inventory, hand),
                    Text.translatable("screen.neko-technology.neko_tag")
            ));
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();

        if (player == null) {
            return ActionResult.PASS;
        }

        if (!player.isSneaking()) {
            return ActionResult.PASS;
        }

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }

        BlockPos pos = context.getBlockPos();

        if (!hasInventoryLikeStorage(serverWorld, pos, context)) {
            return ActionResult.PASS;
        }

        ItemStack stack = context.getStack();
        NekoPlacedTag placedTag = NekoPlacedTag.fromStack(stack);

        NekoTagWorldState state = NekoTagWorldState.get(serverWorld.getServer());
        NekoTagWorldState.ToggleResult result = state.toggle(serverWorld, pos, placedTag);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (result == NekoTagWorldState.ToggleResult.ADDED) {
                serverPlayer.sendMessage(
                        Text.translatable("message.neko-technology.neko_tag.added"),
                        true
                );
            } else {
                serverPlayer.sendMessage(
                        Text.translatable("message.neko-technology.neko_tag.removed"),
                        true
                );
            }
        }

        return ActionResult.SUCCESS;
    }

    private static boolean hasInventoryLikeStorage(
            ServerWorld world,
            BlockPos pos,
            ItemUsageContext context
    ) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof Inventory) {
            return true;
        }

        Storage<ItemVariant> storage = ItemStorage.SIDED.find(
                world,
                pos,
                context.getSide()
        );

        return storage != null;
    }


}