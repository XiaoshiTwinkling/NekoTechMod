package com.nekotech.renderer.components;

import com.nekotech.network.payload.s2c.SyncWirePairsPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class WirePoleRenderer {

    // ================= 调试常量 =================
    private static final float LINE_WIDTH = 0.0625f;    // 线条宽度（方块单位，1像素=0.0625）
    private static final float LINE_HEIGHT = 0.0625f;   // 线条厚度（方块单位，1像素）
    private static final float SAG_AMOUNT = 0.2f;      // 下垂程度
    private static final int SEGMENTS = 20;             // 平滑度
    private static final float HEAD_OFFSET = 0.20f;    // 接线柱头部突出距离
    private static final float BRIGHTNESS = 2f;      // 亮度倍增系数
    // ==============================================

    private static final List<WirePairData> pairs = new ArrayList<>();

    public static void updatePairs(List<SyncWirePairsPayload.WirePairData> newPairs) {
        pairs.clear();
        for (var p : newPairs) {
            pairs.add(new WirePairData(p.pos1(), p.side1(), p.pos2(), p.side2(), p.wireType()));
        }
    }

    public static void initialize() {
        WorldRenderEvents.AFTER_ENTITIES.register(WirePoleRenderer::onAfterEntities);
    }

    private static void onAfterEntities(WorldRenderContext context) {
        if (pairs.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        Vec3d cameraPos = context.camera().getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // 固定最大光照
        int light = LightmapTextureManager.pack(15, 15);

        for (WirePairData pair : pairs) {
            Vec3d start = getWirePoleHead(pair.pos1, pair.side1);
            Vec3d end = getWirePoleHead(pair.pos2, pair.side2);

            if (cameraPos.squaredDistanceTo(start) > 6400 &&
                    cameraPos.squaredDistanceTo(end) > 6400) continue;

            int color = getWireColor(pair.wireType);
            renderThickLine(matrices, consumers, start, end, color, light);
        }

        matrices.pop();

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    /**
     * 渲染有厚度的线缆（长方体，双面渲染，使用 Solid 层 + 固定最大光照 + 亮度倍增）
     */
    private static void renderThickLine(MatrixStack matrices, VertexConsumerProvider consumers,
                                        Vec3d start, Vec3d end, int color, int light) {
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getSolid());

        Vec3d direction = end.subtract(start);
        double length = direction.length();
        if (length < 0.001) return;

        direction = direction.normalize();

        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right = direction.crossProduct(worldUp).normalize();
        if (right.lengthSquared() < 0.001) {
            right = new Vec3d(0, 0, 1).crossProduct(direction).normalize();
        }
        Vec3d localUp = right.crossProduct(direction).normalize();

        double halfW = LINE_WIDTH * 0.5;
        double halfH = LINE_HEIGHT * 0.5;

        List<Vec3d> points = calculateCatenary(start, end);

        MatrixStack.Entry entry = matrices.peek();

        // 颜色乘以亮度系数，并限制在 0~1
        float r = Math.min(((color >> 16) & 0xFF) / 255.0f * BRIGHTNESS, 1.0f);
        float g = Math.min(((color >> 8) & 0xFF) / 255.0f * BRIGHTNESS, 1.0f);
        float b = Math.min((color & 0xFF) / 255.0f * BRIGHTNESS, 1.0f);

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3d p1 = points.get(i);
            Vec3d p2 = points.get(i + 1);

            Vec3d[] c = new Vec3d[8];
            c[0] = p1.add(right.multiply(-halfW)).add(localUp.multiply(-halfH));
            c[1] = p1.add(right.multiply(halfW)).add(localUp.multiply(-halfH));
            c[2] = p1.add(right.multiply(-halfW)).add(localUp.multiply(halfH));
            c[3] = p1.add(right.multiply(halfW)).add(localUp.multiply(halfH));
            c[4] = p2.add(right.multiply(-halfW)).add(localUp.multiply(-halfH));
            c[5] = p2.add(right.multiply(halfW)).add(localUp.multiply(-halfH));
            c[6] = p2.add(right.multiply(-halfW)).add(localUp.multiply(halfH));
            c[7] = p2.add(right.multiply(halfW)).add(localUp.multiply(halfH));

            Vector3f nFront = new Vector3f((float)direction.x, (float)direction.y, (float)direction.z);
            Vector3f nBack = new Vector3f(-(float)direction.x, -(float)direction.y, -(float)direction.z);
            Vector3f nRight = new Vector3f((float)right.x, (float)right.y, (float)right.z);
            Vector3f nLeft = new Vector3f(-(float)right.x, -(float)right.y, -(float)right.z);
            Vector3f nUp = new Vector3f((float)localUp.x, (float)localUp.y, (float)localUp.z);
            Vector3f nDown = new Vector3f(-(float)localUp.x, -(float)localUp.y, -(float)localUp.z);

            // 双面渲染
            addQuad(buffer, entry, c[0], c[1], c[3], c[2], r, g, b, light, nFront);
            addQuad(buffer, entry, c[0], c[2], c[3], c[1], r, g, b, light, new Vector3f(nFront).negate());
            addQuad(buffer, entry, c[4], c[5], c[7], c[6], r, g, b, light, nBack);
            addQuad(buffer, entry, c[4], c[6], c[7], c[5], r, g, b, light, new Vector3f(nBack).negate());
            addQuad(buffer, entry, c[0], c[2], c[6], c[4], r, g, b, light, nLeft);
            addQuad(buffer, entry, c[0], c[4], c[6], c[2], r, g, b, light, new Vector3f(nLeft).negate());
            addQuad(buffer, entry, c[1], c[3], c[7], c[5], r, g, b, light, nRight);
            addQuad(buffer, entry, c[1], c[5], c[7], c[3], r, g, b, light, new Vector3f(nRight).negate());
            addQuad(buffer, entry, c[2], c[3], c[7], c[6], r, g, b, light, nUp);
            addQuad(buffer, entry, c[2], c[6], c[7], c[3], r, g, b, light, new Vector3f(nUp).negate());
            addQuad(buffer, entry, c[0], c[1], c[5], c[4], r, g, b, light, nDown);
            addQuad(buffer, entry, c[0], c[4], c[5], c[1], r, g, b, light, new Vector3f(nDown).negate());
        }
    }

    private static void addQuad(VertexConsumer buffer, MatrixStack.Entry entry,
                                Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4,
                                float r, float g, float b, int light, Vector3f normal) {
        buffer.vertex(entry.getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z)
                .color(r, g, b, 1.0f)
                .texture(0, 0)
                .light(light)
                .normal(entry, normal.x(), normal.y(), normal.z());
        buffer.vertex(entry.getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z)
                .color(r, g, b, 1.0f)
                .texture(0, 0)
                .light(light)
                .normal(entry, normal.x(), normal.y(), normal.z());
        buffer.vertex(entry.getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z)
                .color(r, g, b, 1.0f)
                .texture(0, 0)
                .light(light)
                .normal(entry, normal.x(), normal.y(), normal.z());
        buffer.vertex(entry.getPositionMatrix(), (float)v4.x, (float)v4.y, (float)v4.z)
                .color(r, g, b, 1.0f)
                .texture(0, 0)
                .light(light)
                .normal(entry, normal.x(), normal.y(), normal.z());
    }

    private static Vec3d getWirePoleHead(BlockPos pos, Direction side) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        double offset = HEAD_OFFSET;
        return switch (side) {
            case DOWN -> new Vec3d(cx, pos.getY() - offset, cz);
            case UP -> new Vec3d(cx, pos.getY() + 1 + offset, cz);
            case NORTH -> new Vec3d(cx, cy, pos.getZ() - offset);
            case SOUTH -> new Vec3d(cx, cy, pos.getZ() + 1 + offset);
            case WEST -> new Vec3d(pos.getX() - offset, cy, cz);
            case EAST -> new Vec3d(pos.getX() + 1 + offset, cy, cz);
        };
    }

    private static List<Vec3d> calculateCatenary(Vec3d start, Vec3d end) {
        List<Vec3d> points = new ArrayList<>();
        double distance = start.distanceTo(end);
        for (int i = 0; i <= WirePoleRenderer.SEGMENTS; i++) {
            double t = (double) i / WirePoleRenderer.SEGMENTS;
            double x = start.x + (end.x - start.x) * t;
            double y = start.y + (end.y - start.y) * t;
            double z = start.z + (end.z - start.z) * t;
            double drop = WirePoleRenderer.SAG_AMOUNT * Math.sin(t * Math.PI) * distance * 0.1;
            y -= drop;
            points.add(new Vec3d(x, y, z));
        }
        return points;
    }

    private static int getWireColor(String wireType) {
        return switch (wireType) {
            case "copper" -> 0xB87333;
            case "brass" -> 0xC9AE5D;
            case "neko_copper" -> 0xFF6B6B;
            default -> 0xCCCCCC;
        };
    }

    private record WirePairData(BlockPos pos1, Direction side1, BlockPos pos2, Direction side2, String wireType) {}
}