package com.nekotech.item;


import com.nekotech.NekoTechnology;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.List;
import java.util.Optional;

public class CatBoxItem extends Item{
    public CatBoxItem(Item.Settings settings) {
        super(settings.maxCount(1));  // 纸箱只能堆叠1个
    }

    public static Optional<NbtCompound> getCatData(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            NbtCompound nbt = nbtComponent.copyNbt();
            if (nbt.contains("StoredCat", NbtElement.COMPOUND_TYPE)) {
                return Optional.of(nbt.getCompound("StoredCat"));
            }
        }
        return Optional.empty();
    }

    // 检查是否有猫
    public static boolean hasCat(ItemStack stack) {
        return getCatData(stack).isPresent();
    }

    // 保存猫数据
    public static void saveCatData(ItemStack stack, NbtCompound catData) {
        NbtCompound rootNbt = new NbtCompound();
        rootNbt.put("StoredCat", catData);

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(rootNbt));
    }

    // 清除猫数据
    public static void clearCatData(ItemStack stack) {
        stack.remove(DataComponentTypes.CUSTOM_DATA);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack, world.isClient());
        }

        Optional<NbtCompound> catData = getCatData(stack);

        if (catData.isPresent()) {
            // 有猫，放猫
            if(isLookingAtBlock(user,3)){
                return releaseCat(world, user, stack, catData.get());
            }

        } else {
            // 空纸箱，尝试收猫
            if(getCatInFront(user,3)){
                return tryCatchCat(world, user, stack, hand);
            }
        }
        return TypedActionResult.pass(stack);
    }

    public static boolean isLookingAtBlock(PlayerEntity player, double maxDistance) {
        if (player == null) {
            return false;
        }

        // 进行射线检测
        HitResult hitResult = player.raycast(maxDistance, 0.0f, false);

        // 如果命中类型是 BLOCK，说明选中了方块
        return hitResult.getType() == HitResult.Type.BLOCK;
    }

    public static boolean isLookingAtCat(PlayerEntity player, double maxDistance) {
        HitResult hit = player.raycast(maxDistance, 0.0f, false);
        System.out.println("Hit type: " + hit.getType());
        if (hit.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hit).getEntity();
            return target instanceof CatEntity;
        }
        return false;
    }

    public static boolean getCatInFront(PlayerEntity player, double maxDistance) {
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d playerPos = player.getEyePos();

        Box searchBox = new Box(
                playerPos.x - maxDistance,
                playerPos.y - maxDistance,
                playerPos.z - maxDistance,
                playerPos.x + maxDistance,
                playerPos.y + maxDistance,
                playerPos.z + maxDistance
        );

        List<CatEntity> cats = player.getWorld().getEntitiesByClass(
                CatEntity.class,
                searchBox,
                cat -> cat != null && cat.isAlive()
        );

        for (CatEntity cat : cats) {
            Vec3d toCat = cat.getPos().subtract(playerPos).normalize();
            double dot = lookVec.dotProduct(toCat);

            // 30度内
            if (dot > 0.9) {  // cos(15°) = 0.9
                return true;
            }
        }

        return false;
    }

    private static int getCatTypeByReflection(CatEntity cat) {
        try {
            // 尝试获取 variant 字段
            Field variantField = CatEntity.class.getDeclaredField("variant");
            variantField.setAccessible(true);
            Object variant = variantField.get(cat);

            if (variant instanceof Integer) {
                return (Integer) variant;
            }
            else if (variant instanceof RegistryEntry<?>) {
                RegistryEntry<?> entry = (RegistryEntry<?>) variant;
                if (entry.getKey().isPresent()) {
                    Identifier id = entry.getKey().get().getValue();
                    return variantIdToInt(id);
                }
            }
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        } catch (Exception e) {
        }

        return -1;  // 表示获取失败
    }

    public static int variantIdToInt(Identifier variantId) {
        String path = variantId.getPath().toLowerCase();

        return switch (path) {
            case "tabby" -> 0;           // 花猫
            case "black" -> 1;           // 黑猫
            case "red" -> 2;             // 姜黄猫
            case "siamese" -> 3;         // 暹罗猫
            case "british_shorthair" -> 4;  // 英国短毛猫
            case "calico" -> 5;          // 玳瑁猫
            case "persian" -> 6;         // 波斯猫
            case "ragdoll" -> 7;         // 布偶猫
            case "white" -> 8;           // 白猫
            case "jellie" -> 9;          // 果冻猫
            case "all_black" -> 10;      // 全黑猫
            default -> 0;                // 默认花猫
        };
    }

    public static int getCatType(CatEntity cat) {
        if (cat == null) {
            return 0;  // 默认花猫
        }

        int result = getCatTypeByReflection(cat);
        if (result >= 0) {
            return result;
        }
        // 默认返回花猫
        return 0;
    }

    public static boolean setCatType(CatEntity cat, int catType) {
        if (cat == null) return false;

        // 确保品种在有效范围内
        if (catType < 0 || catType > 10) {
            catType = 0;
        }

        try {
            // 方法1: 尝试直接设置 variant 字段
            Field variantField = CatEntity.class.getDeclaredField("variant");
            variantField.setAccessible(true);
            variantField.set(cat, catType);
            return true;

        } catch (NoSuchFieldException e) {
            // 字段不存在
        } catch (Exception e) {
            // 其他异常
        }

        // 如果直接设置失败，通过NBT方式
        try {
            NbtCompound nbt = new NbtCompound();
            cat.writeNbt(nbt);
            nbt.putInt("CatType", catType);
            cat.readNbt(nbt);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private TypedActionResult<ItemStack> tryCatchCat(World world, PlayerEntity player,
                                                     ItemStack stack, Hand hand) {
        Box searchBox = player.getBoundingBox().expand(3.0);
        List<CatEntity> nearbyCats = world.getEntitiesByType(EntityType.CAT, searchBox,
                cat -> cat != null && cat.isAlive());

        if (!nearbyCats.isEmpty()) {
            CatEntity cat = nearbyCats.get(0);

            // 保存猫数据
            NbtCompound catData = new NbtCompound();
            catData.putInt("Variant", getCatType(cat));

            if (cat.hasCustomName()) {
                catData.putString("CustomName", cat.getCustomName().getString());
            }

            catData.putFloat("Health", cat.getHealth());
            catData.putBoolean("Sitting", cat.isInSittingPose());
            catData.putBoolean("Tamed", cat.isTamed());

            saveCatData(stack, catData);

            // 播放音效
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_CAT_PURR, SoundCategory.NEUTRAL, 1.0F, 1.0F);

            // 移除猫
            cat.discard();

            return TypedActionResult.success(stack, world.isClient());
        }

        return TypedActionResult.pass(stack);
    }
    private BlockPos calculateSpawnPosition(BlockPos blockPos, Direction face) {
        // 根据指向的面计算放置位置
        return switch (face) {
            case UP -> blockPos.up();      // 方块上面
            case DOWN -> blockPos.down();  // 方块下面
            case NORTH -> blockPos.north(); // 方块北面
            case SOUTH -> blockPos.south(); // 方块南面
            case EAST -> blockPos.east();   // 方块东面
            case WEST -> blockPos.west();   // 方块西面
        };
    }

    private Vec3d getExactSpawnPosition(BlockPos spawnPos, Direction face) {
        double x = spawnPos.getX();
        double y = spawnPos.getY();
        double z = spawnPos.getZ();

        // 根据面的不同微调位置
        return switch (face) {
            case UP -> new Vec3d(x, y, z);  // 上面
            case DOWN -> new Vec3d(x, y - 0.2, z);  // 下面，稍微往下
            case NORTH -> new Vec3d(x, y, z - 0.5);  // 北面
            case SOUTH -> new Vec3d(x, y, z + 0.5);  // 南面
            case EAST -> new Vec3d(x + 0.5, y, z);   // 东面
            case WEST -> new Vec3d(x - 0.5, y, z);   // 西面
        };
    }

    private TypedActionResult<ItemStack> releaseCat(World world, PlayerEntity player,
                                                    ItemStack stack, NbtCompound catData) {

        if (world instanceof ServerWorld serverWorld) {
            // 创建新猫
            CatEntity cat = new CatEntity(EntityType.CAT, world);

            // 设置位置
            HitResult hit = player.raycast(3.0, 0.0f, false);

            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos blockPos = blockHit.getBlockPos();
            Direction face = blockHit.getSide();

            // 计算放置位置（在方块面上）
            BlockPos spawnPos = calculateSpawnPosition(blockPos, face);

            NekoTechnology.LOGGER.info(spawnPos.toString());

            Vec3d spawnVec = getExactSpawnPosition(spawnPos, face);

            // 设置位置
            cat.refreshPositionAndAngles(
                    spawnVec.x + 0.5,
                    spawnVec.y,
                    spawnVec.z + 0.5,
                    player.getYaw(),
                    0.0F
            );

            // 应用保存的数据
            if (catData.contains("Variant", NbtElement.INT_TYPE)) {
                setCatType(cat,catData.getInt("Variant"));
            }

            if (catData.contains("CustomName", NbtElement.STRING_TYPE)) {
                String name = catData.getString("CustomName");
                cat.setCustomName(Text.literal(name));
            }

            if (catData.contains("Health", NbtElement.FLOAT_TYPE)) {
                cat.setHealth(catData.getFloat("Health"));
            }

            if (catData.contains("Sitting", NbtElement.BYTE_TYPE)) {
                cat.setInSittingPose(catData.getBoolean("Sitting"));
            }

            if (catData.contains("Tamed", NbtElement.BYTE_TYPE)) {
                cat.setTamed(catData.getBoolean("Tamed"), true);
            }

            // 生成猫
            serverWorld.spawnEntityAndPassengers(cat);

            // 播放音效
            world.playSound(null, spawnPos, SoundEvents.ENTITY_CAT_AMBIENT,
                    SoundCategory.NEUTRAL, 1.0F, 1.0F);

            // 清除数据
            clearCatData(stack);

            return TypedActionResult.success(stack, world.isClient());
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        Optional<NbtCompound> catData = getCatData(stack);

        if (catData.isPresent()) {
            NbtCompound data = catData.get();

            if (data.contains("CustomName", NbtElement.STRING_TYPE)) {
                String name = data.getString("CustomName");
                tooltip.add(Text.literal(Text.translatable("item.neko-technology.neko_box.what_in_it") + name)
                        .formatted(Formatting.GRAY));
            } else {
                tooltip.add(Text.translatable("item.neko-technology.neko_box.what_in_it_defualt")
                        .formatted(Formatting.GRAY));
            }

            tooltip.add(Text.translatable("item.neko-technology.neko_box.put")
                    .formatted(Formatting.DARK_GRAY));
        } else {
            tooltip.add(Text.translatable("item.neko-technology.neko_box.empty")
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("item.neko-technology.neko_box.get")
                    .formatted(Formatting.DARK_GRAY));
        }
    }
}
