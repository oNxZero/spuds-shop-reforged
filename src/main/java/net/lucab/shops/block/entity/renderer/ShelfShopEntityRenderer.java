package net.lucab.shops.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.lucab.shops.block.custom.ShelfShopBlock;
import net.lucab.shops.block.entity.ShelfShopEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

public class ShelfShopEntityRenderer implements BlockEntityRenderer<ShelfShopEntity> {

    private final BlockEntityRendererProvider.Context context;

    public ShelfShopEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.context = ctx;
    }

    @Override
    public void render(ShelfShopEntity shop, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers,
            int light, int overlay) {

        final ShelfShopEntity.RendererData data1 = shop.rendererData();
        final ShelfShopEntity.RendererData data2 = shop.rendererDataTop();

        BlockState state = shop.getBlockState();
        if (!(state.getBlock() instanceof ShelfShopBlock))
            return;
        SlabType type = state.getValue(ShelfShopBlock.SHELVES_ENABLED);

        // global rotation and translation
        matrices.pushPose();
        matrices.translate(0.5f, 0.5f, 0.5f);

        matrices.mulPose(Axis.YP.rotationDegrees(
                switch (shop.getCachedFacingDirection()) {
                    case EAST -> 270f;
                    case SOUTH -> 180f;
                    case WEST -> 90f;
                    default -> 0f;
                }));// was rotated around 0,0,0

        // render bottom
        if (type == SlabType.BOTTOM || type == SlabType.DOUBLE) {
            renderShelf(data1, tickDelta, matrices, vertexConsumers, light, overlay, shop.furtherDataBottom());
        }

        // render top
        matrices.translate(0f, 7f / 16f, 0f);
        if (type == SlabType.TOP || type == SlabType.DOUBLE) {
            renderShelf(data2, tickDelta, matrices, vertexConsumers, light, overlay, shop.furtherDataTop());
        }

        matrices.popPose();

        if (type == SlabType.BOTTOM || type == SlabType.DOUBLE) {
            ShopRenderUtils.renderShopWarns(tickDelta, matrices, vertexConsumers, light, overlay, data1, context,
                    -1.05f,
                    0.3f, shop.getLevel());
        }

        if (type == SlabType.TOP || type == SlabType.DOUBLE) {
            ShopRenderUtils.renderShopWarns(tickDelta, matrices, vertexConsumers, light, overlay, data2, context, -0.6f,
                    0.3f, shop.getLevel());
        }

    }

    private void renderShelf(ShelfShopEntity.RendererData data, float tickDelta, PoseStack matrices,
            MultiBufferSource vertexConsumers, int light, int overlay, ShelfShopEntity.ShelfRenderData furtherData) {
        if (data == null) return;
        data.frameAccumulator();
        if (!data.shopFunctional()) return;
        boolean block = data.stockDisplayType();
        int count = ShopRenderUtils.merchandiseCount(data.displayItem(), 3, 3);
        // A spaced row with small depth/angle variations; blocks never share a footprint.
        for (int i = 0; i < count; i++) {
            float x = (i - (count - 1) / 2f) * .27f;
            float z = count == 1 ? .30f : new float[]{.27f, .32f, .285f}[i];
            matrices.pushPose();
            matrices.translate(x, block ? -.20f : -.29f + i * .003f, z);
            if (block) {
                matrices.mulPose(Axis.YP.rotationDegrees(new float[]{-12, 8, -6, 15}[i % 4]));
                matrices.scale(.4f, .4f, .4f);
            } else {
                matrices.mulPose(Axis.XP.rotationDegrees(90f));
                matrices.mulPose(Axis.YP.rotationDegrees(180f));
                matrices.mulPose(Axis.ZP.rotationDegrees(new float[]{-12, 9, -5, 16}[i % 4]));
                matrices.scale(.36f, .36f, .36f);
            }
            context.getItemRenderer().renderStatic(data.displayItem(),
                    block ? ItemDisplayContext.FIXED : ItemDisplayContext.GUI,
                    light, overlay, matrices, vertexConsumers, null, 0);
            matrices.popPose();
        }
        renderPriceTag(data, matrices, vertexConsumers, light, overlay);
    }

    private void renderPriceTag(ShelfShopEntity.RendererData data, PoseStack matrices,
            MultiBufferSource vertexConsumers, int light, int overlay) {
        matrices.pushPose();
        matrices.translate(0f, -.5f, .014f);
        ShopRenderUtils.renderPrice(matrices, vertexConsumers, light, overlay, data, context, .012f);
        matrices.popPose();
    }
}
