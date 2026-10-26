package com.citygrid.client;

import com.citygrid.Faults;
import com.citygrid.block.PowerPoleBlock;
import com.citygrid.block.SubstationBlock;
import com.citygrid.block.WireNodeBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

/** Рисует провода между узлами: провисающая линия. Если один из концов сломан - провод провисает сильнее. */
public class WireRenderer implements BlockEntityRenderer<WireNodeBlockEntity> {
    private static final int SEGMENTS = 16;

    public WireRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    private static double attachY(BlockState s) {
        Block b = s.getBlock();
        if (b instanceof PowerPoleBlock) return 0.85;
        if (b instanceof SubstationBlock) return 1.0;
        return 0.8;
    }

    private static boolean isBroken(BlockState s) {
        return s.contains(Faults.BROKEN) && s.get(Faults.BROKEN);
    }

    @Override
    public void render(WireNodeBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = be.getWorld();
        if (world == null || be.getLinks().isEmpty()) return;

        BlockPos from = be.getPos();
        BlockState fromState = be.getCachedState();
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f mat = entry.getPositionMatrix();

        for (BlockPos to : be.getLinks()) {
            if (!(world.getBlockEntity(to) instanceof WireNodeBlockEntity)) continue;
            BlockState toState = world.getBlockState(to);

            Vec3d start = new Vec3d(0.5, attachY(fromState), 0.5);
            Vec3d end = new Vec3d(to.getX() - from.getX() + 0.5, to.getY() - from.getY() + attachY(toState),
                    to.getZ() - from.getZ() + 0.5);

            boolean dead = isBroken(fromState) || isBroken(toState);
            double sag = (0.05 * start.distanceTo(end) + 0.15) * (dead ? 3.0 : 1.0);
            int r = dead ? 70 : 25, g = dead ? 50 : 25, b = dead ? 40 : 28;

            Vec3d prev = start;
            for (int i = 1; i <= SEGMENTS; i++) {
                double t = (double) i / SEGMENTS;
                Vec3d cur = start.lerp(end, t).add(0, -sag * 4.0 * t * (1.0 - t), 0);
                Vec3d d = cur.subtract(prev).normalize();
                vc.vertex(mat, (float) prev.x, (float) prev.y, (float) prev.z)
                        .color(r, g, b, 255).normal(entry, (float) d.x, (float) d.y, (float) d.z);
                vc.vertex(mat, (float) cur.x, (float) cur.y, (float) cur.z)
                        .color(r, g, b, 255).normal(entry, (float) d.x, (float) d.y, (float) d.z);
                prev = cur;
            }
        }
    }

    @Override
    public boolean rendersOutsideBoundingBox(WireNodeBlockEntity be) {
        return true;
    }
}
