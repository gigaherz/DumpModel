package gigaherz.dumpmodel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.command.arguments.ItemArgument;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockRayTraceResult;
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
        if (mc.objectMouseOver == null || mc.objectMouseOver.getType() != RayTraceResult.Type.BLOCK)
        {
            mc.ingameGUI.addChatMessage(ChatType.SYSTEM, new StringTextComponent("You must be looking at a block to use the 'target' subcommand."));
            return 0;
        }

        BlockRayTraceResult br = (BlockRayTraceResult) mc.objectMouseOver;
        BlockState state = Objects.requireNonNull(mc.world).getBlockState(br.getPos());
        IModelData data = ModelDataManager.getModelData(mc.world, br.getPos());
        return dumpBlockModel(state, data != null ? data : EmptyModelData.INSTANCE);
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

    private static int dump(Minecraft mc, IBakedModel model, Path folder, Path file, @Nullable BlockState state, IModelData data)
    {
        File outFolder = folder.toFile();
        File outFile = file.toFile();

        //noinspection ResultOfMethodCallIgnored
        outFolder.mkdirs();
        DumpBakedModel.dumpToOBJ(outFile, "item", model, state, data);

        StringTextComponent pathComponent = new StringTextComponent(outFile.getAbsolutePath());
        pathComponent.applyTextStyles(TextFormatting.UNDERLINE,TextFormatting.BOLD);
        pathComponent.applyTextStyle(style -> {
            style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to open folder")));
            style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outFolder.getAbsolutePath()));
        });
        mc.ingameGUI.addChatMessage(ChatType.SYSTEM, new StringTextComponent("Model dumped to ").appendSibling(pathComponent));
        return 1;
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
