package net.lucab.shops;

import com.mojang.logging.LogUtils;

import net.lucab.shops.block.*;
import net.lucab.shops.block.entity.renderer.ShopIconModels;
import net.lucab.shops.config.ConfigHandler;
import net.lucab.shops.item.ModItemGroups;
import net.lucab.shops.item.ModItems;
import net.lucab.shops.lootcondition.ModLootConditions;
import net.lucab.shops.properties.ModProperties;
import net.lucab.shops.screen.ModScreenHandlers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import static net.lucab.shops.block.ModBlocks.postRegistryTasks;

import org.slf4j.Logger;

//import net.spudacious5705.shops.command.DebugShopsStatesCommand;

@Mod("spudaciousshops")
public class SpudaciousShops {
    public static final String MOD_ID = "spudaciousshops";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpudaciousShops(IEventBus modEventBus) {

        ConfigHandler.initialise();

        ModItems.registerModItems(modEventBus);
        ModBlocks.registerModBlocks(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::enqueueCompatibility);

        ModBlockTags.initialise();
        ModProperties.registerModProperties();

        ModBlockEntities.registerBlockEntities(modEventBus);
        ModScreenHandlers.registerScreenHandlers(modEventBus);

        ModLootConditions.register(modEventBus);

        ModItemGroups.register(modEventBus);

        // DebugShopsStatesCommand.register(); //for DEBUG purposes only

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void enqueueCompatibility(final InterModEnqueueEvent event) {
        InterModComms.sendTo("invtweaks", "blacklist-screen",
                () -> "net.lucab.shops.screen.ShopScreenOwner");
        InterModComms.sendTo("invtweaks", "blacklist-screen",
                () -> "net.lucab.shops.screen.ShopScreenCustomer");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("SETTING UP SPUDS SHOP REFORGED...");

        postRegistryTasks.forEach(Runnable::run);
        PostRegAssigner.runAllAssigners();
        ShopIconModels.initialise();
        VariantResources.register();
        // Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    private final java.util.Map<net.minecraft.world.entity.player.Player, Integer> lastQuickBuy =
            new java.util.WeakHashMap<>();

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public void onQuickBuy(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        var player = event.getEntity();
        if (!player.isShiftKeyDown() || player.isSpectator()
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof
                net.lucab.shops.block.entity.AbstractShopEntity shop)
                || shop.quickUserSignIn(player) != net.lucab.shops.properties.PermissionLevel.CUSTOMER) {
            return;
        }
        // Cancel mining on both sides; the normal START packet still reaches the server.
        event.setCanceled(true);
        if (event.getLevel().isClientSide()
                && event.getAction() == net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START) {
            // Track the held mouse button locally so it cannot send repeated START purchases.
            event.setUseItem(net.neoforged.neoforge.common.util.TriState.DEFAULT);
        }
        if (event.getLevel().isClientSide()
                || event.getAction() != net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START
                || !player.canInteractWithBlock(event.getPos(), 0)
                || !event.getLevel().mayInteract(player, event.getPos())) {
            return;
        }
        var hit = player.pick(player.blockInteractionRange(), 1, false);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)
                || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                || !blockHit.getBlockPos().equals(event.getPos())) return;
        Integer previous = lastQuickBuy.get(player);
        if (previous != null && player.tickCount - previous < 5) return;
        lastQuickBuy.put(player, player.tickCount);
        boolean top = false;
        var state = shop.getBlockState();
        if (state.hasProperty(net.lucab.shops.block.custom.ShelfShopBlock.SHELVES_ENABLED)) {
            var shelves = state.getValue(net.lucab.shops.block.custom.ShelfShopBlock.SHELVES_ENABLED);
            top = shelves == net.minecraft.world.level.block.state.properties.SlabType.TOP
                    || (shelves == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE
                    && blockHit.getLocation().y - event.getPos().getY() > 0.5);
        }
        var inventory = top ? shop.getOtherInventoryDelegate(player) : shop.getInventoryDelegate(player);
        if (inventory != null) inventory.trade(player.getInventory());
        player.containerMenu.broadcastChanges();
    }

    public static ResourceLocation getResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

}
