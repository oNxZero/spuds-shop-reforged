package net.lucab.shops.block.custom;

import com.mojang.serialization.MapCodec;

import net.lucab.shops.block.ModBlockEntities;
import net.lucab.shops.block.VariantResources;
import net.lucab.shops.block.entity.CrateShopEntity;
import net.lucab.shops.screen.ScreenSettingsGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrateShopBlock extends AbstractShopBlock {

    public static final VoxelShape CULLING_SHAPE = createCuboidShape(0, -1.0, -1.0, 16.0, 2.0, 17.0);

    public static final VoxelShape SHAPE_NORTH = withStand(Shapes.or(
            createCuboidShape(1.0, 0.0, 0.0, 15.0, 8.0, 8.0),
            createCuboidShape(1.0, 4.0, 4.0, 15.0, 12.0, 12.0),
            createCuboidShape(1.0, 8.0, 8.0, 15.0, 16.0, 16.0)), 0);
    public static final VoxelShape SHAPE_WEST = withStand(Shapes.or(
            createCuboidShape(0.0, 0.0, 1.0, 8.0, 8.0, 15.0),
            createCuboidShape(4.0, 4.0, 1.0, 12.0, 12.0, 15.0),
            createCuboidShape(8.0, 8.0, 1.0, 16.0, 16.0, 15.0)), 1);
    public static final VoxelShape SHAPE_SOUTH = withStand(Shapes.or(
            createCuboidShape(1.0, 0.0, 8.0, 15.0, 8.0, 16.0),
            createCuboidShape(1.0, 4.0, 4.0, 15.0, 12.0, 12.0),
            createCuboidShape(1.0, 8.0, 0.0, 15.0, 16.0, 8.0)), 2);
    public static final VoxelShape SHAPE_EAST = withStand(Shapes.or(
            createCuboidShape(8.0, 0.0, 1.0, 16.0, 8.0, 15.0),
            createCuboidShape(4.0, 4.0, 1.0, 12.0, 12.0, 15.0),
            createCuboidShape(0.0, 8.0, 1.0, 8.0, 16.0, 15.0)), 3);

    private static VoxelShape withStand(VoxelShape crate, int turns) {
        double[][] supports = {
                {0.75, 0, 6.75, 2, 1.25, 14}, {14, 0, 6.75, 15.25, 1.25, 14},
                {0.75, 1.25, 12, 2, 6.75, 14}, {14, 1.25, 12, 15.25, 6.75, 14},
                {2, 0.25, 12, 14, 1.5, 14}
        };
        for (double[] box : supports) {
            double x1 = box[0], z1 = box[2], x2 = box[3], z2 = box[5];
            for (int turn = 0; turn < turns; turn++) {
                double nextX1 = z1, nextX2 = z2;
                z1 = 16 - x2;
                z2 = 16 - x1;
                x1 = nextX1;
                x2 = nextX2;
            }
            crate = Shapes.or(crate, createCuboidShape(x1, box[1], z1, x2, box[4], z2));
        }
        return crate;
    }

    public static final MapCodec<CrateShopBlock> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                    propertiesCodec(),
                    VariantResources.wood_variant.CODEC.optionalFieldOf("variant", VariantResources.wood_variant.OAK)
                            .forGetter(block -> block.variant))
                    .apply(instance, CrateShopBlock::new));

    public final VariantResources.wood_variant variant;

    public CrateShopBlock(BlockBehaviour.Properties properties, VariantResources.wood_variant variant) {
        super(properties);
        this.variant = variant;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public ScreenSettingsGroup getScreenSettings() {
        return ScreenSettingsGroup.createBasicWood(variant);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateShopEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (ModBlockEntities.CRATE_SHOP_ENTITY.get() == type) {
            return level.isClientSide
                    ? (lvl, pos, st, be) -> ((CrateShopEntity) be).renderTick()
                    : (lvl, pos, st, be) -> ((CrateShopEntity) be).serverTick((ServerLevel) lvl, pos, st);

        }
        return null;
    }

    @Override
    protected boolean isStateReplacedValid(BlockState newShopState) {
        return newShopState.getBlock() instanceof CrateShopBlock;
    }

    @Override
    protected boolean onUseWithItem(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getGenericShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }
}
