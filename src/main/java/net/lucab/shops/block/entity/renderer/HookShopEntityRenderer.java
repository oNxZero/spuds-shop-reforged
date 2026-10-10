package net.lucab.shops.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.lucab.shops.block.entity.HookShopEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

public class HookShopEntityRenderer implements BlockEntityRenderer<HookShopEntity> {

    private final BlockEntityRendererProvider.Context context;

    public HookShopEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.context = ctx;
    }

    @Override
    public void render(HookShopEntity shop, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers,
            int light, int overlay) {
        ItemDisplayContext mode;
        final HookShopEntity.RendererData data = shop.rendererData();
        Font font = this.context.getFont();
        if (data == null) {
            return;
        }
        data.frameAccumulator();

        if (data.shopFunctional()) {

            // global rotation and translation
            matrices.pushPose();
            matrices.translate(0.5f, 0.5f, 0.5f);
            matrices.mulPose(Axis.YP.rotationDegrees(
                    switch (data.direction()) {
                        case EAST -> 270f;
                        case SOUTH -> 180f;
                        case WEST -> 90f;
                        default -> 0f;
                    }));// used to have centre 0,0,0

            // render item being sold
            matrices.pushPose();

            if (data.stockDisplayType()) {
                matrices.translate(0f, -0.25f, 0f);
                matrices.mulPose(Axis.YP.rotationDegrees(180.0f));
                matrices.scale(0.5f, 0.5f, 0.5f);
                mode = ItemDisplayContext.FIXED;
            } else {
                matrices.translate(0f, -0.5f, 0f);
                matrices.scale(0.7f, 0.7f, 0.7f);
                matrices.mulPose(Axis.YP.rotationDegrees(180f));
                matrices.mulPose(Axis.ZP.rotationDegrees(45.0f));
                mode = ItemDisplayContext.GUI;
            }

            this.context.getItemRenderer().renderStatic(data.displayItem(), mode,
                    light,
                    overlay,
                    matrices,
                    vertexConsumers,
                    null,
                    0);
            matrices.popPose();

            for (int side = 0; side < 2; side++) {
                matrices.pushPose();
                matrices.mulPose(Axis.YP.rotationDegrees(side * 180f));
                matrices.translate(0f, .1875f, -.032f);
                ShopRenderUtils.renderPrice(matrices, vertexConsumers, light, overlay, data, context, .016f);
                matrices.popPose();
            }
            matrices.popPose();
            ShopRenderUtils.renderShopWarns(tickDelta, matrices, vertexConsumers, light, overlay, data, context, -2.1f,
                    shop.getLevel());
        }
    }
}
