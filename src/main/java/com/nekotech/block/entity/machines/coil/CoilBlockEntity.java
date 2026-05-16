package com.nekotech.block.entity.machines.coil;

import com.nekotech.block.entity.CushionBlockEntity;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.ICatNeedMachine;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.IElectricalMachine;
import com.nekotech.block.entity.api.electrical.ITransferElectrical;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.block.entity.machines.HeaterBlockEntity;
import com.nekotech.block.entity.machines.TakeFreelyMachineBlockEntity;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.IHaveGoogleHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import com.nekotech.item.block.CoilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.*;
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
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CoilBlockEntity extends TakeFreelyMachineBlockEntity
        implements ICatNeedMachine, IElectricalMachine, ITransferElectrical, ComponentAdaptation, IHaveGoogleHUD {

    private static final int MAX_COILS = 6;
    private static final int MAX_FLUX = 200;
    private static final float BASE_POWER_CONSUMPTION = 0.12f; // 每个铜线圈的耗电速度
    private static final int HEAT_MULTIPLIER = 180; // 生铁线圈热量乘数
    private static final float STRENGTH_MULTIPLIER = 3; //吸引的力量乘数
    private static final float HEAT_RATE_MULTIPLIER = 0.2f; // 生铁线圈升温速度乘数
    private static final int ATTRACTION_RANGE_MULTIPLIER = 2; // 紫铜线圈吸引范围乘数


    private List<CoilType> coils = new ArrayList<>(MAX_COILS); //线圈栏
    private boolean isFixed = false; //有没有安装框架
    private float nekoFlux = 100;
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

        for (int i = 0; i < MAX_COILS; i++) {
            coils.add(CoilType.EMPTY);
        }

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

    public List<CoilType> getCoils(){
        return coils;
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
        for (CoilType coil : coils) {
            if (coil == CoilType.EMPTY) {
                return true;
            }
        }
        return false;
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
        if (!isActivelyHeating()) {
            return new float[]{0, 0};
        }
        int[] counts = getCoilCounts();
        float tempBonus = counts[1] * counts[0] * HEAT_MULTIPLIER;
        float rateBonus = counts[1] * counts[0] * HEAT_RATE_MULTIPLIER;
        return new float[]{tempBonus, rateBonus};
    }

    public boolean isActivelyHeating() {
        int[] counts = getCoilCounts();
        int pigIronCount = counts[1];
        return pigIronCount > 0 && nekoFlux > 0.01f && canMachineRun();
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
        if (world == null || world.isClient) return;

        int[] counts = getCoilCounts();
        int copperCount = counts[0];
        int nekoCopperCount = counts[2];

        int range = nekoCopperCount * copperCount * ATTRACTION_RANGE_MULTIPLIER;
        if (range <= 0) return;

        if (attractionTickCounter % 2 != 0) {
            attractionTickCounter++;
            return;
        }

        Box attractionBox = new Box(
                pos.getX() - range, pos.getY() - range, pos.getZ() - range,
                pos.getX() + range + 1, pos.getY() + range + 1, pos.getZ() + range + 1
        );

        List<Entity> entities = world.getOtherEntities(null, attractionBox, this::shouldAttract);

        for (Entity entity : entities) {
            if (entity.squaredDistanceTo(Vec3d.ofCenter(pos)) <= range * range) {
                attractSingleEntity(entity, range);
            }
        }

        attractionTickCounter++;

        if (attractionTickCounter % 100 == 0) {
            cleanupAttractedEntities();
        }
    }

    public boolean isFixed() {
        return isFixed;
    }

    /**
     * 吸引单个实体喵
     */
    private void attractSingleEntity(Entity entity, int range) {
        Vec3d entityPos = entity.getPos();
        Vec3d centerPos = Vec3d.ofCenter(pos);

        double distance = entityPos.distanceTo(centerPos);
        if (distance < 0.5) return; // 已经在中心附近

        Vec3d direction = centerPos.subtract(entityPos).normalize();

        double strength = calculateAttractionStrength(entity, distance, range) * STRENGTH_MULTIPLIER;

        double velocityMultiplier;
        if (entity instanceof ItemEntity) {
            velocityMultiplier = 0.04;
        } else {
            velocityMultiplier = 0.08;
        }

        entity.addVelocity(direction.multiply(strength * velocityMultiplier));
        entity.velocityModified = true;

        spawnAttractionParticles(entityPos, direction);
    }

    /**
     * 计算吸引力强度喵
     */
    private double calculateAttractionStrength(Entity entity, double distance, int range) {
        double baseStrength = 0.3;

        //离得越近，吸引力越弱
        double distanceFactor = 1.0 - (distance / range);
        distanceFactor = Math.max(0.1, distanceFactor);

        double typeMultiplier = 1.0;
        if (entity instanceof ItemEntity) {
            typeMultiplier = 2.0;
        } else if (entity instanceof HostileEntity) {
            typeMultiplier = 0.8;
        } else if (entity instanceof IronGolemEntity) {
            typeMultiplier = 1.5;
        } else if (entity instanceof MinecartEntity) {
            typeMultiplier = 1.2;
        }

        return baseStrength * distanceFactor * typeMultiplier;
    }

    /**
     * 判断实体是否应该被吸引喵
     */
    private boolean shouldAttract(Entity entity) {

        if (entity instanceof ItemEntity itemEntity) {
            return isIronItem(itemEntity.getStack());
        }

        if (entity instanceof LivingEntity livingEntity) {
            return isWearingIronArmor(livingEntity) ||
                    isIronGolem(entity) ||
                    isArmorStandWithIron(livingEntity);
        }

        if (entity instanceof MinecartEntity) {
            return true;
        }

        return false;
    }

    /**
     * 检查生物是否穿戴铁/锁链甲喵
     */
    private boolean isWearingIronArmor(LivingEntity entity) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ANIMAL_ARMOR || slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armor = entity.getEquippedStack(slot);
                if (isIronArmor(armor)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查是否为铁/锁链盔甲喵
     */
    private boolean isIronArmor(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor)) {
            return false;
        }

        ArmorMaterial material = armor.getMaterial().value();
        if (material.equals(ArmorMaterials.IRON.value())  || material.equals(ArmorMaterials.CHAIN.value())) {
            return true;
        }

        // 检查物品名
        String armorId = Registries.ITEM.getId(armor).toString().toLowerCase();
        return armorId.contains("iron") || armorId.contains("chain");
    }


    /**
     * 检查物品是否为铁制品喵
     */
    private boolean isIronItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();

        if (stack.isIn(ItemTags.IRON_ORES) ||
                isIronTool(item) ||
                isIronArmor(item)) {
            return true;
        }

        String itemId = Registries.ITEM.getId(item).toString().toLowerCase();
        return itemId.contains("iron") ||
                itemId.contains("steel") ||
                itemId.contains("chain") ||
                itemId.contains("minecart") ||
                itemId.contains("anvil") ||
                itemId.contains("cauldron") ||
                itemId.contains("bucket") ||
                itemId.contains("hopper") ||
                itemId.contains("rail");
    }

    /**
     * 检查是否为铁制工具喵
     */
    private boolean isIronTool(Item item) {
        if (item instanceof ToolItem toolItem) {
            return toolItem.getMaterial().equals(ToolMaterials.IRON);
        }
        return false;
    }

    /**
     * 检查是否为铁制盔甲喵
     */
    private boolean isIronArmor(Item item) {
        if (item instanceof ArmorItem armorItem) {
            return armorItem.getMaterial().value().equals(ArmorMaterials.IRON.value());
        }
        return false;
    }


    /**
     * 检查是否为铁傀儡喵
     */
    private boolean isIronGolem(Entity entity) {
        return entity.getType() == EntityType.IRON_GOLEM;
    }

    /**
     * 检查是否为铁制盔甲架喵
     */
    private boolean isArmorStandWithIron(LivingEntity entity) {
        if (!(entity instanceof ArmorStandEntity)) return false;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ANIMAL_ARMOR || slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armor = entity.getEquippedStack(slot);
                if (isIronArmor(armor)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 清理过期的吸引实体记录喵
     */
    private void cleanupAttractedEntities() {
        if (world == null || world.isClient) return;

        if (world instanceof ServerWorld serverWorld) {
            attractedEntities.removeIf(uuid -> serverWorld.getEntity(uuid) == null);
        }
    }

    /**
     * 生成吸引粒子效果喵
     */
    private void spawnAttractionParticles(Vec3d fromPos, Vec3d direction) {
        if (world == null || world.isClient) return;

        if (world.random.nextInt(5) != 0) return;

        for (int i = 0; i < 3; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.2;
            double offsetY = (world.random.nextDouble() - 0.5) * 0.2;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.2;

            double progress = world.random.nextDouble() * 0.5;
            double x = fromPos.x + direction.x * progress + offsetX;
            double y = fromPos.y + direction.y * progress + offsetY;
            double z = fromPos.z + direction.z * progress + offsetZ;

            world.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0, 0);
        }
    }

    /**
     * 客户端吸引特效喵
     */
    private void spawnClientAttractionParticles() {
        if (world == null || !world.isClient) return;

        int[] counts = getCoilCounts();
        if (counts[2] == 0 || counts[0] == 0) return;

        // 在方块周围生成旋转粒子
        for (int i = 0; i < 3; i++) {
            double angle = (world.getTime() + i * 120) * 0.1;
            double radius = 0.8;

            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double y = pos.getY() + 0.5 + (world.random.nextDouble() - 0.5) * 0.3;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            // 紫色粒子
            world.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0, 0);
        }
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
                updateBlockState();
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
                world.playSound(null, pos, SoundEvents.BLOCK_COPPER_BULB_PLACE,
                        SoundCategory.BLOCKS, 0.5f, 1.0f);
                updateBlockState();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean putInItem(PlayerEntity player, ItemStack stack) {
        Item item = stack.getItem();

        if (item == ModItems.pig_iron_framework ||
                item == ModItems.copper_coil ||
                item == ModItems.pig_iron_coil ||
                item == ModItems.neko_copper_coil) {

            if (item == ModItems.pig_iron_framework) {
                return fixCoils() && consumeItem(player, stack);
            } else if (item == ModItems.copper_coil ||
                    item == ModItems.pig_iron_coil ||
                    item == ModItems.neko_copper_coil) {
                if (isFixed) return false;
                if (hasEmptySlots()) {
                    return addCoil(item) && consumeItem(player, stack);
                }
                return false;
            }
        }

        return super.putInItem(player, stack);
    }

    private boolean consumeItem(PlayerEntity player, ItemStack stack) {
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return true;
    }

    @Override
    public List<GoogleAbstractHUD> getGoogleHUDs(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) return null;

        List<GoogleAbstractHUD> huds = new ArrayList<>();
        int[] counts = getCoilCounts();

        Text title = Text.translatable("block.neko-technology.coil_block").formatted(Formatting.GOLD);
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

        if (world != null && world.getTime() % 5 == 0 && temperature > 50) {
            spawnHeatParticles();
        }

        if (world != null && world.getTime() % 3 == 0) {
            spawnClientAttractionParticles();
        }
    }

    public void serverTick(World world, BlockPos pos, BlockState state) {
        baseTick(world, pos, state);

        int[] counts = getCoilCounts();
        int copperCount = counts[0];
        int pigIronCount = counts[1];
        int nekoCopperCount = counts[2];

        if (copperCount > 0 && nekoFlux > 0) {
            float consumption = copperCount * BASE_POWER_CONSUMPTION;
            nekoFlux = Math.max(0, nekoFlux - consumption);
        }

        if (pigIronCount > 0 && nekoFlux > 0.01f) {
            if (temperature < maxTemperature) {
                temperature = Math.min(maxTemperature, temperature + heatRate);
                if (world.getTime() % 5 == 0 && temperature > 50) {
                    spawnHeatParticles();
                }
            } else if (temperature > 0) {
                temperature = Math.max(0, temperature - 0.5f);
            }
        }

        if (nekoCopperCount > 0 && copperCount > 0) {
            if (!canMachineRun()) {
                return;
            }
            attractEntities();
        }

        tickComponents();
        markDirty();
    }

    private void updateBlockState() {
        if (world != null && !world.isClient) {
            BlockState currentState = world.getBlockState(pos);

            int filledLayers = 0;
            for (CoilType coil : coils) {
                if (coil != CoilType.EMPTY) filledLayers++;
            }

            if (currentState.get(CoilBlock.LAYERS) != filledLayers) {
                BlockState newState = currentState.with(CoilBlock.LAYERS, filledLayers);
                world.setBlockState(pos, newState, Block.NOTIFY_ALL);
            }
        }
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
