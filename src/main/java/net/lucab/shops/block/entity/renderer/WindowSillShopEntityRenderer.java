package net.lucab.shops.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.lucab.shops.block.entity.WindowSillShopEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;

public class WindowSillShopEntityRenderer implements BlockEntityRenderer<WindowSillShopEntity> {

    private final BlockEntityRendererProvider.Context context;

    public WindowSillShopEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.context = ctx;
    }

    @Override
    public void render(WindowSillShopEntity shop, float tickDelta, PoseStack matrices,
            MultiBufferSource vertexConsumers, int light, int overlay) {
        ItemDisplayContext mode;
        final WindowSillShopEntity.RendererData data = shop.rendererData();
        Font font = this.context.getFont();
        if (data == null) {
            return;
        }
        data.frameAccumulator();

        if (data.shopFunctional()) {

            boolean block = data.stockDisplayType();
            boolean equipment = data.displayItem().getMaxStackSize() == 1
                    || data.displayItem().getItem() instanceof net.minecraft.world.item.TieredItem
                    || data.displayItem().getItem() instanceof net.minecraft.world.item.ArmorItem;
            float scale = block ? .4f : equipment ? .4f : .30f;
            mode = block ? ItemDisplayContext.FIXED : ItemDisplayContext.GUI;
            var model = context.getItemRenderer().getModel(data.displayItem(), shop.getLevel(), null, 0);
            float y = .001f + ShopRenderUtils.restingOffset(model, mode, scale, !block);
            matrices.pushPose();
            matrices.translate(.5f, 0, .5f);
            matrices.mulPose(Axis.YP.rotationDegrees(switch (data.direction()) {
                case EAST -> 270f; case SOUTH -> 180f; case WEST -> 90f; default -> 0f;
            }));
            matrices.scale(.7f, .65f, 1f);
            matrices.translate(0, 1f / 16f, -3.5f / 16f);
            matrices.mulPose(Axis.XP.rotationDegrees(-45f));
            matrices.translate(0, y, (float)(3.5 * Math.sqrt(2)) / 16f);
            matrices.scale(scale, scale, scale);
            if (!block) {
                matrices.mulPose(Axis.XP.rotationDegrees(90f));
                matrices.mulPose(Axis.YP.rotationDegrees(180f));
            }

            this.context.getItemRenderer().renderStatic(
                    data.displayItem(),
                    mode,
                    light,
                    overlay,
                    matrices,
                    vertexConsumers,
                    null,
                    0);
            matrices.popPose();

            matrices.pushPose();
            matrices.translate(.5f, 0, .5f);
            matrices.mulPose(Axis.YP.rotationDegrees(switch (data.direction()) {
                case EAST -> 270f; case SOUTH -> 180f; case WEST -> 90f; default -> 0f;
            }));
            matrices.scale(.7f, .65f, 1f);
            matrices.translate(0, 1f / 16f, -4.4f / 16f);
            matrices.mulPose(Axis.XP.rotationDegrees(22.5f));
            matrices.translate(0, 1.5f / 16f, -.02f / 16f);
            ShopRenderUtils.renderPrice(matrices, vertexConsumers, light, overlay, data, context, .012f);
            matrices.popPose();

            ShopRenderUtils.renderShopWarns(tickDelta, matrices, vertexConsumers, light, overlay, data, context, -0.5f,
                    shop.getLevel());
        }
    }
}
