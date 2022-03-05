package gigaherz.dumpmodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gigaherz.dumpmodel.builders.OBJBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.RenderProperties;
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
    private static final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

    public static void init()
    {
        registerCommands(dispatcher);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("dumpmodel")
                        .then(Commands.literal("held")
                                .executes((ctx) -> {
                                    if (ctx.getSource().getLevel() != null) return 1;
                                    return dumpHeldItem(InteractionHand.MAIN_HAND);
                                })
                                .then(Commands.literal("mainhand").executes(ctx -> {
                                    if (ctx.getSource().getLevel() != null) return 1;
                                    return dumpHeldItem(InteractionHand.MAIN_HAND);
                                }))
                                .then(Commands.literal("offhand").executes(ctx -> {
                                    if (ctx.getSource().getLevel() != null) return 1;
                                    return dumpHeldItem(InteractionHand.OFF_HAND);
                                }))
                        )
                        .then(Commands.literal("target").executes((ctx) -> {
                            if (ctx.getSource().getLevel() != null) return 1;
                            return dumpTargettedBlock();
                        }))
                        .then(Commands.literal("item").then(Commands.argument("item", ItemArgument.item()).executes((ctx) -> {
                            if (ctx.getSource().getLevel() != null) return 1;
                            return dumpItemModel(ItemArgument.getItem(ctx, "item").createItemStack(1, false));
                        })))
                        .then(Commands.literal("block").then(Commands.argument("block", BlockStateArgument.block()).executes((ctx) -> {
                            if (ctx.getSource().getLevel() != null) return 1;
                            return dumpBlockModel(BlockStateArgument.getBlock(ctx, "block").getState(), null);
                        })))
                        .then(Commands.literal("entity").then(Commands.argument("entity", EntityArgument.entity()).executes((ctx) -> {
                            if (ctx.getSource().getLevel() != null) return 1;
                            return dumpEntityRenderer(EntityArgument.getEntity(ctx, "entity"));
                        })))
        );
    }

    public static void onClientChat(ClientChatEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;

        String message = event.getMessage();
        if (!message.startsWith("/dumpmodel "))
            return;

        event.setCanceled(true);

        mc.gui.getChat().addRecentChat(message);

        String command = message.substring(1);

        try
        {
            dispatcher.execute(command, new CommandSourceStack(
                    new ClientCommandSource(), mc.player.position(), mc.player.getRotationVector(), null, 0,
                    "dummy", new TextComponent("Dummy Client Command Parser"), null, null));
        }
        catch (CommandSyntaxException e)
        {
            mc.gui.handleChat(ChatType.SYSTEM, new TextComponent("Error parsing command: " + e.getMessage()), Util.NIL_UUID);
            mc.gui.handleChat(ChatType.SYSTEM, new TextComponent("Usage: /dumpmodel held|target|item <item>|block <block>" + e.getMessage()), Util.NIL_UUID);
        }
    }

    private static int dumpHeldItem(InteractionHand hand)
    {
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = Objects.requireNonNull(mc.player).getItemInHand(hand);
        if (held.getCount() <= 0)
        {
            mc.gui.handleChat(ChatType.SYSTEM, new TextComponent("You must be holding an item in your " + hand + " to use this command."), Util.NIL_UUID);
            return 0;
        }

        return dumpItemModel(held);
    }

    private static int dumpTargettedBlock()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult == null || (mc.hitResult.getType() != HitResult.Type.BLOCK && mc.hitResult.getType() != HitResult.Type.ENTITY))
        {
            mc.gui.handleChat(ChatType.SYSTEM, new TextComponent("You must be looking at a block or entity to use the 'target' subcommand."), Util.NIL_UUID);
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

        if (model.isCustomRenderer())
        {
            VertexDumper dumper = new VertexDumper(OBJBuilder.begin());
            RenderProperties.get(stack.getItem()).getItemStackRenderer()
                    .renderByItem(stack, ItemTransforms.TransformType.FIXED, new PoseStack(), dumper, 0x00F000F0, OverlayTexture.NO_OVERLAY);

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
            data = ModelDataManager.getModelData(Objects.requireNonNull(mc.level), pos);
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
        BakedModel model = mc.getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        switch (state.getRenderShape())
        {
            case MODEL:
                if (!model.isCustomRenderer())
                {
                    mc.getBlockRenderer().renderSingleBlock(state, new PoseStack(), dumper, 0x00F000F0, OverlayTexture.NO_OVERLAY, data);
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
                            if (state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED)
                            {
                                mc.gui.handleChat(ChatType.SYSTEM, new TextComponent("The block needs a builtin renderer but there is no TileEntity Renderer."), Util.NIL_UUID);
                                return 0;
                            }
                        }
                        ter.render(te, mc.getFrameTime(), new PoseStack(), dumper, 0x00F000F0, OverlayTexture.NO_OVERLAY);
                    }
                    else if (model.isCustomRenderer())
                    {
                        if (state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED)
                        {
                            mc.gui.handleChat(ChatType.SYSTEM, new TextComponent("The block needs a builtin renderer but there is no TileEntity."), Util.NIL_UUID);
                            return 0;
                        }
                    }
                }
                else if (model.isCustomRenderer())
                {
                    if (state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED)
                    {
                        mc.gui.handleChat(ChatType.SYSTEM, new TextComponent("The block needs a builtin renderer but I have no BlockPos context."), Util.NIL_UUID);
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
            if (mc.player == null || mc.level == null)
                return 0;

            VertexDumper dumper = new VertexDumper(OBJBuilder.begin());

            if (entity instanceof EnderDragonPart)
            {
                EnderDragonPart dragonPart = (EnderDragonPart) entity;
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

            ResourceLocation regName = entity.getType().getRegistryName();
            if (regName == null)
                throw new RuntimeException("EntityType registry name is null");
            Path folder = FMLPaths.GAMEDIR.get()
                    .resolve("models/entities")
                    .resolve(regName.getNamespace());
            Path file = folder.resolve(regName.getPath() + ".obj");

            return dumpVertexDumper(entity.getScoreboardName(), mc, dumper, folder, file);
        }
        catch (Exception e)
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

    private static int dumpBakedModel(Minecraft mc, BakedModel model, Path folder, Path file, @Nullable BlockState state, IModelData data)
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
        MutableComponent pathComponent = new TextComponent(outFile.getAbsolutePath());
        pathComponent = pathComponent.withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
        pathComponent = pathComponent.withStyle(style -> {
            style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to open folder")));
            style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outFolder.getAbsolutePath()));
            return style;
        });
        mc.gui.handleChat(ChatType.SYSTEM, new TextComponent("Model dumped to ").append(pathComponent), Util.NIL_UUID);
    }

    private static class ClientCommandSource implements CommandSource
    {
        final Minecraft mc = Minecraft.getInstance();

        @Override
        public void sendMessage(Component component, UUID senderUUID)
        {
            mc.gui.handleChat(ChatType.SYSTEM, component, Util.NIL_UUID);
        }

        @Override
        public boolean acceptsSuccess()
        {
            return true;
        }

        @Override
        public boolean acceptsFailure()
        {
            return true;
        }

        @Override
        public boolean shouldInformAdmins()
        {
            return true;
        }
    }
}
