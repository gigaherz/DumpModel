package gigaherz.dumpmodel;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

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
    public DumpModel() throws URISyntaxException, IOException
    {
        MinecraftForge.EVENT_BUS.addListener(this::clientCommands);
    }

    private void clientCommands(RegisterClientCommandsEvent event)
    {
        DumpCommand.init(event.getDispatcher(), event.getBuildContext());
    }
}
