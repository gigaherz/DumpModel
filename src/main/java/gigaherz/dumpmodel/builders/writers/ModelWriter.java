package gigaherz.dumpmodel.builders.writers;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import gigaherz.dumpmodel.builders.BasicMaterial;
import gigaherz.dumpmodel.builders.ModelGroup;
import gigaherz.dumpmodel.builders.NamedMaterial;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ModelWriter<T extends ModelWriter<T>>
{
    private final Map<VertexFormatElement, List<double[]>> elementDatas = new HashMap<>();
    private final Map<String, NamedMaterial> materialLibrary = new HashMap<>();
    private final Map<BasicMaterial, NamedMaterial> texToMaterial = new HashMap<>();
    private final List<ModelGroup<T>> groups = new ArrayList<>();

    public ModelGroup<T> part(String name)
    {
        var part = new ModelGroup<>(this, name);
        groups.add(part);
        return part;
    }

    public abstract void save(Path outFile);

    public NamedMaterial newMaterial(BasicMaterial mat)
    {
        return texToMaterial.computeIfAbsent(mat, tx -> {
            var autoname = "Mat_" + materialLibrary.size();
            var mat1 = new NamedMaterial(autoname, mat);
            materialLibrary.put(autoname, mat1);
            return mat1;
        });
    }

    public Map<VertexFormatElement, List<double[]>> elementDatas()
    {
        return elementDatas;
    }

    public Map<String, NamedMaterial> materialLibrary()
    {
        return materialLibrary;
    }

    public List<ModelGroup<T>> groups()
    {
        return groups;
    }
}
