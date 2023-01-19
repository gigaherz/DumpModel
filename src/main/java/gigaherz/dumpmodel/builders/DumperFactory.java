package gigaherz.dumpmodel.builders;

import java.util.function.Supplier;

public record DumperFactory<T extends ModelBuilderBase<T>>(String extension, Supplier<T> factory)
{
    public static final DumperFactory<ObjModelBuilder> OBJ = new DumperFactory<>(".obj", ObjModelBuilder::begin);
    public static final DumperFactory<UsdModelBuilder> USD = new DumperFactory<>(".usda", UsdModelBuilder::begin);

    public T create()
    {
        return factory.get();
    }
}
