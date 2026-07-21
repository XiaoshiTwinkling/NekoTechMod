package com.nekotech.item.custom.camera;

import com.nekotech.data.worlddata.CatCameraChannelWorldState;
import com.nekotech.catcamera.CatCameraChannelAccess;
import com.nekotech.catcamera.CatCameraChannelService;
import com.nekotech.catcamera.CatCameraViewManager;
import com.nekotech.network.payload.s2c.OpenCatCameraListPayload;
import com.nekotech.network.payload.s2c.OpenCatCameraNamePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class CatCameraTerminalItem extends Item {
    public CatCameraTerminalItem(Settings settings) { super(settings.maxCount(1)); }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof CatEntity cat)) return ActionResult.PASS;
        if (user.getWorld().isClient()) return ActionResult.SUCCESS;
        if (!(user instanceof ServerPlayerEntity player)) return ActionResult.FAIL;
        if (!cat.isTamed() || !cat.isOwner(player)) {
            player.sendMessage(Text.translatable("message.neko-technology.cat_camera.not_owner"), true);
            return ActionResult.FAIL;
        }

        CatCameraChannelAccess access = (CatCameraChannelAccess) cat;
        if (access.neko_technology$isCatCameraChannelActive()) {
            CatCameraViewManager.exitWatchers(player.getServer(), cat.getUuid());
            CatCameraChannelService.delete(cat);
            player.sendMessage(Text.translatable("message.neko-technology.cat_camera.removed"), true);
        } else {
            ServerPlayNetworking.send(player, new OpenCatCameraNamePayload(cat.getUuid()));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            List<OpenCatCameraListPayload.Entry> channels = CatCameraChannelWorldState.get(player.getServer())
                    .getActiveChannels().stream()
                    .map(data -> new OpenCatCameraListPayload.Entry(data.indexedCatUuid(), data.name(), data.dimension()))
                    .toList();
            ServerPlayNetworking.send(player, new OpenCatCameraListPayload(channels));
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}
