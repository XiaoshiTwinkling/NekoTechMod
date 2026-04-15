package com.nekotech.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ClientLaserTargetCache {

    private static final Map<UUID, Vec3d> TARGETS = new HashMap<>();

    private static final Map<UUID, Vec3d> RENDER_POS = new HashMap<>();

    private static final Identifier RED_PIXEL =
            Identifier.of("neko-technology", "textures/misc/red_pixel.png");

    public static void set(UUID uuid, Vec3d pos) {
        TARGETS.put(uuid, pos);
    }

    public static void remove(UUID uuid) {
        TARGETS.remove(uuid);
        RENDER_POS.remove(uuid);
    }

    public static void tick() {
        double speed = 0.5;

        Iterator<Map.Entry<UUID, Vec3d>> it = RENDER_POS.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Vec3d> entry = it.next();
            UUID uuid = entry.getKey();

            Vec3d target = TARGETS.get(uuid);

            // 被删除
            if (target == null) {
                it.remove();
                continue;
            }

            Vec3d current = entry.getValue();

            Vec3d delta = target.subtract(current);
            double distSq = delta.lengthSquared();

            Vec3d next;

            if (distSq > speed * speed) {
                next = current.add(delta.normalize().multiply(speed));
            } else {
                next = target;
            }

            entry.setValue(next);
        }

        // 初始化新目标
        for (Map.Entry<UUID, Vec3d> entry : TARGETS.entrySet()) {
            RENDER_POS.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    public static void render(WorldRenderContext context) {
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        VertexConsumer vc = consumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(RED_PIXEL)
        );

        for (Vec3d pos : RENDER_POS.values()) {

            double maxDist = 16.0;
            if (pos.squaredDistanceTo(camPos) > maxDist * maxDist) {
                continue;
            }

            matrices.push();

            matrices.translate(
                    pos.x - camPos.x,
                    pos.y - camPos.y,
                    pos.z - camPos.z
            );

            float s = 1f / 16f;
            matrices.scale(s, s, s);

            MatrixStack.Entry entry = matrices.peek();
            Matrix4f mat = entry.getPositionMatrix();

            float x1 = -0.5f, x2 = 0.5f;
            float y1 = -0.5f, y2 = 0.5f;
            float z1 = -0.5f, z2 = 0.5f;

            int light = 0xF000F0;

            // DOWN
            vertex(vc, mat, entry, x1,y1,z1, 0,0, light, 0,1,0);
            vertex(vc, mat, entry, x2,y1,z1, 1,0, light, 0,1,0);
            vertex(vc, mat, entry, x2,y1,z2, 1,1, light, 0,1,0);
            vertex(vc, mat, entry, x1,y1,z2, 0,1, light, 0,1,0);

            // UP
            vertex(vc, mat, entry, x1,y2,z2, 0,0, light, 0,1,0);
            vertex(vc, mat, entry, x2,y2,z2, 1,0, light, 0,1,0);
            vertex(vc, mat, entry, x2,y2,z1, 1,1, light, 0,1,0);
            vertex(vc, mat, entry, x1,y2,z1, 0,1, light, 0,1,0);

            // NORTH
            vertex(vc, mat, entry, x1,y1,z2, 0,1, light, 0,1,0);
            vertex(vc, mat, entry, x2,y1,z2, 1,1, light, 0,1,0);
            vertex(vc, mat, entry, x2,y2,z2, 1,0, light, 0,1,0);
            vertex(vc, mat, entry, x1,y2,z2, 0,0, light, 0,1,0);

            // SOUTH
            vertex(vc, mat, entry, x1,y2,z1, 0,0, light, 0,1,0);
            vertex(vc, mat, entry, x2,y2,z1, 1,0, light, 0,1,0);
            vertex(vc, mat, entry, x2,y1,z1, 1,1, light, 0,1,0);
            vertex(vc, mat, entry, x1,y1,z1, 0,1, light, 0,1,0);

            // WEST
            vertex(vc, mat, entry, x1,y1,z1, 0,1, light, 0,1,0);
            vertex(vc, mat, entry, x1,y1,z2, 1,1, light, 0,1,0);
            vertex(vc, mat, entry, x1,y2,z2, 1,0, light, 0,1,0);
            vertex(vc, mat, entry, x1,y2,z1, 0,0, light, 0,1,0);

            // EAST
            vertex(vc, mat, entry, x2,y1,z2, 0,1, light, 0,1,0);
            vertex(vc, mat, entry, x2,y1,z1, 1,1, light, 0,1,0);
            vertex(vc, mat, entry, x2,y2,z1, 1,0, light, 0,1,0);
            vertex(vc, mat, entry, x2,y2,z2, 0,0, light, 0,1,0);

            matrices.pop();
        }
    }

    private static void vertex(VertexConsumer vc,
                               Matrix4f mat,
                               MatrixStack.Entry entry,
                               float x, float y, float z,
                               float u, float v,
                               int light,
                               float nx, float ny, float nz) {

        vc.vertex(mat, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, nx, ny, nz);
    }
}
