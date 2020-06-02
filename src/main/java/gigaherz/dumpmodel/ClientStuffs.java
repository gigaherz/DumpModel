package gigaherz.dumpmodel;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
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
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
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
                        .then(Commands.literal("held").executes((ctx) -> {
                            if (ctx.getSource().getWorld() != null) return 1;
                            return dumpHeldItem();
                        }))
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
                            return dumpBlockModel(BlockStateArgument.getBlockState(ctx, "block").getState(), EmptyModelData.INSTANCE);
                        })))
                        .then(Commands.literal("entity").then(Commands.argument("entity", EntityArgument.entity()).executes((ctx) -> {
                            if (ctx.getSource().getWorld() != null) return 1;
                            return dumpEntityRenderer(EntityArgument.getEntity(ctx, "entity"));
                        }))));
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
            mc.ingameGUI.addChatMessage(ChatType.SYSTEM, new StringTextComponent("Error parsing command: " + e.getMessage()));
            mc.ingameGUI.addChatMessage(ChatType.SYSTEM, new StringTextComponent("Usage: /dumpmodel held|target|item <item>|block <block>" + e.getMessage()));
        }
    }

    private static int dumpHeldItem()
    {
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = Objects.requireNonNull(mc.player).getHeldItemMainhand();
        if (held.getCount() <= 0)
        {
            mc.ingameGUI.addChatMessage(ChatType.SYSTEM, new StringTextComponent("You must be holding an item in your main hand to use the 'held' subcommand."));
            return 0;
        }

        return dumpItemModel(held);
    }

    private static int dumpTargettedBlock()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.objectMouseOver == null || (mc.objectMouseOver.getType() != RayTraceResult.Type.BLOCK && mc.objectMouseOver.getType() != RayTraceResult.Type.ENTITY))
        {
            mc.ingameGUI.addChatMessage(ChatType.SYSTEM, new StringTextComponent("You must be looking at a block or entity to use the 'target' subcommand."));
            return 0;
        }

        if (mc.objectMouseOver.getType() == RayTraceResult.Type.BLOCK)
        {
            BlockRayTraceResult br = (BlockRayTraceResult) mc.objectMouseOver;
            BlockState state = Objects.requireNonNull(mc.world).getBlockState(br.getPos());
            IModelData data = ModelDataManager.getModelData(mc.world, br.getPos());
            return dumpBlockModel(state, data != null ? data : EmptyModelData.INSTANCE);
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
        return dump(mc, model, folder, file, null, EmptyModelData.INSTANCE);
    }

    private static int dumpBlockModel(BlockState state, IModelData data)
    {
        Minecraft mc = Minecraft.getInstance();
        IBakedModel model = mc.getBlockRendererDispatcher().getBlockModelShapes().getModel(state);
        ResourceLocation regName = state.getBlock().getRegistryName();
        if (regName == null)
            throw new RuntimeException("Block registry name is null");
        Path folder = FMLPaths.GAMEDIR.get()
                .resolve("models/blocks")
                .resolve(regName.getNamespace());
        Path file = folder.resolve(regName.getPath() + ".obj");
        return dump(mc, model, folder, file, state, data);
    }

    private static int dumpEntityRenderer(Entity entity)
    {
        try
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.world == null)
                return 0;

            VertexDumper dumper = new VertexDumper();

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

            File outFolder = folder.toFile();
            File outFile = file.toFile();

            //noinspection ResultOfMethodCallIgnored
            outFolder.mkdirs();

            dumper.dumpToOBJ(outFile, entity.getScoreboardName());

            showSuccessMessage(mc, outFolder, outFile);
            return 1;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Error rendering entity to dump: ", e);
        }
    }

    private static int dump(Minecraft mc, IBakedModel model, Path folder, Path file, @Nullable BlockState state, IModelData data)
    {
        File outFolder = folder.toFile();
        File outFile = file.toFile();

        //noinspection ResultOfMethodCallIgnored
        outFolder.mkdirs();
        DumpBakedModel.dumpToOBJ(outFile, "item", model, state, data);

        showSuccessMessage(mc, outFolder, outFile);
        return 1;
    }

    private static void showSuccessMessage(Minecraft mc, File outFolder, File outFile)
    {
        StringTextComponent pathComponent = new StringTextComponent(outFile.getAbsolutePath());
        pathComponent.applyTextStyles(TextFormatting.UNDERLINE,TextFormatting.BOLD);
        pathComponent.applyTextStyle(style -> {
            style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to open folder")));
            style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outFolder.getAbsolutePath()));
        });
        mc.ingameGUI.addChatMessage(ChatType.SYSTEM, new StringTextComponent("Model dumped to ").appendSibling(pathComponent));
    }

    private static class ClientCommandSource implements ICommandSource
    {
        final Minecraft mc = Minecraft.getInstance();

        @Override
        public void sendMessage(ITextComponent component)
        {
            mc.ingameGUI.addChatMessage(ChatType.SYSTEM, component);
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
