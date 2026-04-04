package com.nekotech.item.custom;

import com.nekotech.NekoTechnology;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class CatBoxItem extends Item {

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
            return TypedActionResult.success(stack, true);
        }

        Optional<NbtCompound> catData = getCatData(stack);

        if (catData.isPresent()) {
            // 有猫，放猫
            if(isLookingAtBlock(user,3)){
                return releaseCat(world, user, stack, catData.get());
            }
        } else {
            CatEntity cat = raycastCat(user, 3.0);
            if (cat != null) {
                return tryCatchCat(world, user, stack, cat);
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

    public static CatEntity raycastCat(PlayerEntity player, double maxDistance) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(maxDistance));

        Box searchBox = player.getBoundingBox()
                .stretch(direction.multiply(maxDistance))
                .expand(1.0D);

        EntityHitResult result = ProjectileUtil.getEntityCollision(
                player.getWorld(),
                player,
                start,
                end,
                searchBox,
                entity -> entity instanceof CatEntity && entity.isAlive()
        );

        if (result == null) {
            return null;
        }

        Entity entity = result.getEntity();
        return entity instanceof CatEntity cat ? cat : null;
    }

    //删除一些有可能会出bug的数据
    public static NbtCompound sanitizeCatNbt(NbtCompound nbt) {
        // 唯一标识
        nbt.remove("UUID");

        // 位置运动
        nbt.remove("Pos");
        nbt.remove("Motion");
        nbt.remove("Rotation");

        // 维度世界
        nbt.remove("Dimension");
        nbt.remove("WorldUUID");

        // 实体类型标识
        nbt.remove("id");

        nbt.remove("Passengers");
        nbt.remove("Leash");

        return nbt;
    }

    private TypedActionResult<ItemStack> tryCatchCat(World world, PlayerEntity player,
                                                     ItemStack stack, CatEntity cat) {
        if (cat == null || !cat.isAlive()) {
            return TypedActionResult.pass(stack);
        }

        NbtCompound catData = new NbtCompound();
        cat.writeNbt(catData);
        sanitizeCatNbt(catData);

        saveCatData(stack, catData);

        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_CAT_PURR,
                SoundCategory.NEUTRAL,
                1.0F, 1.0F
        );

        cat.discard();

        return TypedActionResult.success(stack, world.isClient());
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
        if (!(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.pass(stack);
        }

        HitResult hit = player.raycast(3.0, 0.0f, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return TypedActionResult.pass(stack);
        }

        BlockPos blockPos = blockHit.getBlockPos();
        Direction face = blockHit.getSide();

        BlockPos spawnPos = calculateSpawnPosition(blockPos, face);
        Vec3d spawnVec = getExactSpawnPosition(spawnPos, face);

        NekoTechnology.LOGGER.info("Spawn cat at {}", spawnPos);

        CatEntity cat = new CatEntity(EntityType.CAT, world);

        // 恢复数据
        cat.readNbt(catData);

        cat.refreshPositionAndAngles(
                spawnVec.x + 0.5,
                spawnVec.y,
                spawnVec.z + 0.5,
                player.getYaw(),
                0.0F
        );

        serverWorld.spawnEntityAndPassengers(cat);

            // 播放音效
            world.playSound(null, spawnPos, SoundEvents.ENTITY_CAT_AMBIENT,
                    SoundCategory.NEUTRAL, 1.0F, 1.0F);

        clearCatData(stack);

        return TypedActionResult.success(stack, world.isClient());
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
