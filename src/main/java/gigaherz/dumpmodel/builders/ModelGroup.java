package gigaherz.dumpmodel.builders;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ModelGroup<T extends ModelBuilderBase<T>>
{
    private final ModelBuilderBase<T> parent;
    private final String name;
    private final List<ModelMesh<T>> groups = new ArrayList<>();

    ModelGroup(ModelBuilderBase<T> parent, String name)
    {
        this.parent = parent;
        this.name = name;
    }

    public ModelBuilderBase<T> parent()
    {
        return parent;
    }

    public String name()
    {
        return name;
    }

    public List<ModelMesh<T>> meshes()
    {
        return groups;
    }

    public ModelMesh<T> group(@Nullable Direction side)
    {
        return group(side == null ? "general" : side.toString(), side);
    }

    public ModelMesh<T> group(String name, @Nullable Direction side)
    {
        var group = new ModelMesh<>(this, name, side);
        groups.add(group);
        return group;
    }

    public T end()
    {
        return (T) parent;
    }
}
