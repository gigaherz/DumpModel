package gigaherz.dumpmodel.builders;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import gigaherz.dumpmodel.Configs;
import gigaherz.dumpmodel.builders.writers.GltfModelWriter;
import gigaherz.dumpmodel.builders.writers.ModelWriter;
import gigaherz.dumpmodel.builders.writers.ObjModelWriter;
import gigaherz.dumpmodel.builders.writers.UsdModelWriter;

import java.util.function.Supplier;

public record WriterFactory<T extends ModelWriter<T>>(String extension, Supplier<T> factory)
{
    private static final BiMap<String, WriterFactory<?>> factories = HashBiMap.create();

    public static final WriterFactory<ObjModelWriter> OBJ = register("obj", new WriterFactory<>(".obj", ObjModelWriter::begin));
    public static final WriterFactory<UsdModelWriter> USD = register("usda", new WriterFactory<>(".usda", UsdModelWriter::begin));
    public static final WriterFactory<GltfModelWriter> GLTF = register("gltf", new WriterFactory<>(".gltf", GltfModelWriter::begin));

    private static WriterFactory<?> activeFactory = WriterFactory.OBJ;

    private static <T extends ModelWriter<T>> WriterFactory<T> register(String name, WriterFactory<T> factory)
    {
        factories.put(name, factory);
        return factory;
    }

    public static boolean isValidFactoryName(String name)
    {
        return factories.containsKey(name);
    }

    public static WriterFactory<?> getActiveFactory()
    {
        return activeFactory;
    }

    public static void setActiveFactory(String name)
    {
        var factory = factories.get(name);
        if (factory == null) throw new RuntimeException("Factory with name " + name + " not found");
        activeFactory = factory;
        Configs.activeFactoryUpdated(name);
    }

    public static String getActiveFactoryName()
    {
        return factories.inverse().get(activeFactory);
    }

    public static Iterable<String> getFactoryNames()
    {
        return factories.keySet();
    }

    // Implementation
    public T create()
    {
        return factory.get();
    }
}
