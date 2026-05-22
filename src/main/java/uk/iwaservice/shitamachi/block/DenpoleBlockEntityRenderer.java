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

public class DenpoleBlockEntityRenderer<T extends CableBlockEntity> implements BlockEntityRenderer<T> {

    public DenpoleBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(T be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;

        BlockPos sourcePos = be.getBlockPos();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();

        net.minecraft.world.phys.Vec3 cam =
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double srcDistSq = cam.distanceToSqr(sourcePos.getX() + 0.5, sourcePos.getY() + 0.5, sourcePos.getZ() + 0.5);

        for (ConnectionData conn : be.getConnections()) {
            BlockPos targetPos = conn.pos();
            if (!(level.getBlockEntity(targetPos) instanceof CableBlockEntity)) continue;
            double tgtDistSq = cam.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
            if (srcDistSq > tgtDistSq) continue;

            float dx = targetPos.getX() - sourcePos.getX();
            float dy = targetPos.getY() - sourcePos.getY();
            float dz = targetPos.getZ() - sourcePos.getZ();

            renderCatenary(consumer, matrix,
                    0.5f, 0.5f, 0.5f,
                    dx + 0.5f, dy + 0.5f, dz + 0.5f,
                    conn.thickness(), conn.sag());
        }
    }

    private void renderCatenary(VertexConsumer consumer, Matrix4f matrix,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float w, float sagFactor) {
        int segments = 24;
        float hDist = (float) Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
        float sag = hDist * sagFactor;

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

        float[] frameRx = new float[segments + 1];
        float[] frameRy = new float[segments + 1];
        float[] frameRz = new float[segments + 1];
        float[] frameUx = new float[segments + 1];
        float[] frameUy = new float[segments + 1];
        float[] frameUz = new float[segments + 1];

        for (int i = 0; i <= segments; i++) {
            float tx, ty, tz;
            if (i < segments) {
                tx = px[i + 1] - px[i]; ty = py[i + 1] - py[i]; tz = pz[i + 1] - pz[i];
            } else {
                tx = px[i] - px[i - 1]; ty = py[i] - py[i - 1]; tz = pz[i] - pz[i - 1];
            }
            float tLen = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (tLen > 1e-5f) { tx /= tLen; ty /= tLen; tz /= tLen; }

            float rcx = -tz, rcz = tx;
            float rLen = (float) Math.sqrt(rcx * rcx + rcz * rcz);
            if (rLen > 1e-5f) { rcx /= rLen; rcz /= rLen; } else { rcx = 1; rcz = 0; }

            float ucx = 0 * tz - rcz * ty;
            float ucy = rcz * tx - rcx * tz;
            float ucz = rcx * ty - 0 * tx;
            float uLen = (float) Math.sqrt(ucx * ucx + ucy * ucy + ucz * ucz);
            if (uLen > 1e-5f) { ucx /= uLen; ucy /= uLen; ucz /= uLen; }

            frameRx[i] = rcx; frameRy[i] = 0; frameRz[i] = rcz;
            frameUx[i] = ucx; frameUy[i] = ucy; frameUz[i] = ucz;
        }

        int r = 40, g = 40, b = 45;
        int light = (15 << 20) | (15 << 4);

        float[] rm0 = {-1, +1, +1, -1};
        float[] um0 = {+1, +1, -1, -1};
        float[] rm1 = {+1, +1, -1, -1};
        float[] um1 = {+1, -1, -1, +1};

        for (int face = 0; face < 4; face++) {
            for (int i = 0; i <= segments; i++) {
                float ax = px[i] + (frameRx[i] * rm0[face] + frameUx[i] * um0[face]) * w;
                float ay = py[i] + (frameRy[i] * rm0[face] + frameUy[i] * um0[face]) * w;
                float az = pz[i] + (frameRz[i] * rm0[face] + frameUz[i] * um0[face]) * w;
                float bx = px[i] + (frameRx[i] * rm1[face] + frameUx[i] * um1[face]) * w;
                float by = py[i] + (frameRy[i] * rm1[face] + frameUy[i] * um1[face]) * w;
                float bz = pz[i] + (frameRz[i] * rm1[face] + frameUz[i] * um1[face]) * w;
                consumer.addVertex(matrix, ax, ay, az).setColor(r, g, b, 255).setUv2(light >> 16 & 0xFFFF, light & 0xFFFF);
                consumer.addVertex(matrix, bx, by, bz).setColor(r, g, b, 255).setUv2(light >> 16 & 0xFFFF, light & 0xFFFF);
            }

            if (face < 3) {
                int nf = face + 1;
                float lx = px[segments] + (frameRx[segments] * rm1[face] + frameUx[segments] * um1[face]) * w;
                float ly = py[segments] + (frameRy[segments] * rm1[face] + frameUy[segments] * um1[face]) * w;
                float lz = pz[segments] + (frameRz[segments] * rm1[face] + frameUz[segments] * um1[face]) * w;
                float fx = px[0] + (frameRx[0] * rm0[nf] + frameUx[0] * um0[nf]) * w;
                float fy = py[0] + (frameRy[0] * rm0[nf] + frameUy[0] * um0[nf]) * w;
                float fz = pz[0] + (frameRz[0] * rm0[nf] + frameUz[0] * um0[nf]) * w;
                consumer.addVertex(matrix, lx, ly, lz).setColor(r, g, b, 255).setUv2(light >> 16 & 0xFFFF, light & 0xFFFF);
                consumer.addVertex(matrix, fx, fy, fz).setColor(r, g, b, 255).setUv2(light >> 16 & 0xFFFF, light & 0xFFFF);
                consumer.addVertex(matrix, fx, fy, fz).setColor(r, g, b, 255).setUv2(light >> 16 & 0xFFFF, light & 0xFFFF);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
