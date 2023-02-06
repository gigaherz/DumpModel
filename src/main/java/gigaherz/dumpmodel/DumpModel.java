package gigaherz.dumpmodel;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;

@Mod("dumpmodel")
public class DumpModel
{
    public DumpModel()
    {
        MinecraftForge.EVENT_BUS.addListener(this::clientCommands);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Configs.CLIENT_SPEC);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::readConfigs);

    }
    public void readConfigs(ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == Configs.CLIENT_SPEC)
            Configs.refreshClient();
    }

    private void clientCommands(RegisterClientCommandsEvent event)
    {
        DumpCommand.init(event.getDispatcher(), event.getBuildContext());
    }
}
