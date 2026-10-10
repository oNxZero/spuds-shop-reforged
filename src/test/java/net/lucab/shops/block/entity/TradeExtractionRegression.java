package net.lucab.shops.block.entity;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** Exercises the actual extraction code used for both stock and payment. */
public final class TradeExtractionRegression {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        checkTogglePackets();
        check(Items.ENDER_PEARL, 48, 16, 16, 16);
        check(Items.DIAMOND_SWORD, 4, 1, 1, 1, 1);
        check(Items.DIAMOND, 64, 10, 20, 34);
        check(Items.DIAMOND, 17, 10, 20, 34);
        check(Items.DIAMOND, 16, 16, 16);

        ItemStack named = new ItemStack(Items.DIAMOND, 20);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep me"));
        List<ItemStack> mixed = new ArrayList<>(List.of(named, new ItemStack(Items.DIAMOND, 10)));
        var result = AbstractShopEntity.InventoryDelegate.takeItems(
                new ItemStack(Items.DIAMOND, 7), mixed::get, 0, 1);
        require(named.getCount() == 20, "different components must remain untouched");
        require(result.stream().mapToInt(ItemStack::getCount).sum() == 7, "matching extraction");
        require(mixed.get(1).getCount() == 3, "matching remainder");
        checkTradeStackLimits();
        checkRepeatedPurchases();
        checkDurabilityMatching();
        System.out.println("Trade extraction regressions passed");
    }

    private static void checkTradeStackLimits() {
        ShopInventory shop = ShopInventory.create();
        for (int slot : new int[]{ShopInventory.VENDING_SLOT, ShopInventory.PAYMENT_SLOT}) {
            for (Item item : new Item[]{Items.ENDER_PEARL, Items.DIAMOND_SWORD, Items.DIAMOND}) {
                ItemStack input = new ItemStack(item, 64);
                shop.set(slot, input);
                require(shop.get(slot).getCount() == input.getMaxStackSize(), "trade respects item stack limit");
                require(input.getCount() == 64, "limiting trade does not consume cursor items");
            }
            shop.set(slot, new ItemStack(Items.ENDER_PEARL, 7));
            require(shop.get(slot).getCount() == 7, "valid trade quantities remain unchanged");
            ItemStack custom = new ItemStack(Items.DIAMOND, 64);
            custom.set(DataComponents.MAX_STACK_SIZE, 8);
            shop.set(slot, custom);
            require(shop.get(slot).getCount() == 8, "component stack limits are respected");
        }
        shop.set(0, new ItemStack(Items.ENDER_PEARL, 48));
        require(shop.get(0).getCount() == 48, "trade limits must not truncate stored stock");
    }

    private static void checkDurabilityMatching() {
        ItemStack template = new ItemStack(Items.SHEARS);
        template.setDamageValue(20);
        ItemStack worn = new ItemStack(Items.SHEARS);
        worn.setDamageValue(150);
        ItemStack fresh = new ItemStack(Items.SHEARS);
        require(net.lucab.shops.screen.ShopScreenHandlerOwner.canUseInTrade(template, worn),
                "worn shears must match the template");
        require(net.lucab.shops.screen.ShopScreenHandlerOwner.canUseInTrade(template, fresh),
                "fresh shears must match the template");
        ItemStack named = worn.copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Special shears"));
        require(!net.lucab.shops.screen.ShopScreenHandlerOwner.canUseInTrade(template, named),
                "ignoring durability must preserve other component matching");
        ShopInventory shop = ShopInventory.create();
        shop.set(ShopInventory.VENDING_SLOT, template.copy());
        shop.set(0, worn);
        shop.set(1, named);
        require(shop.availableStock() == 1, "display must count worn stock but exclude nonmatching names");
        require(!shop.outOfStock(), "stock validation must ignore durability");
        var extracted = AbstractShopEntity.InventoryDelegate.takeItems(template, shop::get, 0, ShopInventory.STOCK_END);
        require(extracted.size() == 1 && extracted.getFirst().getDamageValue() == 150,
                "purchases must deliver actual stock durability");
        require(shop.get(0).isEmpty() && shop.outOfStock(), "stock must be consumed exactly once");
        require(shop.availableStock() == 0, "display must update after stock extraction");
        require(template.getDamageValue() == 20, "matching must not modify template durability");
    }

    private static void checkRepeatedPurchases() {
        ShopInventory shop = ShopInventory.create();
        shop.set(ShopInventory.VENDING_SLOT, new ItemStack(Items.DIAMOND, 3));
        shop.set(ShopInventory.PAYMENT_SLOT, new ItemStack(Items.EMERALD, 2));
        shop.set(0, new ItemStack(Items.DIAMOND, 4));
        shop.set(1, new ItemStack(Items.DIAMOND, 3));
        require(shop.availableStock() == 7, "stock display must count items across stacks");
        int delivered = 0;
        while (!shop.outOfStock()) {
            var result = AbstractShopEntity.InventoryDelegate.takeItems(
                    shop.getVendingStack(), shop::get, 0, ShopInventory.STOCK_END);
            int quantity = result.stream().mapToInt(ItemStack::getCount).sum();
            require(quantity == 3, "each repeat purchase must deliver the configured quantity");
            delivered += quantity;
        }
        require(delivered == 6 && shop.get(1).getCount() == 1,
                "repeated purchases must stop before consuming partial stock");
        require(shop.availableStock() == 1, "stock display must retain partial trade leftovers");
        for (int slot = ShopInventory.STOCK_END + 1; slot <= ShopInventory.PROFIT_END; slot++) {
            shop.set(slot, new ItemStack(Items.EMERALD, 64));
        }
        require(shop.paymentRegisterFull(), "full registers must block purchases");
        shop.get(ShopInventory.PROFIT_END).setCount(63);
        require(shop.paymentRegisterFull(), "partial register capacity must block purchases");
        shop.get(ShopInventory.PROFIT_END).setCount(62);
        require(!shop.paymentRegisterFull(), "exact register capacity must allow purchases");
    }

    private static void checkTogglePackets() {
        for (var id : net.lucab.shops.screen.ToggleButtonID.values()) {
            var buffer = io.netty.buffer.Unpooled.buffer();
            try {
                var request = new net.lucab.shops.screen.networking.ToggleSyncPkt(id, true);
                net.lucab.shops.screen.networking.ToggleSyncPkt.STREAM_CODEC.encode(buffer, request);
                require(request.equals(net.lucab.shops.screen.networking.ToggleSyncPkt.STREAM_CODEC.decode(buffer)),
                        "toggle request round trip");
                buffer.clear();
                var response = new net.lucab.shops.screen.networking.ToggleSyncResponsePkt(id, false);
                net.lucab.shops.screen.networking.ToggleSyncResponsePkt.STREAM_CODEC.encode(buffer, response);
                require(response.equals(net.lucab.shops.screen.networking.ToggleSyncResponsePkt.STREAM_CODEC.decode(buffer)),
                        "toggle response round trip");
            } finally {
                buffer.release();
            }
        }
        for (int id : new int[] {-1, net.lucab.shops.screen.ToggleButtonID.values().length,
                Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            for (boolean response : new boolean[] {false, true}) {
                var buffer = io.netty.buffer.Unpooled.buffer();
                try {
                    buffer.writeInt(id).writeBoolean(true);
                    boolean rejected = false;
                    try {
                        if (response) {
                            net.lucab.shops.screen.networking.ToggleSyncResponsePkt.STREAM_CODEC.decode(buffer);
                        } else {
                            net.lucab.shops.screen.networking.ToggleSyncPkt.STREAM_CODEC.decode(buffer);
                        }
                    } catch (io.netty.handler.codec.DecoderException expected) {
                        rejected = true;
                    }
                    require(rejected, "invalid toggle ID must be rejected: " + id);
                } finally {
                    buffer.release();
                }
            }
        }
    }

    private static void check(Item item, int requested, int... counts) {
        List<ItemStack> inventory = new ArrayList<>();
        int before = 0;
        for (int count : counts) {
            inventory.add(new ItemStack(item, count));
            before += count;
        }
        var result = AbstractShopEntity.InventoryDelegate.takeItems(
                new ItemStack(item, requested), inventory::get, 0, counts.length - 1);
        int extracted = result.stream().mapToInt(ItemStack::getCount).sum();
        int remaining = inventory.stream().mapToInt(ItemStack::getCount).sum();
        require(extracted == requested, "exact requested quantity: " + item);
        require(remaining + extracted == before, "item conservation: " + item);
        require(result.stream().allMatch(s -> s.getCount() <= s.getMaxStackSize()), "legal stack sizes");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
