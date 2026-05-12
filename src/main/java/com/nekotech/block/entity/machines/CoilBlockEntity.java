package com.nekotech.block.entity.machines;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.CushionBlockEntity;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.ICatNeedMachine;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.IElectricalMachine;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import com.nekotech.modTags.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.html.parser.Entity;
import java.util.*;

public class CoilBlockEntity extends TakeFreelyMachineBlockEntity
        implements ICatNeedMachine, IElectricalMachine, ITransferElectrical, ComponentAdaptation, IHaveGoogleHUD {

    private static final int MAX_COILS = 6;
    private static final int MAX_FLUX = 200;
    private static final float BASE_POWER_CONSUMPTION = 1.5f; // 每个铜线圈的耗电速度
    private static final int HEAT_MULTIPLIER = 180; // 生铁线圈热量乘数
    private static final float HEAT_RATE_MULTIPLIER = 1.0f; // 生铁线圈升温速度乘数
    private static final int ATTRACTION_RANGE_MULTIPLIER = 4; // 紫铜线圈吸引范围乘数

    // 线圈の枚举
    private enum CoilType {
        COPPER(ModItems.copper_coil, 0xFFD700, "copper"),
        PIG_IRON(ModItems.pig_iron_coil, 0xC0C0C0, "pig_iron"),
        NEKO_COPPER(ModItems.neko_copper_coil, 0x8B4513, "neko_copper"),
        EMPTY(null, 0x000000, "empty");

        final Item item;
        final int color;
        final String id;

        CoilType(Item item, int color, String id) {
            this.item = item;
            this.color = color;
            this.id = id;
        }

        static CoilType fromItem(Item item) {
            for (CoilType type : values()) {
                if (type.item == item) return type;
            }
            return EMPTY;
        }
    }

    private final DefaultedList<CoilType> coils = DefaultedList.ofSize(MAX_COILS, CoilType.EMPTY); //线圈栏
    private boolean isFixed = false; //有没有安装框架
    private float nekoFlux = 0;
    private float temperature = 0;
    private float maxTemperature = 0;
    private float heatRate = 0;

    private BlockPos boundCushionPos = null;

    private final Map<Direction, Item> attachedComponents = new EnumMap<>(Direction.class); //经典零件二人组
    private final Set<Item> validComponents = new HashSet<>();

    private final List<UUID> attractedEntities = new ArrayList<>(); //吸引铁质物品的表
    private int attractionTickCounter = 0;

    public CoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.coil_block, pos, state, MAX_COILS);
        initValidComponents();
    }

    private void initValidComponents() {
        validComponents.add(ModItems.brass_flux_inputer);
        validComponents.add(ModItems.brass_flux_outputer);
        validComponents.add(ModItems.neko_copper_flux_inputer);
        validComponents.add(ModItems.neko_copper_flux_outputer);
        validComponents.add(ModItems.component_casing);
    }

    /**
     * 尝试缠绕一个线圈喵
     * @return 是否成功缠绕
     */
    public boolean addCoil(Item coilItem) {
        if (isFixed) return false;

        for (int i = 0; i < coils.size(); i++) {
            if (coils.get(i) == CoilType.EMPTY) {
                coils.set(i, CoilType.fromItem(coilItem));
                recalculateProperties();
                markDirty();
                updateNeighbors();
                return true;
            }
        }
        return false;
    }

    /**
     * 用生铁框架固定线圈喵
     */
    public boolean fixCoils() {
        if (isFixed || hasEmptySlots()) return false;

        isFixed = true;
        markDirty();
        if (world != null && !world.isClient) {
            world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE,
                    SoundCategory.BLOCKS, 0.5f, 1.0f);
        }
        return true;
    }

    /**
     * 检查是否有空槽位喵
     */
    public boolean hasEmptySlots() {
        return coils.contains(CoilType.EMPTY);
    }

    /**
     * 获取线圈数量统计喵
     */
    public int[] getCoilCounts() {
        int copper = 0, pigIron = 0, nekoCopper = 0;
        for (CoilType coil : coils) {
            if (coil == CoilType.COPPER) copper++;
            else if (coil == CoilType.PIG_IRON) pigIron++;
            else if (coil == CoilType.NEKO_COPPER) nekoCopper++;
        }
        return new int[]{copper, pigIron, nekoCopper};
    }

    /**
     * 重新计算线圈属性喵
     */
    private void recalculateProperties() {
        int[] counts = getCoilCounts();
        int copper = counts[0];
        int pigIron = counts[1];

        // 计算最大温度
        maxTemperature = pigIron * copper * HEAT_MULTIPLIER;

        // 计算升温速度
        heatRate = pigIron * copper * HEAT_RATE_MULTIPLIER;
    }

    /**
     * 获取加热效果喵
     * @return 最大温度加成, 升温速度加成
     */
    public float[] getHeatBonus() {
        int[] counts = getCoilCounts();
        float tempBonus = counts[1] * counts[0] * HEAT_MULTIPLIER;
        float rateBonus = counts[1] * counts[0] * HEAT_RATE_MULTIPLIER;
        return new float[]{tempBonus, rateBonus};
    }

    /**
     * 加热器更新喵
     */
    private void updateNeighbors() {
        if (world == null || world.isClient) return;

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = world.getBlockEntity(pos.offset(dir));
            if (neighbor instanceof HeaterBlockEntity heater) {
                heater.onStructureChanged();
            }
        }
    }

    /**
     * 吸引附近的含铁实体喵
     */
    private void attractEntities() {

    }

    //api

    @Override
    public void setBoundCushion(BlockPos pos) {
        this.boundCushionPos = pos;
        markDirty();
    }

    @Override
    public CushionBlockEntity getBoundCushion() {
        if (world != null && boundCushionPos != null) {
            BlockEntity be = world.getBlockEntity(boundCushionPos);
            if (be instanceof CushionBlockEntity cushion) {
                return cushion;
            }
        }
        return null;
    }

    @Override
    public float getNekoFlux() {
        return nekoFlux;
    }

    @Override
    public void setNekoFlux(float value) {
        this.nekoFlux = Math.max(0, Math.min(value, MAX_FLUX));
        markDirty();
    }

    @Override
    public float getMaxNekoFlux() {
        return MAX_FLUX;
    }

    @Override
    public Map<Direction, Item> getAttachedComponents() {
        return attachedComponents;
    }

    @Override
    public Set<Item> getValidComponents() {
        return validComponents;
    }

    @Override
    public boolean attachComponent(Direction side, Item component) {
        if (!canAttachComponent(side, component)) return false;

        attachedComponents.put(side, component);

        if (world != null && !world.isClient) {
            ConductorSystem.onComponentChanged((ServerWorld) world, pos, side);
        }

        markDirty();
        return true;
    }

    @Override
    public boolean handleRightClick(PlayerEntity player, ItemStack stack) {
        if (player.getWorld().isClient) return true;

        Item item = stack.getItem();

        if (item == ModItems.pig_iron_framework) {
            if (fixCoils()) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                return true;
            }
        }

        if (item == ModItems.copper_coil ||
                item == ModItems.pig_iron_coil ||
                item == ModItems.neko_copper_coil) {

            if (addCoil(item)) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                world.playSound(null, pos, SoundEvents.BLOCK_WOOL_PLACE,
                        SoundCategory.BLOCKS, 0.5f, 1.0f);
                return true;
            }
        }

        // 3. 默认物品交互
        return super.handleRightClick(player, stack);
    }

    // 覆盖putInItem，只允许放入线圈相关物品
    @Override
    public boolean putInItem(PlayerEntity player, ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.copper_coil ||
                item == ModItems.pig_iron_coil ||
                item == ModItems.neko_copper_coil ||
                item == ModItems.pig_iron_framework) {
            return handleRightClick(player, stack);
        }
        return false;
    }

    @Override
    public List<GoogleAbstractHUD> getGoogleHUDs(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) return null;

        List<GoogleAbstractHUD> huds = new ArrayList<>();
        int[] counts = getCoilCounts();

        Text title = Text.translatable("block.neko-technology.coil").formatted(Formatting.GOLD);
        Text content = Text.literal("")
                .append(Text.translatable("hud.coil.copper", counts[0]).formatted(Formatting.YELLOW))
                .append("\n")
                .append(Text.translatable("hud.coil.pig_iron", counts[1]).formatted(Formatting.GRAY))
                .append("\n")
                .append(Text.translatable("hud.coil.neko_copper", counts[2]).formatted(Formatting.RED))
                .append("\n")
                .append(Text.translatable("hud.coil.flux", String.format("%.1f", nekoFlux), MAX_FLUX).formatted(Formatting.AQUA))
                .append("\n")
                .append(Text.translatable("hud.coil.fixed",
                                isFixed ? Text.translatable("hud.yes") : Text.translatable("hud.no"))
                        .formatted(isFixed ? Formatting.GREEN : Formatting.RED));

        huds.add(new InfoBoxHUDData(pos, title, content));
        return huds;
    }


    public static void tick(World world, BlockPos pos, BlockState state, CoilBlockEntity blockEntity) {
        if (world.isClient) {
            blockEntity.clientTick();
        } else {
            blockEntity.serverTick(world, pos, state);
        }
    }

    public void clientTick() {
        // 客户端粒子效果
        if (world != null && world.getTime() % 5 == 0 && temperature > 50) {
            spawnHeatParticles();
        }
    }

    public void serverTick(World world, BlockPos pos, BlockState state) {
        lazytick(world, pos, state);

        if (!canMachineRun()) {
            return;
        }

        int[] counts = getCoilCounts();
        int copperCount = counts[0];

        if (copperCount > 0 && nekoFlux > 0) {
            float consumption = copperCount * BASE_POWER_CONSUMPTION;
            nekoFlux = Math.max(0, nekoFlux - consumption);
        }

        if (temperature < maxTemperature) {
            temperature = Math.min(maxTemperature, temperature + heatRate);
        } else if (temperature > 0) {
            temperature = Math.max(0, temperature - 0.5f);
        }

        attractEntities();

        tickComponents();

        markDirty();
    }

    private void spawnHeatParticles() {
        // 热热的粒子效果喵
        if (world.random.nextFloat() < 0.1f) {
            double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 1.5;
            double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.3;
            world.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0.05, 0);
        }
    }

    private void tickComponents() {
        if (world == null || world.isClient) return;

        for (Direction side : Direction.values()) {
            Item component = getComponent(side);
            if (component != null) {
                componentTick(world, side);
            }
        }
    }


    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        // 线圈数据
        NbtList coilList = new NbtList();
        for (CoilType coil : coils) {
            coilList.add(NbtString.of(coil.id));
        }
        nbt.put("Coils", coilList);
        nbt.putBoolean("IsFixed", isFixed);

        // 能量和温度
        nbt.putFloat("NekoFlux", nekoFlux);
        nbt.putFloat("Temperature", temperature);
        nbt.putFloat("MaxTemperature", maxTemperature);
        nbt.putFloat("HeatRate", heatRate);

        // 猫垫绑定
        if (boundCushionPos != null) {
            nbt.putInt("CushionX", boundCushionPos.getX());
            nbt.putInt("CushionY", boundCushionPos.getY());
            nbt.putInt("CushionZ", boundCushionPos.getZ());
        }

        // 零件
        NbtCompound componentsNbt = new NbtCompound();
        for (Map.Entry<Direction, Item> entry : attachedComponents.entrySet()) {
            String sideName = entry.getKey().getName();
            String itemId = Registries.ITEM.getId(entry.getValue()).toString();
            componentsNbt.putString(sideName, itemId);
        }
        nbt.put("AttachedComponents", componentsNbt);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        // 线圈数据喵
        coils.clear();
        if (nbt.contains("Coils", NbtElement.LIST_TYPE)) {
            NbtList coilList = nbt.getList("Coils", NbtElement.STRING_TYPE);
            for (int i = 0; i < Math.min(MAX_COILS, coilList.size()); i++) {
                String coilId = coilList.getString(i);
                CoilType type = Arrays.stream(CoilType.values())
                        .filter(t -> t.id.equals(coilId))
                        .findFirst()
                        .orElse(CoilType.EMPTY);
                coils.add(type);
            }
        }
        while (coils.size() < MAX_COILS) {
            coils.add(CoilType.EMPTY);
        }

        isFixed = nbt.getBoolean("IsFixed");

        // 能量/温度喵
        nekoFlux = nbt.getFloat("NekoFlux");
        temperature = nbt.getFloat("Temperature");
        maxTemperature = nbt.getFloat("MaxTemperature");
        heatRate = nbt.getFloat("HeatRate");

        // 猫垫绑定喵
        if (nbt.contains("CushionX")) {
            int x = nbt.getInt("CushionX");
            int y = nbt.getInt("CushionY");
            int z = nbt.getInt("CushionZ");
            boundCushionPos = new BlockPos(x, y, z);
        }

        // 零件喵
        attachedComponents.clear();
        if (nbt.contains("AttachedComponents", NbtElement.COMPOUND_TYPE)) {
            NbtCompound componentsNbt = nbt.getCompound("AttachedComponents");
            for (String sideName : componentsNbt.getKeys()) {
                Direction side = Direction.byName(sideName);
                String itemId = componentsNbt.getString(sideName);
                Item item = Registries.ITEM.get(Identifier.tryParse(itemId));
                if (side != null) {
                    attachedComponents.put(side, item);
                }
            }
        }

        recalculateProperties();
    }


    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt, registries);
        return nbt;
    }
}
