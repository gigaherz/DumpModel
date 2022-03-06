package gigaherz.dumpmodel.builders;

public record SimpleMaterial(String name, String texture, float r, float g, float b, float a)
        implements IMaterial<SimpleMaterial>
{
}
