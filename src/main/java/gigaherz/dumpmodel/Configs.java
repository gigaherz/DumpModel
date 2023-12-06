package gigaherz.dumpmodel;

import gigaherz.dumpmodel.builders.WriterFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Configs
{
    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;
    static {
        final Pair<ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    private static class ClientConfig
    {
        ModConfigSpec.ConfigValue<String> saveFormat;

        ClientConfig(ModConfigSpec.Builder builder)
        {
            builder.push("general");
            saveFormat = builder
                    .comment("The file format used for saving models. ", "Allowed values: " + String.join(", ", WriterFactory.getFactoryNames()))
                    .define("format", WriterFactory.getActiveFactoryName(), (e) -> e instanceof String s && WriterFactory.isValidFactoryName(s));
            builder.pop();
        }
    }

    public static void activeFactoryUpdated(String name)
    {
        CLIENT.saveFormat.set(name);
        CLIENT.saveFormat.save();
    }

    public static void refreshClient()
    {
        var format = CLIENT.saveFormat.get();
        if (WriterFactory.isValidFactoryName(format))
        {
            WriterFactory.setActiveFactory(format);
        }
        else
        {
            activeFactoryUpdated(WriterFactory.getActiveFactoryName());
        }
    }
}
