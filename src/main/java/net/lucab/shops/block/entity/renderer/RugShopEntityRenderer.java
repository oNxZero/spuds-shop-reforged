package net.lucab.shops.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.lucab.shops.block.entity.RugShopEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

public class RugShopEntityRenderer implements BlockEntityRenderer<RugShopEntity> {
    private final BlockEntityRendererProvider.Context context;
    public RugShopEntityRenderer(BlockEntityRendererProvider.Context ctx) { context = ctx; }

    @Override
    public void render(RugShopEntity shop, float tickDelta, PoseStack matrices,
            MultiBufferSource buffers, int light, int overlay) {
        final var data = shop.rendererData();
        if (data == null) return;
        data.frameAccumulator();
        if (!data.shopFunctional()) return;

        // Merchandise and its price sign share the same local front/back direction.
        matrices.pushPose();
        matrices.translate(.5f, 0, .5f);
        matrices.mulPose(Axis.YP.rotationDegrees(switch (shop.getBlockState().getValue(
                net.lucab.shops.block.custom.RugShopBlock.FACING)) {
            case EAST -> 270f; case SOUTH -> 180f; case WEST -> 90f; default -> 0f;
        }));
        matrices.translate(-.5f, 0, -.5f);
        // Stable merchandise resting on the carpet; no bobbing or tumbling.
        for (int i = 0; i < ShopRenderUtils.merchandiseCount(data.displayItem(), 3, 3); i++) {
            matrices.pushPose();
            float x = data.displayItem().getCount() == 1 ? .5f : new float[]{.31f, .69f, .48f}[i];
            float z = data.displayItem().getCount() == 1 ? .5f : new float[]{.51f, .57f, .69f}[i];
            boolean block = data.stockDisplayType();
            matrices.translate(x, block ? .1505f : .077f, z);
            if (!block) {
                matrices.mulPose(Axis.XP.rotationDegrees(90f));
                matrices.mulPose(Axis.YP.rotationDegrees(180f));
            }
            if (!block) matrices.mulPose(Axis.ZP.rotationDegrees((i-1)*15f));
            if (block) matrices.mulPose(Axis.YP.rotationDegrees(new float[]{-23f, 17f, -8f}[i]));
            float scale = block ? .176f : .30f;
            matrices.scale(scale, scale, scale);
            context.getItemRenderer().renderStatic(data.displayItem(), block ? ItemDisplayContext.NONE : ItemDisplayContext.GUI,
                    light, overlay, matrices, buffers, null, 0);
            matrices.popPose();
        }

        matrices.popPose();

        matrices.pushPose();
        matrices.translate(.5f,0,.5f);
        matrices.mulPose(Axis.YP.rotationDegrees(switch (shop.getBlockState().getValue(
                net.lucab.shops.block.custom.RugShopBlock.FACING)) {
            case EAST -> 270f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            default -> 0f;
        }));
        matrices.translate(0f,.0625f,-.375f);
        matrices.mulPose(Axis.XP.rotationDegrees(22.5f));
        matrices.translate(0f, .09375f, -.002f);
        ShopRenderUtils.renderPrice(matrices, buffers, light, overlay, data, context, .011f);
        matrices.popPose();
        ShopRenderUtils.renderShopWarns(tickDelta, matrices, buffers, light, overlay,
                data, context, -.5f, shop.getLevel());
    }
}
