package gigaherz.dumpmodel.builders;

public record ModelMaterial(String name, String texture, float r, float g, float b, float a)
        implements IMaterial<ModelMaterial>
{
}
