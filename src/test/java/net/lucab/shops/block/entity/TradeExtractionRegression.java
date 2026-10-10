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
        System.out.println("Trade extraction regressions passed");
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
