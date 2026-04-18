package com.nekotech.network;

import com.nekotech.NekoTechnology;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        PayloadTypeRegistry.playS2C().register(
                HudNetworkPayloads.SEND_RAY_POS,
                HudNetworkPayloads.SendRayPosPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                HudNetworkPayloads.REMOVE_RAY_POS,
                HudNetworkPayloads.RemoveRayPosPayload.CODEC
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
                // 获取HUDs数据
                java.util.List<GoogleAbstractHUD> huds = hudProvider.getGoogleHUDs(
                        player.getWorld(), pos,
                        player.getWorld().getBlockState(pos)
                );

                if (huds != null && !huds.isEmpty()) {
                    // 序列化HUD列表
                    NbtCompound nbt = serializeHudList(huds, player.getRegistryManager());

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
     * 序列化HUD列表为NBT喵~
     * 这个方法会把多个HUD打包成一个NBT列表喵~
     */
    private static NbtCompound serializeHudList(
            java.util.List<GoogleAbstractHUD> huds,
            RegistryWrapper.WrapperLookup registries
    ) {
        NbtCompound nbt = new NbtCompound();
        NbtList hudList = new NbtList();  // 创建一个NBT列表来存放所有HUD喵~

        for (GoogleAbstractHUD hud : huds) {
            NbtCompound hudNbt = serializeHudData(hud, registries);
            hudList.add(hudNbt);
        }

        nbt.put("huds", hudList);
        return nbt;
    }

    /**
     * 从NBT反序列化HUD列表喵~
     */
    public static @Nullable java.util.List<GoogleAbstractHUD> deserializeHudList(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registries,
            BlockPos pos
    ) {
        if (!nbt.contains("huds")) {
            return null;  // 如果没有，说明是旧格式的单HUD包，返回null喵~
        }

        NbtList hudList = nbt.getList("huds", 10);  // 10 对应 NbtElement.COMPOUND_TYPE 喵~
        java.util.List<GoogleAbstractHUD> huds = new java.util.ArrayList<>();

        // 遍历NBT列表，把每个HUD NBT都反序列化喵~
        for (int i = 0; i < hudList.size(); i++) {
            NbtCompound hudNbt = hudList.getCompound(i);
            GoogleAbstractHUD hud = deserializeHudData(hudNbt, registries, pos);
            if (hud != null) {
                huds.add(hud);  // 把反序列化成功的HUD添加到列表里喵~
            }
        }

        return huds;
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
            case "info_box":
                if (hud instanceof InfoBoxHUDData infoBoxData) {
                    serializeInfoBoxData(nbt, infoBoxData, registries);
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
            NbtCompound itemNbt = new NbtCompound();

            if (!stack.isEmpty()) {
                // 序列化物品
                stack.encode(registries, itemNbt);

                itemNbt.putInt("Count", stack.getCount());

                if (!itemNbt.contains("id")) {
                    // 获取物品ID
                    Identifier itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
                    if (itemId != null) {
                        itemNbt.putString("id", itemId.toString());
                    }
                }

                itemsList.add(itemNbt);
            } else {
                NbtCompound ItemNbt = new NbtCompound();
                itemNbt.putBoolean("empty", true);
                itemsList.add(ItemNbt);
            }
        }

        containerNbt.put("items", itemsList);

        nbt.put("container_data", containerNbt);
    }

    /**
     * 序列化信息框数据
     */
    private static void serializeInfoBoxData(
            NbtCompound nbt,
            InfoBoxHUDData data,
            RegistryWrapper.WrapperLookup registries
    ) {
        NbtCompound infoBoxNbt = new NbtCompound();

        // 序列化标题
        if (data.getTitle() != null) {
            infoBoxNbt.putString("title", Text.Serialization.toJsonString(data.getTitle(), registries));
        }

        // 序列化内容
        if (data.getContent() != null) {
            infoBoxNbt.putString("content", Text.Serialization.toJsonString(data.getContent(), registries));
        }

        // 序列化宽度
        infoBoxNbt.putInt("maxWidth", data.getMaxWidth());

        nbt.put("info_box_data", infoBoxNbt);
    }

    /**
     * 从NBT反序列化HUD数据
     */
    public static @Nullable GoogleAbstractHUD deserializeHudData(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registries,
            BlockPos pos
    ) {
        String type = nbt.getString("type");

        switch (type) {
            case "container":
                return deserializeContainerData(nbt.getCompound("container_data"), registries, pos);
            case "info_box":
                return deserializeInfoBoxData(nbt.getCompound("info_box_data"), registries, pos);
            default:
                return null;
        }
    }

    /**
     * 从NBT反序列化容器数据
     */
    private static @Nullable ContainerHUDData deserializeContainerData(
            NbtCompound containerNbt,
            RegistryWrapper.WrapperLookup registries,
            BlockPos pos) {
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
        NbtList itemsList = containerNbt.getList("items", 10);


        for (int i = 0; i < itemsList.size(); i++) {
            NbtCompound itemNbt = itemsList.getCompound(i);

            if (itemNbt.contains("empty") && itemNbt.getBoolean("empty")) {
                items.add(ItemStack.EMPTY);
                continue;
            }

            if (!itemNbt.contains("id")) {
                items.add(ItemStack.EMPTY);
                continue;
            }

            ItemStack stack = ItemStack.fromNbt(registries, itemNbt).orElse(ItemStack.EMPTY);

            int savedCount = 1;
            if (itemNbt.contains("Count", 99)) {
                savedCount = itemNbt.getInt("Count");
            } else if (itemNbt.contains("count", 99)) {
                savedCount = itemNbt.getInt("count");
            }

            if (savedCount > 1 && !stack.isEmpty()) {
                stack.setCount(savedCount);
            }

            NekoTechnology.LOGGER.info(stack.toString());

            items.add(stack);
        }

        return new ContainerHUDData(pos, items, title, columns, rows);
    }

    /**
     * 从NBT反序列化信息框数据
     */
    private static @Nullable InfoBoxHUDData deserializeInfoBoxData(
            NbtCompound infoBoxNbt,
            RegistryWrapper.WrapperLookup registries,
            BlockPos pos) {
        if (infoBoxNbt.isEmpty()) {
            return null;
        }

        // 反序列化标题
        Text title = null;
        if (infoBoxNbt.contains("title")) {
            try {
                String titleJson = infoBoxNbt.getString("title");
                Optional<Text> result = Optional.ofNullable(Text.Serialization.fromJson(titleJson, registries));
                if (result.isPresent()) {
                    title = result.get();
                }
            } catch (Exception e) {
                com.nekotech.NekoTechnology.LOGGER.error("解析标题失败", e);
            }
        }

        // 反序列化内容
        Text content = null;
        if (infoBoxNbt.contains("content")) {
            try {
                String contentJson = infoBoxNbt.getString("content");
                Optional<Text> result = Optional.ofNullable(Text.Serialization.fromJson(contentJson, registries));
                if (result.isPresent()) {
                    content = result.get();
                }
            } catch (Exception e) {
                com.nekotech.NekoTechnology.LOGGER.error("解析内容失败", e);
            }
        }

        // 获取宽度
        int maxWidth = infoBoxNbt.getInt("maxWidth");
        if (maxWidth <= 0) {
            maxWidth = 200; // 默认值
        }

        if (title == null && content == null) {
            return null;
        }

        return new InfoBoxHUDData(pos, title, content, maxWidth);
    }
}
