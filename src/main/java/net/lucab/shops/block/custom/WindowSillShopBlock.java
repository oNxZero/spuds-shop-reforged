package net.lucab.shops.block.custom;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.lucab.shops.block.ModBlockEntities;
import net.lucab.shops.block.PostRegAssigner;
import net.lucab.shops.block.VariantResources;
import net.lucab.shops.block.entity.WindowSillShopEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

public class WindowSillShopBlock extends AbstractShopBlock {

    public static final MapCodec<WindowSillShopBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("stone_type").forGetter(b -> b.STONE_TYPE))
            .apply(instance, WindowSillShopBlock::new));

    public Item STONE_TYPE;

    private static final VoxelShape[] TRAY_SHAPES = new VoxelShape[4];
    static {
        for (int turn = 0; turn < 4; turn++) {
            VoxelShape shape = Shapes.empty();
            for (double[] box : new double[][]{
                    {1,0,4,15,2,6}, {1,0,6,15,4,8},
                    {1,0,8,15,6,10}, {1,0,10,15,8.5,12},
                    {4.25,.5,3.5,11.75,4.25,4.5}}) {
                double x1=8+(box[0]-8)*.7,z1=box[2],x2=8+(box[3]-8)*.7,z2=box[5];
                for (int i=0;i<turn;i++) {
                    double nx1=16-z2,nx2=16-z1;
                    z1=x1;z2=x2;x1=nx1;x2=nx2;
                }
                shape=Shapes.or(shape,createCuboidShape(x1,box[1]*.65,z1,x2,box[4]*.65,z2));
            }
            TRAY_SHAPES[turn]=shape;
        }
    }

    public WindowSillShopBlock(BlockBehaviour.Properties properties, Item stoneType) {
        super(properties);
        this.STONE_TYPE = stoneType;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindowSillShopEntity(pos, state);
    }

    @Override
    protected boolean onUseWithItem(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player) {
        Item item = stack.getItem();
        if (item != this.STONE_TYPE) {
            if (world.getBlockEntity(pos) instanceof WindowSillShopEntity shopEntity) {
                if (VariantResources.WINDOW_SILL.containsKey(item)) {
                    BlockState newSill = net.lucab.shops.block.VariantResources.WINDOW_SILL.get(item)
                            .defaultBlockState();

                    if (!player.isCreative()) {
                        stack.shrink(1);

                        world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5f, pos.getY() + 0.3f,
                                pos.getZ() + 0.5f, STONE_TYPE.getDefaultInstance(), 0f, 0.1f, 0f));
                    }

                    world.setBlockAndUpdate(pos, copyValues(newSill, state, FACING));
                    shopEntity.forceUpdateClient();
                    return true;

                }
            }
        }

        return false;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (ModBlockEntities.WINDOW_SHOP_ENTITY.get() == type) {
            return level.isClientSide
                    ? (lvl, pos, st, be) -> ((WindowSillShopEntity) be).renderTick()
                    : (lvl, pos, st, be) -> ((WindowSillShopEntity) be).serverTick((ServerLevel) lvl, pos, st);

        }
        return null;
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getGenericShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case EAST -> TRAY_SHAPES[1];
            case SOUTH -> TRAY_SHAPES[2];
            case WEST -> TRAY_SHAPES[3];
            default -> TRAY_SHAPES[0];
        };
    }

    @Override
    protected boolean isStateReplacedValid(BlockState newShopState) {
        return newShopState.getBlock() instanceof WindowSillShopBlock;
    }

    @Override
    public TagKey<Block> getPreferredTool() {
        return BlockTags.MINEABLE_WITH_PICKAXE;
    }
}
