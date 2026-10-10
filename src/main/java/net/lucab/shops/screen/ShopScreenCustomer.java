package net.lucab.shops.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ShopScreenCustomer extends AbstractContainerScreen<ShopScreenHandlerCustomer> {

    protected int textureX = 0;
    protected int textureY = 0;

    private final ResourceLocation TEXTURE;

    public ShopScreenCustomer(ShopScreenHandlerCustomer handler, Inventory playerInventory, Component title) {
        super(handler, playerInventory, title);
        TEXTURE = handler.getSettings().CUSTOMER().textureID();
        imageWidth = 256;
        imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();

        leftPos = (width - imageWidth) / 2 + 42;
        topPos = (height - imageHeight) / 2 + 50;

        textureX = leftPos - 25;
        textureY = topPos - 90;
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawStockLabel(guiGraphics, menu.getAvailableStock(), 123, 60);
    }

    private void drawStockLabel(GuiGraphics graphics, int stock, int centerX, int y) {
        Component text = Component.translatable("gui.spudaciousshops.stock", stock < 0 ? "∞" : stock);
        graphics.drawString(font, text, centerX - font.width(text) / 2, y,
                stock == 0 ? 0x8B2820 : 0x3F2C13, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, textureX, textureY, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
