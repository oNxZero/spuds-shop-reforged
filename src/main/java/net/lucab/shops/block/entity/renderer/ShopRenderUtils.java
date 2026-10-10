package net.lucab.shops.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.lucab.shops.block.entity.AbstractShopEntity.RendererData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;

import static java.lang.Math.atan2;
import static net.lucab.shops.block.entity.renderer.ShopIconModels.NO_STOCK;
import static net.lucab.shops.block.entity.renderer.ShopIconModels.REG_FULL;

public final class ShopRenderUtils {

    private ShopRenderUtils() {
    }

    private record IconBounds(float width, float height, float x, float y) {}
    private static final java.util.Map<net.minecraft.client.resources.model.BakedModel, IconBounds> ICON_BOUNDS =
            new java.util.WeakHashMap<>();

    private static IconBounds iconBounds(net.minecraft.client.resources.model.BakedModel model) {
        return ICON_BOUNDS.computeIfAbsent(model, key -> {
            PoseStack transform = new PoseStack();
            key.getTransforms().getTransform(ItemDisplayContext.GUI).apply(false, transform);
            transform.translate(-.5f, -.5f, -.5f);
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            if (!key.isGui3d()) {
                var sprite = key.getParticleIcon().contents();
                int left = sprite.width(), top = sprite.height(), right = 0, bottom = 0;
                for (int y = 0; y < sprite.height(); y++) for (int x = 0; x < sprite.width(); x++) {
                    if (!sprite.isTransparent(0, x, y)) {
                        left = Math.min(left, x); top = Math.min(top, y);
                        right = Math.max(right, x + 1); bottom = Math.max(bottom, y + 1);
                    }
                }
                for (float x : new float[]{(float)left / sprite.width(), (float)right / sprite.width()})
                    for (float y : new float[]{1f - (float)bottom / sprite.height(), 1f - (float)top / sprite.height()}) {
                        var v = new org.joml.Vector3f(x, y, .5f).mulPosition(transform.last().pose());
                        minX = Math.min(minX, v.x); maxX = Math.max(maxX, v.x);
                        minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y);
                    }
            } else {
                for (int side = -1; side < 6; side++) {
                    var random = net.minecraft.util.RandomSource.create(42);
                    for (var quad : key.getQuads(null, side < 0 ? null : net.minecraft.core.Direction.values()[side], random)) {
                        int[] vertices = quad.getVertices();
                        int stride = vertices.length / 4;
                        for (int i = 0; i < 4; i++) {
                            var v = new org.joml.Vector3f(Float.intBitsToFloat(vertices[i * stride]),
                                    Float.intBitsToFloat(vertices[i * stride + 1]),
                                    Float.intBitsToFloat(vertices[i * stride + 2])).mulPosition(transform.last().pose());
                            minX = Math.min(minX, v.x); maxX = Math.max(maxX, v.x);
                            minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y);
                        }
                    }
                }
            }
            if (!Float.isFinite(minX) || maxY <= minY || maxX <= minX) return new IconBounds(1, 1, 0, 0);
            return new IconBounds(maxX - minX, maxY - minY, (minX + maxX) / 2f, (minY + maxY) / 2f);
        });
    }

    /** Lowest rendered vertex after display transforms, used to rest merchandise on a surface. */
    public static float restingOffset(net.minecraft.client.resources.model.BakedModel model,
            ItemDisplayContext mode, float scale, boolean flat) {
        PoseStack transform = new PoseStack();
        transform.scale(scale, scale, scale);
        if (flat) {
            transform.mulPose(Axis.XP.rotationDegrees(90f));
            transform.mulPose(Axis.YP.rotationDegrees(180f));
        }
        model.getTransforms().getTransform(mode).apply(false, transform);
        transform.translate(-.5f, -.5f, -.5f);
        float minY = Float.POSITIVE_INFINITY;
        for (int side = -1; side < 6; side++) {
            for (var quad : model.getQuads(null, side < 0 ? null : net.minecraft.core.Direction.values()[side],
                    net.minecraft.util.RandomSource.create(42))) {
                int[] v = quad.getVertices();
                int stride = v.length / 4;
                for (int i = 0; i < 4; i++) {
                    var point = new org.joml.Vector3f(Float.intBitsToFloat(v[i * stride]),
                            Float.intBitsToFloat(v[i * stride + 1]), Float.intBitsToFloat(v[i * stride + 2]))
                            .mulPosition(transform.last().pose());
                    minY = Math.min(minY, point.y);
                }
            }
        }
        return Float.isFinite(minY) ? -minY : (flat ? .015f : scale / 4f);
    }

    public static int merchandiseCount(net.minecraft.world.item.ItemStack stack, int blockCap, int itemCap) {
        var item = stack.getItem();
        boolean equipment = item instanceof net.minecraft.world.item.TieredItem
                || item instanceof net.minecraft.world.item.ArmorItem || stack.getMaxStackSize() == 1;
        return Math.min(stack.getCount(), equipment ? 1 : item instanceof net.minecraft.world.item.BlockItem ? blockCap : itemCap);
    }

    /** Front faces negative z: positive local x is the viewer's left. */
    public static void renderPrice(PoseStack poses, MultiBufferSource buffers, int light, int overlay,
            RendererData data, BlockEntityRendererProvider.Context context, float textScale) {
        var font = context.getFont();
        var model = context.getItemRenderer().getModel(data.paymentItem(), null, null, 0);
        var bounds = iconBounds(model);
        float height = 7f * textScale;
        float iconScale = height / Math.max(bounds.width(), bounds.height());
        float iconWidth = bounds.width() * iconScale;
        float width = font.width(data.text()) * textScale;
        float gap = 4f * textScale;
        float right = (width + gap + iconWidth) / 2f;
        poses.pushPose();
        poses.translate(right, height / 2f, 0);
        poses.mulPose(Axis.ZP.rotationDegrees(180f));
        poses.scale(textScale, textScale, -textScale);
        font.drawInBatch(data.text(), 0, 0, 0xffffff, false,
                poses.last().pose(), buffers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, light);
        poses.popPose();
        poses.pushPose();
        poses.translate(right - width - gap - iconWidth / 2f, 0, -.001f);
        poses.mulPose(Axis.YP.rotationDegrees(180f));
        poses.scale(iconScale, iconScale, .001f);
        poses.translate(-bounds.x(), -bounds.y(), 0);
        context.getItemRenderer().renderStatic(data.paymentItem(), ItemDisplayContext.GUI,
                light, overlay, poses, buffers, null, 0);
        poses.popPose();
    }

    public static void renderShopWarns(float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers,
            int light,
            int overlay, final RendererData data, BlockEntityRendererProvider.Context context, float yOffset,
            Level level) {
        renderShopWarns(tickDelta, matrices, vertexConsumers, light, overlay, data, context, yOffset, 0.5f, level);
    }

    static void renderShopWarns(float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light,
            int overlay, final RendererData data, BlockEntityRendererProvider.Context context, float yOffset,
            float scale, Level level) {
        if (data.shopFunctional() && data.renderIcons()) {
            if (data.stockWarning || data.paymentWarning) {

                if (data.updateIconRotation()) {
                    data.targetRotation = calcTargetRotation(data);
                }

                data.frameRotation = (data.targetRotation - data.lastRotation);
                if (data.frameRotation > Math.PI) {
                    data.frameRotation -= Math.PI * 2;
                } else if (data.frameRotation < -Math.PI) {
                    data.frameRotation += Math.PI * 2;
                }

                data.frameRotation *= tickDelta * 0.08;

                data.lastRotation += data.frameRotation;

                if (data.lastRotation > Math.PI) {
                    data.lastRotation -= data.doublePi;
                } else if (data.lastRotation < -Math.PI) {
                    data.lastRotation += data.doublePi;
                }

                matrices.pushPose();
                matrices.translate(0.5f, 1.4f + yOffset, 0.5f);
                matrices.mulPose(Axis.YP.rotation((float) data.lastRotation));
                matrices.mulPose(Axis.YP.rotationDegrees(90.0f));
                matrices.scale(scale, scale, scale);

                if (data.stockWarning && data.paymentWarning) {

                    matrices.translate(0.5f, 0.0f, 0.0f);

                    context.getItemRenderer().renderStatic(NO_STOCK, ItemDisplayContext.GUI,
                            light,
                            overlay,
                            matrices,
                            vertexConsumers,
                            level,
                            0);
                    matrices.translate(-1.0f, 0.0f, 0.0f);
                    context.getItemRenderer().renderStatic(REG_FULL, ItemDisplayContext.GUI,
                            light,
                            overlay,
                            matrices,
                            vertexConsumers,
                            level,
                            0);
                    matrices.popPose();
                } else if (data.stockWarning) {
                    context.getItemRenderer().renderStatic(NO_STOCK, ItemDisplayContext.GUI,
                            light,
                            overlay,
                            matrices,
                            vertexConsumers,
                            level,
                            0);
                    matrices.popPose();
                } else {
                    context.getItemRenderer().renderStatic(REG_FULL, ItemDisplayContext.GUI,
                            light,
                            overlay,
                            matrices,
                            vertexConsumers,
                            level,
                            0);
                    matrices.popPose();
                }
            }
        }

    }

    public static double calcTargetRotation(RendererData data) {
        Player player1 = Minecraft.getInstance().player;
        if (player1 != null) {
            double px = player1.getX();
            double pz = player1.getZ();
            double dx = data.x();
            double dz = data.z();
            double x = px - (dx + 0.5f);
            double z = pz - (dz + 0.5f);
            return -atan2(z, x);
        }
        return data.targetRotation;
    }
}