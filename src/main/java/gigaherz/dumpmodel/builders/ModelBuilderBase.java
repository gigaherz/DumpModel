package gigaherz.dumpmodel.builders;

import com.mojang.blaze3d.vertex.VertexFormatElement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ModelBuilderBase<T extends ModelBuilderBase<T>>
{
    private final Map<VertexFormatElement, List<double[]>> elementDatas = new HashMap<>();
    private final Map<String, ModelMaterial> materialLibrary = new HashMap<>();
    private final Map<String, ModelMaterial> texToMaterial = new HashMap<>();
    private final List<ModelGroup<T>> groups = new ArrayList<>();

    public ModelGroup<T> part(String name)
    {
        var part = new ModelGroup<>(this, name);
        groups.add(part);
        return part;
    }

    public abstract void save(Path outFile);

    public ModelMaterial newMaterial(String tex)
    {
        return texToMaterial.computeIfAbsent(tex, tx -> {
            var autoname = "Mat_" + materialLibrary.size();
            var mat = new ModelMaterial(autoname, tex);
            materialLibrary.put(autoname, mat);
            return mat;
        });
    }

    public Map<VertexFormatElement, List<double[]>> elementDatas()
    {
        return elementDatas;
    }

    public Map<String, ModelMaterial> materialLibrary()
    {
        return materialLibrary;
    }

    public List<ModelGroup<T>> groups()
    {
        return groups;
    }

}
