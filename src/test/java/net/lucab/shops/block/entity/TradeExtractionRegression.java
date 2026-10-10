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
        checkRepeatedPurchases();
        checkDurabilityMatching();
        System.out.println("Trade extraction regressions passed");
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
