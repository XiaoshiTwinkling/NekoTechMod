package com.nekotech.item.custom;

import com.nekotech.data.worlddata.NekoTagWorldState;
import com.nekotech.item.custom.NekoTag.NekoPlacedTag;
import com.nekotech.network.payload.s2c.OpenTagListPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class NekoTagReaderItem extends Item {

    public NekoTagReaderItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }

        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        BlockPos pos = context.getBlockPos();

        List<NekoPlacedTag> tags = NekoTagWorldState
                .get(serverWorld.getServer())
                .getTagsAt(serverWorld, pos);

        List<OpenTagListPayload.TagEntry> entries = tags.stream()
                .map(tag -> new OpenTagListPayload.TagEntry(
                        tag.color(),
                        tag.priority(),
                        tag.task(),
                        tag.displayStackId()
                ))
                .toList();

        ServerPlayNetworking.send(
                player,
                new OpenTagListPayload(pos, entries)
        );

        return ActionResult.SUCCESS;
    }
}