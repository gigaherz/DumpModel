package gigaherz.dumpmodel;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gigaherz.dumpmodel.builders.DumperFactory;
import gigaherz.dumpmodel.builders.ModelBuilderBase;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class DumpCommand
{
    public static DumperFactory<?> factory = DumperFactory.OBJ;

    public static void init(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext)
    {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("dumpmodel")
                        .then(Commands.literal("atlas")
                                .executes((ctx) -> dumpBlockAtlas())
                        )
                        .then(Commands.literal("format")
                                .then(Commands.literal("obj")
                                        .executes((ctx) -> { factory = DumperFactory.OBJ; return 0; })
                                )
                                .then(Commands.literal("usd")
                                        .executes((ctx) -> { factory = DumperFactory.USD; return 0; })
                                )
                        )
                        .then(Commands.literal("held")
                                .executes((ctx) -> dumpHeldItem(InteractionHand.MAIN_HAND))
                                .then(Commands.literal("mainhand")
                                        .executes(ctx -> dumpHeldItem(InteractionHand.MAIN_HAND)))
                                .then(Commands.literal("offhand")
                                        .executes(ctx -> dumpHeldItem(InteractionHand.OFF_HAND)))
                        )
                        .then(Commands.literal("target")
                                .executes((ctx) -> dumpTargettedBlock()))
                        .then(Commands.literal("item")
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes((ctx) ->
                                                dumpItemModel(ItemArgument.getItem(ctx, "item").createItemStack(1, false)))))
                        .then(Commands.literal("block")
                                .then(Commands.argument("block", BlockStateArgument.block(buildContext))
                                        .executes((ctx) -> dumpBlockModel(BlockStateArgument.getBlock(ctx, "block").getState(), null))))
                        .then(Commands.literal("entity")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes((ctx) -> dumpEntityRenderer(EntityArgument.getEntity(ctx, "entity")))))
                        .then(Commands.literal("scene")
                                .executes(ctx -> dumpScene(Minecraft.getInstance().player))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> dumpScene(BlockPosArgument.getLoadedBlockPos(ctx, "pos")))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                                .executes(ctx -> dumpScene(BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                                                        IntegerArgumentType.getInteger(ctx, "radius")))
                                        )
                                )
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                        .executes(ctx -> dumpScene(Minecraft.getInstance().player,
                                                IntegerArgumentType.getInteger(ctx, "radius")))
                                )
                        )
        );
    }

    private static int dumpBlockAtlas()
    {
        Minecraft mc = Minecraft.getInstance();
        Path folder = FMLPaths.GAMEDIR.get().resolve("models");
        Path file = folder.resolve("atlas.png");

        Utils.dumpTexture(TextureAtlas.LOCATION_BLOCKS, file);

        showSuccessMessage(mc, folder, file, "Texture");
        return 1;
    }

    private static int dumpHeldItem(InteractionHand hand)
    {
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = Objects.requireNonNull(mc.player).getItemInHand(hand);
        if (held.getCount() <= 0)
        {
            mc.gui.getChat().addMessage(Component.literal("You must be holding an item in your " + hand + " to use this command."));
            return 0;
        }

        return dumpItemModel(held);
    }

    private static int dumpTargettedBlock()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult == null || (mc.hitResult.getType() != HitResult.Type.BLOCK && mc.hitResult.getType() != HitResult.Type.ENTITY))
        {
            mc.gui.getChat().addMessage(Component.literal("You must be looking at a block or entity to use the 'target' subcommand."));
            return 0;
        }

        if (mc.hitResult.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult br = (BlockHitResult) mc.hitResult;
            BlockState state = Objects.requireNonNull(mc.level).getBlockState(br.getBlockPos());
            return dumpBlockModel(state, br.getBlockPos());
        }
        else if (mc.hitResult.getType() == HitResult.Type.ENTITY)
        {
            EntityHitResult br = (EntityHitResult) mc.hitResult;
            return dumpEntityRenderer(br.getEntity());
        }

        return 0;
    }

    private static int dumpItemModel(ItemStack stack)
    {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
        ResourceLocation regName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (regName == null)
            throw new RuntimeException("Item registry name is null");
        Path folder = FMLPaths.GAMEDIR.get()
                .resolve("models/items")
                .resolve(regName.getNamespace());
        Path file;
        if (stack.hasTag())
        {
            folder = folder.resolve(regName.getPath());
            file = folder.resolve(Objects.requireNonNull(stack.getTag()).hashCode() + factory.extension());
        }
        else
        {
            file = folder.resolve(regName.getPath() + factory.extension());
        }

        if (model.isCustomRenderer())
        {
            VertexDumper dumper = new VertexDumper(factory.create());
            IClientItemExtensions.of(stack.getItem()).getCustomRenderer()
                    .renderByItem(stack, ItemTransforms.TransformType.FIXED, new PoseStack(), dumper, 0x00F000F0, OverlayTexture.NO_OVERLAY);

            return dumpVertexDumper(mc, dumper, folder, file);
        }
        else
        {
            return dumpBakedModel(mc, model, folder, file);
        }
    }

    private static int dumpBlockModel(BlockState state, @Nullable BlockPos pos)
    {
        Minecraft mc = Minecraft.getInstance();

        var data = ModelData.EMPTY;
        if (pos != null)
        {
            data = Objects.requireNonNull(Objects.requireNonNull(mc.level).getModelDataManager()).getAt(pos);
            if (data == null) data = ModelData.EMPTY;
        }

        ResourceLocation regName = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (regName == null)
            throw new RuntimeException("Block registry name is null");
        Path folder = FMLPaths.GAMEDIR.get()
                .resolve("models/blocks")
                .resolve(regName.getNamespace());
        Path file = folder.resolve(regName.getPath() + factory.extension());

        VertexDumper dumper = new VertexDumper(factory.create());
        BakedModel model = mc.getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        RenderShape renderShape = state.getRenderShape();
        boolean isCustomRenderer = model.isCustomRenderer();
        switch (renderShape)
        {
            case MODEL:
                if (!isCustomRenderer)
                {
                    for(var rt : RenderType.chunkBufferLayers())
                    {
                        mc.getBlockRenderer().renderSingleBlock(state, new PoseStack(), dumper, 0x00F000F0, OverlayTexture.NO_OVERLAY, data, rt);
                    }
                }
                // fallthrough;
            case ENTITYBLOCK_ANIMATED:
                if (pos != null)
                {
                    BlockEntity te = mc.level.getBlockEntity(pos);
                    if (te != null)
                    {
                        BlockEntityRenderer<BlockEntity> ter = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(te);
                        if (ter == null)
                        {
                            if (renderShape == RenderShape.ENTITYBLOCK_ANIMATED)
                            {
                                mc.gui.getChat().addMessage(Component.literal("The block needs a builtin renderer but there is no TileEntity Renderer."));
                                return 0;
                            }
                        }
                        else
                        {
                            ter.render(te, mc.getFrameTime(), new PoseStack(), dumper, 0x00F000F0, OverlayTexture.NO_OVERLAY);
                        }
                    }
                    else if (isCustomRenderer)
                    {
                        if (renderShape == RenderShape.ENTITYBLOCK_ANIMATED)
                        {
                            mc.gui.getChat().addMessage(Component.literal("The block needs a builtin renderer but there is no TileEntity."));
                            return 0;
                        }
                    }
                }
                else if (isCustomRenderer)
                {
                    if (renderShape == RenderShape.ENTITYBLOCK_ANIMATED)
                    {
                        mc.gui.getChat().addMessage(Component.literal("The block needs a builtin renderer but I have no BlockPos context."));
                        return 0;
                    }
                }
                break;
        }
        return dumpVertexDumper(mc, dumper, folder, file);
    }

    private static int dumpEntityRenderer(Entity entity)
    {
        try
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null)
                return 0;

            VertexDumper dumper = new VertexDumper(factory.create());

            if (entity instanceof EnderDragonPart dragonPart)
            {
                EnderDragon dragon = dragonPart.parentMob;

                EntityRenderer<? super EnderDragon> renderer = mc.getEntityRenderDispatcher().getRenderer(dragon);

                renderer.render(dragon, 0, mc.getFrameTime(), new PoseStack(), dumper, 0x00F000F0);
            }
            else
            {
                @SuppressWarnings("rawtypes")
                EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);

                //noinspection unchecked
                renderer.render(entity, 0, mc.getFrameTime(), new PoseStack(), dumper, 0x00F000F0);
            }

            ResourceLocation regName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (regName == null)
                throw new RuntimeException("EntityType registry name is null");
            Path folder = FMLPaths.GAMEDIR.get()
                    .resolve("models/entities")
                    .resolve(regName.getNamespace());
            Path file = folder.resolve(regName.getPath() + factory.extension());

            return dumpVertexDumper(mc, dumper, folder, file);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Error rendering entity to dump: ", e);
        }
    }

    private static int dumpScene(BlockPos pos)
    {
        return dumpScene(pos, 50);
    }

    private static int dumpScene(BlockPos pos, int radius)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return 0;

        AABB aabb = AABB.ofSize(new Vec3(pos.getX(), pos.getY(), pos.getZ()), radius, radius, radius);

        return dumpScene(aabb);
    }

    private static int dumpScene(Entity executer)
    {
        return dumpScene(executer, 50);
    }

    private static int dumpScene(Entity executer, int radius)
    {
        return dumpScene(executer.position(), radius);
    }

    private static int dumpScene(Vec3 pos, int radius)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return 0;

        var aabb = AABB.ofSize(pos, radius, radius, radius);

        return dumpScene(aabb);
    }

    private static int dumpScene(AABB aabb)
    {
        Path folder, outPath;
        var timestamp = System.currentTimeMillis();
        do
        {
            folder = FMLPaths.GAMEDIR.get()
                    .resolve("models/scenes");
            //noinspection ResultOfMethodCallIgnored
            folder.toFile().mkdirs();
            outPath = folder.resolve("scene_" + timestamp + factory.extension());
            timestamp++;
        } while (Files.exists(outPath));

        try
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null)
                return 0;

            var entities = mc.level.getEntitiesOfClass(Entity.class, aabb);

            var minP = new BlockPos(aabb.minX, aabb.minY, aabb.minZ);
            var maxP = new BlockPos(Mth.ceil(aabb.maxX), Mth.ceil(aabb.maxY), Mth.ceil(aabb.maxZ));

            var builder = factory.create();

            var posestack = new PoseStack();

            var dumper0 = new VertexDumper(builder, true);
            dumper0.setOrigin(minP);

            var outFile = outPath;
            var textures = new HashMap<ResourceLocation, String>();
            Function<ResourceLocation, String> textureDumper = tx ->
                    textures.computeIfAbsent(tx, tex -> Utils.dumpTexture(outFile, tx).getAbsolutePath());

            BlockAndTintGetter level = new LimitedWrapper(mc.level, minP, maxP);
            Set<BlockEntity> blockEntities = Sets.newHashSet();
            VisGraph visgraph = new VisGraph();
            RandomSource random = RandomSource.create();
            BlockRenderDispatcher blockrenderdispatcher = Minecraft.getInstance().getBlockRenderer();
            for (var blockpos2 : BlockPos.betweenClosed(minP, maxP))
            {
                BlockState blockstate = level.getBlockState(blockpos2);
                if (blockstate.isSolidRender(level, blockpos2))
                {
                    visgraph.setOpaque(blockpos2);
                }

                if (blockstate.hasBlockEntity())
                {
                    BlockEntity blockentity = level.getBlockEntity(blockpos2);
                    if (blockentity != null)
                    {
                        blockEntities.add(blockentity);
                    }
                }

                BlockState blockstate1 = level.getBlockState(blockpos2);
                FluidState fluidstate = blockstate1.getFluidState();
                ModelData modelData = Objects.requireNonNull(Objects.requireNonNull(mc.level).getModelDataManager()).getAt(blockpos2);;
                if (modelData == null) modelData = ModelData.EMPTY;
                if (!fluidstate.isEmpty())
                {
                    posestack.pushPose();
                    posestack.translate(blockpos2.getX() - (blockpos2.getX()&15), blockpos2.getY() - (blockpos2.getY()&15), blockpos2.getZ() - (blockpos2.getZ()&15));
                    var consumer = new TransformingConsumer(dumper0.getBuffer(ItemBlockRenderTypes.getRenderLayer(fluidstate)), posestack.last().pose(), posestack.last().normal());
                    blockrenderdispatcher.renderLiquid(blockpos2, level, consumer, blockstate1, fluidstate);
                    posestack.popPose();
                }

                if (blockstate.getRenderShape() != RenderShape.INVISIBLE)
                {
                    for (RenderType rendertype : RenderType.chunkBufferLayers())
                    {
                        posestack.pushPose();
                        posestack.translate(blockpos2.getX(), blockpos2.getY(), blockpos2.getZ());
                        blockrenderdispatcher.renderBatched(blockstate, blockpos2, level, posestack, dumper0.getBuffer(rendertype), true, random, modelData, rendertype);
                        posestack.popPose();
                    }
                }
            }
            dumper0.finish(textureDumper, "terrain");

            for (var entity : entities)
            {
                var dumper = new VertexDumper(builder);
                dumper.setOrigin(minP);

                posestack.pushPose();
                posestack.translate(entity.getX(), entity.getY(), entity.getZ());

                if (!(entity instanceof PartEntity<?>))
                {
                    var renderer = mc.getEntityRenderDispatcher().getRenderer(entity);

                    renderer.render(entity, 0, mc.getFrameTime(), posestack, dumper, 0x00F000F0);
                }

                dumper.finish(textureDumper, String.format("entity_%s", entity.getId()));

                posestack.popPose();
            }

            for (var be : blockEntities)
            {
                var dumper = new VertexDumper(builder);
                dumper.setOrigin(minP);

                var pos = be.getBlockPos();

                posestack.pushPose();
                posestack.translate(pos.getX(), pos.getY(), pos.getZ());

                var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(be);
                if (renderer != null)
                {
                    renderer.render(be, 0, posestack, dumper, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

                    dumper.finish(textureDumper, String.format("be_%s_%s_%s", pos.getX(), pos.getY(), pos.getZ()));
                }
                posestack.popPose();
            }

            return dumpBuilder(mc, builder, folder, outPath);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Error rendering entity to dump: ", e);
        }
    }

    private static int dumpVertexDumper(Minecraft mc, VertexDumper dumper, Path folder, Path file)
    {
        var textures = new HashMap<ResourceLocation, String>();
        Function<ResourceLocation, String> textureDumper = tx ->
                textures.computeIfAbsent(tx, tex -> Utils.dumpTexture(file, tx).getAbsolutePath());
        dumper.finish(textureDumper, "object1");

        return dumpBuilder(mc, dumper.builder, folder, file);
    }

    private static int dumpBuilder(Minecraft mc, ModelBuilderBase<?> builder, Path folder, Path file)
    {
        //noinspection ResultOfMethodCallIgnored
        folder.toFile().mkdirs();

        builder.save(file);

        showSuccessMessage(mc, folder, file);
        return 1;
    }

    private static int dumpBakedModel(Minecraft mc, BakedModel model, Path folder, Path file)
    {
        //noinspection ResultOfMethodCallIgnored
        folder.toFile().mkdirs();
        Utils.dumpToOBJ(file, "item", model);

        showSuccessMessage(mc, folder, file);
        return 1;
    }

    private static void showSuccessMessage(Minecraft mc, Path outFolder, Path outFile)
    {
        showSuccessMessage(mc, outFolder, outFile, "Model");
    }

    private static void showSuccessMessage(Minecraft mc, Path outFolder, Path outFile, String what)
    {
        MutableComponent pathComponent = Component.literal(outFile.toFile().getAbsolutePath());
        pathComponent = pathComponent.withStyle(style -> style
                .withUnderlined(true)
                .withColor(ChatFormatting.GREEN)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open folder")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outFolder.toFile().getAbsolutePath())));
        mc.gui.getChat().addMessage(Component.literal(what + " dumped to ").append(pathComponent));
    }

    private static class LimitedWrapper implements BlockAndTintGetter
    {
        private final ClientLevel level;
        private final BlockPos minP;
        private final BlockPos maxP;

        public LimitedWrapper(ClientLevel level, BlockPos minP, BlockPos maxP)
        {
            this.level = level;
            this.minP = minP;
            this.maxP = maxP;
        }

        @Override
        public float getShade(Direction p_45522_, boolean p_45523_)
        {
            return level.getShade(p_45522_, p_45523_);
        }

        @Override
        public LevelLightEngine getLightEngine()
        {
            return level.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos p_45520_, ColorResolver p_45521_)
        {
            return level.getBlockTint(p_45520_, p_45521_);
        }

        @org.jetbrains.annotations.Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos p_45570_)
        {
            if (isOutsideLimits(p_45570_)) return null;
            return level.getBlockEntity(p_45570_);
        }

        private boolean isOutsideLimits(BlockPos pos)
        {
            return pos.getX() < minP.getX() || pos.getY() < minP.getY() || pos.getZ() < minP.getZ() ||
                    pos.getX() > maxP.getX() || pos.getY() > maxP.getY() || pos.getZ() > maxP.getZ();
        }

        @Override
        public BlockState getBlockState(BlockPos p_45571_)
        {
            if (isOutsideLimits(p_45571_)) return Blocks.AIR.defaultBlockState();
            return level.getBlockState(p_45571_);
        }

        @Override
        public FluidState getFluidState(BlockPos p_45569_)
        {
            if (isOutsideLimits(p_45569_)) return Fluids.EMPTY.defaultFluidState();
            return level.getFluidState(p_45569_);
        }

        @Override
        public int getHeight()
        {
            return level.getHeight();
        }

        @Override
        public int getMinBuildHeight()
        {
            return level.getMinBuildHeight();
        }
    }
}
