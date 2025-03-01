package gigaherz.dumpmodel.builders;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import gigaherz.dumpmodel.builders.writers.ModelWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ModelFaceVertex<T extends ModelWriter<T>>
{
    private final ModelFace<T> parent;
    private final Map<VertexFormatElement, Integer> indices = new HashMap<>();

    public ModelFaceVertex(ModelFace<T> parent)
    {
        this.parent = parent;
    }

    public ModelFace<T> parent()
    {
        return parent;
    }

    public Map<VertexFormatElement, Integer> indices()
    {
        return indices;
    }

    public ModelFaceVertex<T> position(double x, double y, double z)
    {
        return element(VertexFormatElement.POSITION, x, y, z);
    }

    public ModelFaceVertex<T> tex(double u, double v)
    {
        return element(VertexFormatElement.UV0, u, v);
    }

    public ModelFaceVertex<T> normal(double x, double y, double z)
    {
        return element(VertexFormatElement.NORMAL, x, y, z);
    }

    public ModelFaceVertex<T> color(double r, double g, double b, double a)
    {
        return element(VertexFormatElement.COLOR, r, g, b, a);
    }

    public ModelFaceVertex<T> element(VertexFormatElement element, double... values)
    {
        if (indices.containsKey(element))
            throw new IllegalStateException("This element has already been assigned!");

        var elementArrays = parent.parent().parent().parent().elementDatas().computeIfAbsent(element, k -> new ArrayList<>());

        var index = elementArrays.size();

        elementArrays.add(values);

        indices.put(element, index);

        parent.parent().requireElements().put(element, elementArrays.size());

        return this;
    }

    public ModelFace<T> end()
    {
        return parent;
    }
}
