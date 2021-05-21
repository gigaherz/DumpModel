package gigaherz.dumpmodel;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gigaherz.dumpmodel.builders.OBJBuilder;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ItemArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class ClientStuffs
{
    private static final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();

    public static void init()
    {
        registerCommands(dispatcher);
    }

    private static void registerCommands(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("dumpmodel")
                        .then(Commands.literal("held")
                                .executes((ctx) -> {
                                    if (ctx.getSource().getWorld() != null) return 1;
                                    return dumpHeldItem(Hand.MAIN_HAND);
                                })
                                .then(Commands.literal("mainhand").executes(ctx -> {
                                    if (ctx.getSource().getWorld() != null) return 1;
                                    return dumpHeldItem(Hand.MAIN_HAND);
                                }))
                                .then(Commands.literal("offhand").executes(ctx -> {
                                    if (ctx.getSource().getWorld() != null) return 1;
                                    return dumpHeldItem(Hand.OFF_HAND);
                                }))
                        )
                        .then(Commands.literal("target").executes((ctx) -> {
                            if (ctx.getSource().getWorld() != null) return 1;
                            return dumpTargettedBlock();
                        }))
                        .then(Commands.literal("item").then(Commands.argument("item", ItemArgument.item()).executes((ctx) -> {
                            if (ctx.getSource().getWorld() != null) return 1;
                            return dumpItemModel(ItemArgument.getItem(ctx, "item").createStack(1, false));
                        })))
                        .then(Commands.literal("block").then(Commands.argument("block", BlockStateArgument.blockState()).executes((ctx) -> {
                            if (ctx.getSource().getWorld() != null) return 1;
                            return dumpBlockModel(BlockStateArgument.getBlockState(ctx, "block").getState(), null);
                        })))
                        .then(Commands.literal("entity").then(Commands.argument("entity", EntityArgument.entity()).executes((ctx) -> {
                            if (ctx.getSource().getWorld() != null) return 1;
                            return dumpEntityRenderer(EntityArgument.getEntity(ctx, "entity"));
                        })))
        );
    }

    public static void onClientChat(ClientChatEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.world == null)
            return;

        String message = event.getMessage();
        if (!message.startsWith("/dumpmodel "))
            return;

        event.setCanceled(true);

        mc.ingameGUI.getChatGUI().addToSentMessages(message);

        String command = message.substring(1);

        try
        {
            dispatcher.execute(command, new CommandSource(
                    new ClientCommandSource(), mc.player.getPositionVec(), mc.player.getPitchYaw(), null, 0,
                    "dummy", new StringTextComponent("Dummy Client Command Parser"), null, null));
        }
        catch (CommandSyntaxException e)
        {
            mc.ingameGUI.func_238450_a_(ChatType.SYSTEM, new StringTextComponent("Error parsing command: " + e.getMessage()), Util.DUMMY_UUID);
            mc.ingameGUI.func_238450_a_(ChatType.SYSTEM, new StringTextComponent("Usage: /dumpmodel held|target|item <item>|block <block>" + e.getMessage()), Util.DUMMY_UUID);
        }
    }

    private static int dumpHeldItem(Hand hand)
    {
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = Objects.requireNonNull(mc.player).getHeldItem(hand);
        if (held.getCount() <= 0)
        {
            mc.ingameGUI.func_238450_a_(ChatType.SYSTEM, new StringTextComponent("You must be holding an item in your "+hand+" to use this command."), Util.DUMMY_UUID);
            return 0;
        }

        return dumpItemModel(held);
    }

    private static int dumpTargettedBlock()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.objectMouseOver == null || (mc.objectMouseOver.getType() != RayTraceResult.Type.BLOCK && mc.objectMouseOver.getType() != RayTraceResult.Type.ENTITY))
        {
            mc.ingameGUI.func_238450_a_(ChatType.SYSTEM, new StringTextComponent("You must be looking at a block or entity to use the 'target' subcommand."), Util.DUMMY_UUID);
            return 0;
        }

        if (mc.objectMouseOver.getType() == RayTraceResult.Type.BLOCK)
        {
            BlockRayTraceResult br = (BlockRayTraceResult) mc.objectMouseOver;
            BlockState state = Objects.requireNonNull(mc.world).getBlockState(br.getPos());
            return dumpBlockModel(state, br.getPos());
        }
        else if (mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY)
        {
            EntityRayTraceResult br = (EntityRayTraceResult) mc.objectMouseOver;
            return dumpEntityRenderer(br.getEntity());
        }

        return 0;
    }

    private static int dumpItemModel(ItemStack stack)
    {
        Minecraft mc = Minecraft.getInstance();
        IBakedModel model = mc.getItemRenderer().getItemModelWithOverrides(stack, mc.world, mc.player);
        ResourceLocation regName = stack.getItem().getRegistryName();
        if (regName == null)
            throw new RuntimeException("Item registry name is null");
        Path folder = FMLPaths.GAMEDIR.get()
                .resolve("models/items")
                .resolve(regName.getNamespace());
        Path file;
        if (stack.hasTag())
        {
            folder = folder.resolve(regName.getPath());
            file = folder.resolve(Objects.requireNonNull(stack.getTag()).hashCode() + ".obj");
        }
        else
        {
            file = folder.resolve(regName.getPath() + ".obj");
        }

        if (model.isBuiltInRenderer())
        {
            VertexDumper dumper = new VertexDumper(OBJBuilder.begin());
            stack.getItem().getItemStackTileEntityRenderer()
                    .func_239207_a_(stack, ItemCameraTransforms.TransformType.FIXED, new MatrixStack(), dumper, 0x00F000F0, OverlayTexture.NO_OVERLAY);

            return dumpVertexDumper(regName.toString(), mc, dumper, folder, file);
        }
        else
        {
            return dumpBakedModel(mc, model, folder, file, null, EmptyModelData.INSTANCE);
        }
    }

    private static int dumpBlockModel(BlockState state, @Nullable BlockPos pos)
    {
        Minecraft mc = Minecraft.getInstance();

        IModelData data = EmptyModelData.INSTANCE;
        if (pos != null)
        {
            data = ModelDataManager.getModelData(Objects.requireNonNull(mc.world), pos);
            if (data == null) data = EmptyModelData.INSTANCE;
        }

        ResourceLocation regName = state.getBlock().getRegistryName();
        if (regName == null)
            throw new RuntimeException("Block registry name is null");
        Path folder = FMLPaths.GAMEDIR.get()
                .resolve("models/blocks")
                .resolve(regName.getNamespace());
        Path file = folder.resolve(regName.getPath() + ".obj");

        VertexDumper dumper = new VertexDumper(OBJBuilder.begin());
        IBakedModel model = mc.getBlockRendererDispatcher().getBlockModelShapes().getModel(state);
        switch(state.getRenderType())
        {
            case MODEL:
                if (!model.isBuiltInRenderer())
                {
                    mc.getBlockRendererDispatcher().renderBlock(state, new MatrixStack(), dumper, 0x00F000F0, OverlayTexture.NO_OVERLAY, data);
                }
                // fallthrough;
            case ENTITYBLOCK_ANIMATED:
                if (pos != null)
                {
                    TileEntity te = mc.world.getTileEntity(pos);
                    if (te != null)
                    {
                        TileEntityRenderer<TileEntity> ter = TileEntityRendererDispatcher.instance.getRenderer(te);
                        if (ter == null)
                        {
                            if (state.getRenderType() == BlockRenderType.ENTITYBLOCK_ANIMATED)
                            {
                                mc.ingameGUI.func_238450_a_(ChatType.SYSTEM, new StringTextComponent("The block needs a builtin renderer but there is no TileEntity Renderer."), Util.DUMMY_UUID);
                                return 0;
                            }
                        }
                        ter.render(te, mc.getRenderPartialTicks(), new MatrixStack(), dumper, 0x00F000F0, OverlayTexture.NO_OVERLAY);
                    }
                    else if (model.isBuiltInRenderer())
                    {
                        if (state.getRenderType() == BlockRenderType.ENTITYBLOCK_ANIMATED)
                        {
                            mc.ingameGUI.func_238450_a_(ChatType.SYSTEM, new StringTextComponent("The block needs a builtin renderer but there is no TileEntity."), Util.DUMMY_UUID);
                            return 0;
                        }
                    }
                }
                else if (model.isBuiltInRenderer())
                {
                    if (state.getRenderType() == BlockRenderType.ENTITYBLOCK_ANIMATED)
                    {
                        mc.ingameGUI.func_238450_a_(ChatType.SYSTEM, new StringTextComponent("The block needs a builtin renderer but I have no BlockPos context."), Util.DUMMY_UUID);
                        return 0;
                    }
                }
                break;
        }
        return dumpVertexDumper(regName.toString(), mc, dumper, folder, file);
    }

    private static int dumpEntityRenderer(Entity entity)
    {
        try
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.world == null)
                return 0;

            VertexDumper dumper = new VertexDumper(OBJBuilder.begin());

            if (entity instanceof EnderDragonPartEntity)
            {
                EnderDragonPartEntity dragonPart = (EnderDragonPartEntity) entity;
                EnderDragonEntity dragon = dragonPart.dragon;

                EntityRenderer<? super EnderDragonEntity> renderer = mc.getRenderManager().getRenderer(dragon);

                renderer.render(dragon, 0, mc.getRenderPartialTicks(), new MatrixStack(), dumper, 0x00F000F0);
            }
            else
            {
                @SuppressWarnings("rawtypes")
                EntityRenderer renderer = mc.getRenderManager().getRenderer(entity);

                //noinspection unchecked
                renderer.render(entity, 0, mc.getRenderPartialTicks(), new MatrixStack(), dumper, 0x00F000F0);
            }

            ResourceLocation regName = entity.getType().getRegistryName();
            if (regName == null)
                throw new RuntimeException("EntityType registry name is null");
            Path folder = FMLPaths.GAMEDIR.get()
                    .resolve("models/entities")
                    .resolve(regName.getNamespace());
            Path file = folder.resolve(regName.getPath() + ".obj");

            return dumpVertexDumper(entity.getScoreboardName(), mc, dumper, folder, file);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Error rendering entity to dump: ", e);
        }
    }

    private static int dumpVertexDumper(String name, Minecraft mc, VertexDumper dumper, Path folder, Path file)
    {
        File outFolder = folder.toFile();
        File outFile = file.toFile();

        //noinspection ResultOfMethodCallIgnored
        outFolder.mkdirs();

        dumper.dumpToOBJ(outFile, name);

        showSuccessMessage(mc, outFolder, outFile);
        return 1;
    }

    private static int dumpBakedModel(Minecraft mc, IBakedModel model, Path folder, Path file, @Nullable BlockState state, IModelData data)
    {
        File outFolder = folder.toFile();
        File outFile = file.toFile();

        //noinspection ResultOfMethodCallIgnored
        outFolder.mkdirs();
        Utils.dumpToOBJ(outFile, "item", model, state, data);

        showSuccessMessage(mc, outFolder, outFile);
        return 1;
    }

    private static void showSuccessMessage(Minecraft mc, File outFolder, File outFile)
    {
        IFormattableTextComponent pathComponent = new StringTextComponent(outFile.getAbsolutePath());
        pathComponent = pathComponent.mergeStyle(TextFormatting.UNDERLINE,TextFormatting.BOLD);
        pathComponent = pathComponent.modifyStyle(style -> {
            style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to open folder")));
            style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outFolder.getAbsolutePath()));
            return style;
        });
        mc.ingameGUI.func_238450_a_(ChatType.SYSTEM, new StringTextComponent("Model dumped to ").append(pathComponent), Util.DUMMY_UUID);
    }

    private static class ClientCommandSource implements ICommandSource
    {
        final Minecraft mc = Minecraft.getInstance();

        @Override
        public void sendMessage(ITextComponent component, UUID senderUUID)
        {
            mc.ingameGUI.func_238450_a_(ChatType.SYSTEM, component, Util.DUMMY_UUID);
        }

        @Override
        public boolean shouldReceiveFeedback()
        {
            return true;
        }

        @Override
        public boolean shouldReceiveErrors()
        {
            return true;
        }

        @Override
        public boolean allowLogging()
        {
            return true;
        }
    }
}
