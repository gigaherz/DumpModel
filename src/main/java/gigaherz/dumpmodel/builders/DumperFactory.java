package gigaherz.dumpmodel.builders;

import java.util.function.Supplier;

public record DumperFactory<T extends ModelWriter<T>>(String extension, Supplier<T> factory)
{
    public static final DumperFactory<ObjModelWriter> OBJ = new DumperFactory<>(".obj", ObjModelWriter::begin);
    public static final DumperFactory<UsdModelWriter> USD = new DumperFactory<>(".usda", UsdModelWriter::begin);
    public static final DumperFactory<GltfModelWriter> GLTF = new DumperFactory<>(".gltf", GltfModelWriter::begin);

    public T create()
    {
        return factory.get();
    }
}
