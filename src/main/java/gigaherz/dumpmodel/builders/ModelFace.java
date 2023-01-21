package gigaherz.dumpmodel.builders;

import com.google.common.collect.Lists;

import java.util.List;

public class ModelFace<T extends ModelWriter<T>>
{
    private final ModelMesh<T> parent;
    private List<ModelFaceVertex<T>> vertices = Lists.newArrayList();

    public ModelFace(ModelMesh<T> parent)
    {
        this.parent = parent;
    }

    public ModelMesh<T> parent()
    {
        return parent;
    }

    public List<ModelFaceVertex<T>> vertices()
    {
        return vertices;
    }

    public ModelFaceVertex<T> vertex()
    {
        var vertex = new ModelFaceVertex<>(this);
        vertices.add(vertex);
        return vertex;
    }

    public ModelMesh<T> end()
    {
        return parent;
    }
}
