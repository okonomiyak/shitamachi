package uk.iwaservice.shitamachi.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class DenpoleBlockEntityRenderer implements BlockEntityRenderer<DenpoleBlockEntity> {

    public DenpoleBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(DenpoleBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;

        BlockPos sourcePos = be.getBlockPos();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();

        net.minecraft.world.phys.Vec3 cam =
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double srcDistSq = cam.distanceToSqr(sourcePos.getX() + 0.5, sourcePos.getY() + 0.5, sourcePos.getZ() + 0.5);

        for (BlockPos targetPos : be.getConnections()) {
            if (!(level.getBlockEntity(targetPos) instanceof DenpoleBlockEntity)) continue;
            double tgtDistSq = cam.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
            if (srcDistSq > tgtDistSq) continue;

            float dx = targetPos.getX() - sourcePos.getX();
            float dy = targetPos.getY() - sourcePos.getY();
            float dz = targetPos.getZ() - sourcePos.getZ();

            renderCatenary(consumer, matrix,
                    0.5f, 15.0f / 16.0f, 0.5f,
                    dx + 0.5f, dy + 15.0f / 16.0f, dz + 0.5f);
        }
    }

    private void renderCatenary(VertexConsumer consumer, Matrix4f matrix,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2) {
        int segments = 24;
        float hDist = (float) Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
        float sag = hDist * 0.05f;

        float[] px = new float[segments + 1];
        float[] py = new float[segments + 1];
        float[] pz = new float[segments + 1];

        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float u = t - 0.5f;
            px[i] = x1 + (x2 - x1) * t;
            py[i] = y1 + (y2 - y1) * t - sag * (1.0f - 4.0f * u * u);
            pz[i] = z1 + (z2 - z1) * t;
        }

        float w = 0.025f;
        int r = 40, g = 40, b = 45;
        int light = (15 << 20) | (15 << 4);

        Vector3f cam = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
        float cx = cam.x(), cy = cam.y(), cz = cam.z();

        for (int i = 0; i <= segments; i++) {
            float tx, ty, tz;
            if (i < segments) {
                tx = px[i + 1] - px[i]; ty = py[i + 1] - py[i]; tz = pz[i + 1] - pz[i];
            } else {
                tx = px[i] - px[i - 1]; ty = py[i] - py[i - 1]; tz = pz[i] - pz[i - 1];
            }
            float wx = ty * cz - tz * cy;
            float wy = tz * cx - tx * cz;
            float wz = tx * cy - ty * cx;
            float wLen = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
            if (wLen > 1e-5f) { wx /= wLen; wy /= wLen; wz /= wLen; }

            consumer.addVertex(matrix, px[i] - wx * w, py[i] - wy * w, pz[i] - wz * w)
                    .setColor(r, g, b, 255)
                    .setUv2(light >> 16 & 0xFFFF, light & 0xFFFF);
            consumer.addVertex(matrix, px[i] + wx * w, py[i] + wy * w, pz[i] + wz * w)
                    .setColor(r, g, b, 255)
                    .setUv2(light >> 16 & 0xFFFF, light & 0xFFFF);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(DenpoleBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
