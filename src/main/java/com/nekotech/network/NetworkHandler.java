package com.nekotech.network;

import com.nekotech.NekoTechnology;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import com.nekotech.item.custom.NekoTag.NekoTagData;
import com.nekotech.screen.NekoTag.NekoTagScreenHandler;
import com.nekotech.util.NekoTask;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.InventoryProvider;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NetworkHandler {
    public static void initialize() {
        // 注册网络包类型
        PayloadTypeRegistry.playC2S().register(
                NetworkPayloads.REQUEST_HUD_DATA,
                NetworkPayloads.RequestHudDataPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                NetworkPayloads.SEND_HUD_DATA,
                NetworkPayloads.SendHudDataPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                NetworkPayloads.SEND_RAY_POS,
                NetworkPayloads.SendRayPosPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                NetworkPayloads.REMOVE_RAY_POS,
                NetworkPayloads.RemoveRayPosPayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                NetworkPayloads.NekoTagUpdatePayload.ID,
                NetworkPayloads.NekoTagUpdatePayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                NetworkPayloads.NekoTagUpdatePayload.ID,
                (payload, context) -> {
                    context.server().execute(() -> handleNekoTagUpdate(context.player(), payload));
                }
        );


        // 注册请求处理器
        ServerPlayNetworking.registerGlobalReceiver(
                NetworkPayloads.REQUEST_HUD_DATA,
                NetworkHandler::handleHudDataRequest
        );
    }

    private static void handleNekoTagUpdate(ServerPlayerEntity player, NetworkPayloads.NekoTagUpdatePayload payload) {

        Hand hand = Hand.valueOf(payload.hand());
        ItemStack tagStack = player.getStackInHand(hand);

        if (!tagStack.isOf(ModItems.neko_tag)) {
            return;
        }

        DyeColor color = DyeColor.byName(payload.color(), DyeColor.WHITE);

        short priority = payload.priority();
        if (priority < 0) {
            priority = 0;
        }

        NekoTask task = NekoTask.byId(payload.task());

        ItemStack displayStack = ItemStack.EMPTY;
        Identifier displayId = Identifier.tryParse(payload.displayItemId());

        if (displayId != null) {
            Item item = Registries.ITEM.get(displayId);

            if (item != Items.AIR) {
                displayStack = new ItemStack(item);
            }
        }

        NekoTagData.save(
                tagStack,
                color,
                priority,
                task,
                displayStack
        );

        player.getInventory().markDirty();
    }

    private static void handleHudDataRequest(
            NetworkPayloads.RequestHudDataPayload payload,
            ServerPlayNetworking.Context context
    ) {
        BlockPos pos = payload.pos();

        // 在主线程处理
        context.server().execute(() -> {
            ServerPlayerEntity player = context.player();
            World world = player.getWorld();

            // 获取方块实体
            var blockEntity = world.getBlockEntity(pos);
            List<GoogleAbstractHUD> huds = new ArrayList<>();

            if (blockEntity instanceof IHaveGoogleHUD hudProvider) {
                huds = hudProvider.getGoogleHUDs(world, pos, world.getBlockState(pos));
            } else {
                // 尝试多种方式获取原版容器
                Inventory inventory = getInventoryAt(world, pos);
                if (inventory != null) {
                    huds = createContainerHUDForVanillaContainer(inventory, world, pos);
                }
            }

            if (huds != null && !huds.isEmpty()) {
                // 序列化HUD列表
                NbtCompound nbt = serializeHudList(huds, player.getRegistryManager());

                // 发送回客户端
                ServerPlayNetworking.send(
                        player,
                        new NetworkPayloads.SendHudDataPayload(pos, nbt)
                );
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

    /**
     * 为原版容器创建容器HUD数据喵~
     * 这个方法会读取原版容器的物品栏并创建对应的ContainerHUDData喵~
     */
    private static List<GoogleAbstractHUD> createContainerHUDForVanillaContainer(
            Inventory inventory,
            World world,
            BlockPos pos
    ) {
        List<GoogleAbstractHUD> huds = new ArrayList<>();

        // 获取容器中的物品
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            items.add(stack.copy()); // 复制一份，避免修改原物品
        }

        // 获取容器的标题（使用方块的可翻译名称）
        BlockState state = world.getBlockState(pos);
        Text containerTitle = state.getBlock().getName(); // 获取方块的本地化名称

        // 计算容器的行列数（根据容器类型）
        int columns = calculateColumnsForContainer(inventory);
        int rows = calculateRowsForContainer(inventory, columns);

        // 创建ContainerHUDData
        ContainerHUDData containerHUD = new ContainerHUDData(pos, items, containerTitle, columns, rows);
        huds.add(containerHUD);

        return huds;
    }

    /**
     * 根据容器类型计算列数喵~
     * 这是一个简化实现，您可以根据实际需求调整喵~
     */
    private static int calculateColumnsForContainer(Inventory inventory) {
        int size = inventory.size();

        // 常见原版容器的列数
        if (size == 27) return 9; // 箱子
        if (size == 54) return 9; // 大箱子
        if (size == 5) return 5;  // 投掷器/发射器
        if (size == 3) return 3;  // 炼药锅（特殊）

        // 默认：如果小于9格，就按实际大小；否则用9列
        return Math.min(size, 9);
    }

    /**
     * 根据容器大小和列数计算行数喵~
     */
    private static int calculateRowsForContainer(Inventory inventory, int columns) {
        int size = inventory.size();
        if (columns == 0) return 0;
        return (size + columns - 1) / columns; // 向上取整
    }

    @Nullable
    private static Inventory getInventoryAt(World world, BlockPos pos) {
        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof Inventory inventory) {
            return inventory;
        }

        var block = world.getBlockState(pos).getBlock();
        if (block instanceof InventoryProvider inventoryProvider) {
            return inventoryProvider.getInventory(world.getBlockState(pos), world, pos);
        }

        return null;
    }
}
