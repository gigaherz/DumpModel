package gigaherz.dumpmodel;

import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("dumpmodel")
public class DumpModel
{
    public DumpModel()
    {
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();
        modbus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.addListener(this::clientChat);
    }

    private void clientSetup(FMLClientSetupEvent event)
    {
        ClientStuffs.init();
    }

    private void clientChat(ClientChatEvent event)
    {
        ClientStuffs.onClientChat(event);
    }
}
