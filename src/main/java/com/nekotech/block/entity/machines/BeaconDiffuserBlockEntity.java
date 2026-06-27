package com.nekotech.block.entity.machines;

import com.nekotech.NekoTechnology;
import com.nekotech.block.custom.BeaconDiffuserBlock;
import com.nekotech.block.entity.ModBlockEntities;
import com.nekotech.block.entity.api.IHaveGoogleHUD;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.block.entity.api.electrical.IElectricalAppliance;
import com.nekotech.block.entity.api.electrical.conductor.ConductorSystem;
import com.nekotech.item.ModItems;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.templates.InfoBoxHUDData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 信标扩散器
 */
public class BeaconDiffuserBlockEntity extends BlockEntity
        implements IElectricalAppliance, IHaveGoogleHUD, ComponentAdaptation {

    // 能耗参数
    private static final float BASE_CONSUMPTION_PER_LEVEL = 0.5f;  // 每层基础消耗
    private static final int EFFECT_DURATION = 220;                // 效果持续时间（ticks），稍长于间隔
    private static final int APPLY_INTERVAL = 40;                  // 每2秒应用一次效果

    private float nekoFlux = 500;
    private int beaconLevel = 0;                                   // 缓存的信标层数
    private StatusEffect primaryEffect = null;
    private StatusEffect secondaryEffect = null;
    private int tickCounter = 0;

    // 零件系统
    private final Map<Direction, Item> attachedComponents = new EnumMap<>(Direction.class);
    private final Set<Item> validComponents = new HashSet<>();

    public BeaconDiffuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEACON_DIFFUSER, pos, state);
        initValidComponents();
    }

    private void initValidComponents() {
        validComponents.add(ModItems.BRASS_FLUX_INPUTER);
        validComponents.add(ModItems.NEKO_COPPER_FLUX_INPUTER);
    }

    public static void tick(World world, BlockPos pos, BlockState state, BeaconDiffuserBlockEntity be) {
        if (world.isClient) return;

        be.tickCounter++;
        if (be.tickCounter % 20 == 0) {
            be.updateBeaconStatus();
        }

        if (be.beaconLevel <= 0 || be.primaryEffect == null) {
            return;
        }

        float consumption = be.beaconLevel * BASE_CONSUMPTION_PER_LEVEL;
        if (be.nekoFlux < consumption) {
            return;
        }

        be.nekoFlux -= consumption;
        be.markDirty();

        if (be.tickCounter % APPLY_INTERVAL == 0) {
            be.applyEffectsToFriendlyMobs();
        }

        if (!state.get(BeaconDiffuserBlock.ACTIVE)) {
            world.setBlockState(pos, state.with(BeaconDiffuserBlock.ACTIVE, true), 3);
        }


    }

    /**
     * 更新下方信标的信息
     */
    public void updateBeaconStatus() {
        if (world == null || world.isClient) return;

        BlockPos belowPos = pos.down();
        BlockEntity be = world.getBlockEntity(belowPos);
        if (be instanceof BeaconBlockEntity beacon) {
            try {
                java.lang.reflect.Field levelField = BeaconBlockEntity.class.getDeclaredField("level");
                levelField.setAccessible(true);
                this.beaconLevel = levelField.getInt(beacon);

                java.lang.reflect.Field primaryField = null;
                try {
                    primaryField = BeaconBlockEntity.class.getDeclaredField("primaryEffect");
                } catch (NoSuchFieldException e) {
                    primaryField = BeaconBlockEntity.class.getDeclaredField("primary"); // Yarn 备用名
                }
                primaryField.setAccessible(true);
                Object primaryObj = primaryField.get(beacon);
                if (primaryObj instanceof RegistryEntry<?> entry) {
                    this.primaryEffect = (StatusEffect) entry.value();
                } else if (primaryObj instanceof StatusEffect effect) {
                    this.primaryEffect = effect;
                } else {
                    this.primaryEffect = null;
                }

                java.lang.reflect.Field secondaryField = null;
                try {
                    secondaryField = BeaconBlockEntity.class.getDeclaredField("secondaryEffect");
                } catch (NoSuchFieldException e) {
                    secondaryField = BeaconBlockEntity.class.getDeclaredField("secondary"); // Yarn 备用名
                }
                secondaryField.setAccessible(true);
                Object secondaryObj = secondaryField.get(beacon);
                if (secondaryObj instanceof RegistryEntry<?> entry) {
                    this.secondaryEffect = (StatusEffect) entry.value();
                } else if (secondaryObj instanceof StatusEffect effect) {
                    this.secondaryEffect = effect;
                } else {
                    this.secondaryEffect = null;
                }

            }  catch (NoSuchFieldException | IllegalAccessException ignored) {}
        } else {
            this.beaconLevel = 0;
            this.primaryEffect = null;
            this.secondaryEffect = null;
        }

        if (this.beaconLevel <= 0 && world.getBlockState(pos).get(BeaconDiffuserBlock.ACTIVE)) {
            world.setBlockState(pos, world.getBlockState(pos).with(BeaconDiffuserBlock.ACTIVE, false), 3);
        }
        markDirty();
    }

    /**
     * 给信标范围内的友好生物施加效果
     */
    private void applyEffectsToFriendlyMobs() {
        if (world == null || world.isClient || beaconLevel <= 0) return;

        int range = 10 + 10 * beaconLevel;
        Box box = new Box(pos).expand(range);

        List<LivingEntity> entities = world.getNonSpectatingEntities(LivingEntity.class, box);

        for (LivingEntity entity : entities) {
            if (isFriendly(entity)) {
                if (primaryEffect != null) {
                    entity.addStatusEffect(new StatusEffectInstance(
                            Registries.STATUS_EFFECT.getEntry(primaryEffect),
                            EFFECT_DURATION,
                            beaconLevel - 1,
                            true, true
                    ));
                }
                if (secondaryEffect != null && beaconLevel >= 4) {
                    entity.addStatusEffect(new StatusEffectInstance(
                            Registries.STATUS_EFFECT.getEntry(secondaryEffect),
                            EFFECT_DURATION,
                            0,
                            true, true
                    ));
                }
            }
        }
    }
    /**
     * 判断是否为友好生物（非怪物、非玩家？玩家也可受益？根据需求可调整）
     */
    private boolean isFriendly(LivingEntity entity) {
        return entity instanceof PassiveEntity;
    }

    @Override
    public float getNekoFlux() {
        return nekoFlux;
    }

    @Override
    public void setNekoFlux(float value) {
        this.nekoFlux = Math.max(0, value);
        markDirty();
    }

    @Override
    public float getMaxNekoFlux() {
        return 1000;
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
    public void removeComponent(Direction side) {
        attachedComponents.remove(side);
        if (world != null && !world.isClient) {
            ConductorSystem.onComponentChanged((ServerWorld) world, pos, side);
        }
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putFloat("NekoFlux", nekoFlux);
        nbt.putInt("BeaconLevel", beaconLevel);
        if (primaryEffect != null) {
            nbt.putString("PrimaryEffect", Registries.STATUS_EFFECT.getId(primaryEffect).toString());
        }
        if (secondaryEffect != null) {
            nbt.putString("SecondaryEffect", Registries.STATUS_EFFECT.getId(secondaryEffect).toString());
        }

        NbtCompound componentsNbt = new NbtCompound();
        for (Map.Entry<Direction, Item> entry : attachedComponents.entrySet()) {
            componentsNbt.putString(entry.getKey().getName(), Registries.ITEM.getId(entry.getValue()).toString());
        }
        nbt.put("AttachedComponents", componentsNbt);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.nekoFlux = nbt.getFloat("NekoFlux");
        this.beaconLevel = nbt.getInt("BeaconLevel");
        if (nbt.contains("PrimaryEffect")) {
            this.primaryEffect = Registries.STATUS_EFFECT.get(Identifier.tryParse(nbt.getString("PrimaryEffect")));
        } else {
            this.primaryEffect = null;
        }
        if (nbt.contains("SecondaryEffect")) {
            this.secondaryEffect = Registries.STATUS_EFFECT.get(Identifier.tryParse(nbt.getString("SecondaryEffect")));
        } else {
            this.secondaryEffect = null;
        }

        attachedComponents.clear();
        if (nbt.contains("AttachedComponents", NbtElement.COMPOUND_TYPE)) {
            NbtCompound compNbt = nbt.getCompound("AttachedComponents");
            for (String sideName : compNbt.getKeys()) {
                Direction side = Direction.byName(sideName);
                Item item = Registries.ITEM.get(Identifier.tryParse(compNbt.getString(sideName)));
                if (side != null && item != Items.AIR) {
                    attachedComponents.put(side, item);
                }
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    @Nullable
    public GoogleAbstractHUD getGoogleHUD(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) return null;
        Text title = Text.translatable("block.neko-technology.beacon_diffuser").formatted(Formatting.GOLD);
        Text content = Text.literal("")
                .append(Text.translatable("hud.beacon_diffuser.level", beaconLevel).formatted(Formatting.AQUA))
                .append("\n")
                .append(Text.translatable("hud.beacon_diffuser.flux", String.format("%.1f", nekoFlux)).formatted(Formatting.YELLOW))
                .append("\n")
                .append(Text.translatable("hud.beacon_diffuser.effect",
                                primaryEffect != null ? Text.translatable(primaryEffect.getTranslationKey()) : Text.translatable("hud.none"))
                        .formatted(Formatting.GREEN));
        return new InfoBoxHUDData(pos, title, content);
    }
}
