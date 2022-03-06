package gigaherz.dumpmodel;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod("dumpmodel")
public class DumpModel
{
    public DumpModel()
    {
        MinecraftForge.EVENT_BUS.addListener(this::clientCommands);
    }

    private void clientCommands(RegisterClientCommandsEvent event)
    {
        DumpCommand.init(event.getDispatcher());
    }
}
