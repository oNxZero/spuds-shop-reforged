package net.lucab.shops.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.lucab.shops.block.entity.AngledShopEntity;
import net.lucab.shops.util.CushionModel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;

public class AngledShopBlockRenderer implements BlockEntityRenderer<AngledShopEntity> {

    private final BlockEntityRendererProvider.Context context;

    private final CushionModel model;

    public AngledShopBlockRenderer(BlockEntityRendererProvider.Context ctx) {
        this.context = ctx;
        this.model = new CushionModel(ctx.bakeLayer(CushionModel.LAYER_LOCATION));
    }

    @Override
    public void render(AngledShopEntity shop, float tickDelta, PoseStack poseStack, MultiBufferSource bufferSource,
            int light, int overlay) {
        ItemDisplayContext mode;
        final AngledShopEntity.RendererData data = shop.rendererData();
        Font font = this.context.getFont();

        poseStack.pushPose();
        poseStack.translate(0.5f, 0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                switch (shop.getCachedFacingDirection()) {
                    case EAST -> 270f;
                    case SOUTH -> 180f;
                    case WEST -> 90f;
                    default -> 0f;
                }));
        poseStack.translate(-0.5f, 0f, -0.5f);

        this.model.renderToBuffer(
                poseStack,
                bufferSource.getBuffer(RenderType.entitySolid(shop.getCushionTextureID())),
                light,
                overlay,
                0xFFFFFFFF);

        poseStack.popPose();

        if (data == null) {
            return;
        }
        data.frameAccumulator();
        if (data.shopFunctional()) {

            // render item being sold
            poseStack.pushPose();
            poseStack.translate(0.5f, 1.02f, 0.5f);

            poseStack.mulPose(Axis.YP.rotationDegrees(data.rotation()));
            poseStack.mulPose(Axis.XP.rotationDegrees(-67.5f));

            if (data.stockDisplayType()) {
                poseStack.scale(0.3f, 0.3f, 0.3f);
                mode = ItemDisplayContext.FIXED;
            } else {
                poseStack.scale(0.4f, 0.4f, 0.4f);
                mode = ItemDisplayContext.GUI;
            }
            this.context.getItemRenderer().renderStatic(
                    data.displayItem(),
                    mode,
                    light,
                    overlay,
                    poseStack,
                    bufferSource,
                    null,
                    0);

            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(.5f, 0, .5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(switch (data.direction()) {
                case EAST -> 270f; case SOUTH -> 180f; case WEST -> 90f; default -> 0f;
            }));
            poseStack.translate(0, 6f / 16f, -.4375f);
            poseStack.mulPose(Axis.XP.rotationDegrees(22.5f));
            poseStack.translate(0, 2f / 16f, -1.02f / 16f);
            ShopRenderUtils.renderPrice(poseStack, bufferSource, light, overlay, data, context, .018f);
            poseStack.popPose();

            ShopRenderUtils.renderShopWarns(tickDelta, poseStack, bufferSource, light, overlay, data, context, 0.375f,
                    shop.getLevel());

        }
    }

}