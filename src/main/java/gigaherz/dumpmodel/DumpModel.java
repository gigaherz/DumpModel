package gigaherz.dumpmodel;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod("dumpmodel")
public class DumpModel
{
    public DumpModel(ModContainer container, IEventBus modEventBus)
    {
        NeoForge.EVENT_BUS.addListener(this::clientCommands);

        container.registerConfig(ModConfig.Type.CLIENT, Configs.CLIENT_SPEC);

        modEventBus.addListener(this::loadConfigs);
        modEventBus.addListener(this::reloadConfigs);
    }

    public void loadConfigs(ModConfigEvent.Loading event)
    {
        readConfigs(event);
    }

    public void reloadConfigs(ModConfigEvent.Reloading event)
    {
        readConfigs(event);
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
