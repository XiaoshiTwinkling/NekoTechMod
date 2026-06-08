package com.nekotech.renderer;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;


public class TailRenderer {

    private static final Identifier TEX_3 =
            Identifier.of("neko-technology", "textures/entity/cat_tail_3.png");
    private static final Identifier TEX_2 =
            Identifier.of("neko-technology", "textures/entity/cat_tail_2.png");
    private static final Identifier TEX_1 =
            Identifier.of("neko-technology", "textures/entity/cat_tail_1.png");

    public void render(MatrixStack matrices,
                       VertexConsumerProvider vertices,
                       int light,
                       AbstractClientPlayerEntity player,
                       float age,
                       float limbAngle,
                       float limbDistance) {

        matrices.push();

        float t = age * 0.2F;

        // 根部
        float r1 = MathHelper.sin(t) * 8F + limbDistance * 12F;
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5F)); // 上翘
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(r1));   // 原摆动
        renderSegment(matrices, vertices.getBuffer(RenderLayer.getEntityCutout(TEX_1)), light);
        matrices.translate(0, 0, 3F / 16F);

        // 中段
        float r2 = MathHelper.sin(t + 0.5F) * 12F;
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(r2));
        renderSegment(matrices, vertices.getBuffer(RenderLayer.getEntityCutout(TEX_2)), light);
        matrices.translate(0, 0, 3F / 16F);

        // 尾尖
        float r3 = MathHelper.sin(t + 1.0F) * 16F;
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(r3));
        renderSegment(matrices, vertices.getBuffer(RenderLayer.getEntityCutout(TEX_3)), light);

        matrices.pop();
    }

    private void renderSegment(MatrixStack matrices, VertexConsumer vc, int light) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f mat = entry.getPositionMatrix();

        float x1 = (7.5F - 8F) / 16F;
        float x2 = (8.5F - 8F) / 16F;
        float y1 = (11F - 12F) / 16F;
        float y2 = (12F - 12F) / 16F;
        float z1 = 0F;
        float z2 = 3F / 16F;

        drawCube(mat, entry, vc, light, x1, x2, y1, y2, z1, z2);
    }

    private float uv(int px) {
        return px / 16F;
    }

    private void drawCube(Matrix4f mat,
                          MatrixStack.Entry entry,
                          VertexConsumer vc,
                          int light,
                          float x1, float x2,
                          float y1, float y2,
                          float z1, float z2) {

        // NORTH
        quad(vc, mat, entry, x1,y1,z2, x2,y1,z2, x2,y2,z2, x1,y2,z2,
                uv(4), uv(1), uv(3), uv(0),
                light, 0, 0, -1);

        // SOUTH
        quad(vc, mat, entry, x1,y2,z1, x2,y2,z1, x2,y1,z1, x1,y1,z1,
                uv(3), uv(3), uv(2), uv(2),
                light, 0, 0, 1);

        // EAST
        quad(vc, mat, entry, x2,y1,z2, x2,y1,z1, x2,y2,z1, x2,y2,z2,
                uv(3), uv(1), uv(0), uv(0),
                light, 1, 0, 0);

        // WEST
        quad(vc, mat, entry, x1,y1,z1, x1,y1,z2, x1,y2,z2, x1,y2,z1,
                uv(3), uv(2), uv(0), uv(1),
                light, -1, 0, 0);

        // UP
        quad(vc, mat, entry, x1,y2,z2, x2,y2,z2, x2,y2,z1, x1,y2,z1,
                uv(2), uv(5), uv(1), uv(2),
                light, 0, 1, 0);

        // DOWN
        quad(vc, mat, entry, x1,y1,z1, x2,y1,z1, x2,y1,z2, x1,y1,z2,
                uv(1), uv(5), uv(0), uv(2),
                light, 0, -1, 0);
    }

    private void quad(VertexConsumer vc,
                      Matrix4f mat,
                      MatrixStack.Entry entry,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float x4, float y4, float z4,
                      float u1, float v1,
                      float u2, float v2,
                      int light,
                      float nx, float ny, float nz) {

        vertex(vc, mat, entry, x1,y1,z1, u1,v2, light, nx, ny, nz);
        vertex(vc, mat, entry, x2,y2,z2, u2,v2, light, nx, ny, nz);
        vertex(vc, mat, entry, x3,y3,z3, u2,v1, light, nx, ny, nz);
        vertex(vc, mat, entry, x4,y4,z4, u1,v1, light, nx, ny, nz);
    }

    private void vertex(VertexConsumer vc,
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