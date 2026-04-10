package com.nekotech.network;

import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HudNetworkHandler {
    public static void initialize() {
        // 注册网络包类型
        PayloadTypeRegistry.playC2S().register(
                HudNetworkPayloads.REQUEST_HUD_DATA,
                HudNetworkPayloads.RequestHudDataPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                HudNetworkPayloads.SEND_HUD_DATA,
                HudNetworkPayloads.SendHudDataPayload.CODEC
        );

        // 注册请求处理器
        ServerPlayNetworking.registerGlobalReceiver(
                HudNetworkPayloads.REQUEST_HUD_DATA,
                (payload, context) -> handleHudDataRequest(payload, context)
        );
    }

    private static void handleHudDataRequest(
            HudNetworkPayloads.RequestHudDataPayload payload,
            ServerPlayNetworking.Context context
    ) {
        BlockPos pos = payload.pos();

        // 在主线程处理
        context.server().execute(() -> {
            ServerPlayerEntity player = context.player();

            // 获取方块实体
            var blockEntity = player.getWorld().getBlockEntity(pos);
            if (blockEntity instanceof IHaveGoogleHUD hudProvider) {
                // 获取HUD数据
                GoogleAbstractHUD hud = hudProvider.getGoogleHUD(
                        player.getWorld(), pos,
                        player.getWorld().getBlockState(pos)
                );

                if (hud != null) {
                    // 序列化HUD数据
                    NbtCompound nbt = serializeHudData(hud, player.getRegistryManager());

                    // 发送回客户端
                    ServerPlayNetworking.send(
                            player,
                            new HudNetworkPayloads.SendHudDataPayload(pos, nbt)
                    );
                }
            }
        });
    }

    /**
     * 序列化HUD数据为NBT
     */
    private static NbtCompound serializeHudData(
            GoogleAbstractHUD hud,
            RegistryWrapper.WrapperLookup registries
    ) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("type", hud.getType());

        // 根据类型序列化
        switch (hud.getType()) {
            case "container":
                if (hud instanceof ContainerHUDData containerData) {
                    serializeContainerData(nbt, containerData, registries);
                }
                break;
        }

        return nbt;
    }

    /**
     * 序列化容器数据
     */
    private static void serializeContainerData(
            NbtCompound nbt,
            ContainerHUDData data,
            RegistryWrapper.WrapperLookup registries
    ) {
        NbtCompound containerNbt = new NbtCompound();

        // 序列化标题
        Text title = data.getTitle();
        if (title != null) {
            containerNbt.putString("title", Text.Serialization.toJsonString(title, registries));
        }

        // 序列化尺寸
        containerNbt.putInt("columns", data.getColumns());
        containerNbt.putInt("rows", data.getRows());

        // 序列化物品列表
        NbtList itemsList = new NbtList();
        for (ItemStack stack : data.getItems()) {
            if (!stack.isEmpty()) {
                NbtCompound itemNbt = new NbtCompound();
                stack.encode(registries, itemNbt);
                if (!itemNbt.contains("id")) {
                    itemNbt.putString("id", Registries.ITEM.getId(stack.getItem()).toString());
                }
                itemsList.add(itemNbt);
            }
        }
        containerNbt.put("items", itemsList);

        nbt.put("container_data", containerNbt);
    }

    /**
     * 从NBT反序列化HUD数据
     */
    public static @Nullable GoogleAbstractHUD deserializeHudData(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registries
    ) {
        String type = nbt.getString("type");

        switch (type) {
            case "container":
                return deserializeContainerData(nbt.getCompound("container_data"), registries);
            default:
                return null;
        }
    }

    /**
     * 从NBT反序列化容器数据
     */
    private static @Nullable ContainerHUDData deserializeContainerData(
            NbtCompound containerNbt,
            RegistryWrapper.WrapperLookup registries
    ) {
        if (containerNbt.isEmpty()) {
            return null;
        }

        // 反序列化标题
        Text title = null;
        if (containerNbt.contains("title")) {
            String titleJson = containerNbt.getString("title");
            title = Text.Serialization.fromJson(titleJson, registries);
        }

        // 反序列化尺寸
        int columns = containerNbt.getInt("columns");
        int rows = containerNbt.getInt("rows");

        // 反序列化物品列表
        List<ItemStack> items = new ArrayList<>();
        NbtList itemsList = containerNbt.getList("items", 10); // 10 = COMPOUND

        for (int i = 0; i < itemsList.size(); i++) {
            NbtCompound itemNbt = itemsList.getCompound(i);
            ItemStack stack = ItemStack.fromNbt(registries, itemNbt).orElse(ItemStack.EMPTY);
            items.add(stack);
        }

        return new ContainerHUDData(items, title, columns, rows);
    }
}
