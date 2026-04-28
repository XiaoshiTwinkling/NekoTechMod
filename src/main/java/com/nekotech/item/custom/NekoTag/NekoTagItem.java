package com.nekotech.item.custom.NekoTag;

import com.nekotech.screen.NekoTag.NekoTagScreenHandler;
import com.nekotech.item.ModItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class NekoTagItem extends ModItem {

    public NekoTagItem(Settings settings, String id) {
        super(settings, id);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, player) ->
                            new NekoTagScreenHandler(syncId, inventory, hand),
                    Text.translatable("screen.neko-technology.neko_tag")
            ));
        }

        return TypedActionResult.success(stack, world.isClient());
    }
}