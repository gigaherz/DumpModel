package gigaherz.dumpmodel;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod("dumpmodel")
public class DumpModel
{
    public DumpModel()
    {
        NeoForge.EVENT_BUS.addListener(this::clientCommands);

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
