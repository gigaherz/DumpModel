package gigaherz.dumpmodel.builders;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import gigaherz.dumpmodel.Utils;
import gigaherz.dumpmodel.builders.writers.ModelWriter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelMesh<T extends ModelWriter<T>>
{
    private final ModelGroup<T> parent;
    private final String name;
    private NamedMaterial mat;
    private final Map<VertexFormatElement, Integer> requireElements = new HashMap<>();
    private final List<ModelFace<T>> faces = new ArrayList<>();

    public ModelMesh(ModelGroup<T> parent, String name, @Nullable Direction side)
    {
        this.parent = parent;
        this.name = side != null
                ? String.format("side_%s_%s", side, name)
                : name;
    }

    public ModelGroup<T> parent()
    {
        return parent;
    }

    public String name()
    {
        return name;
    }

    public NamedMaterial material()
    {
        return mat;
    }

    public Map<VertexFormatElement, Integer> requireElements()
    {
        return requireElements;
    }

    public List<ModelFace<T>> faces()
    {
        return faces;
    }

    public ModelMesh<T> setMaterial(NamedMaterial mat)
    {
        this.mat = mat;
        return this;
    }

    public ModelMesh<T> addQuad(BakedQuad quad)
    {
        VertexFormat fmt = DefaultVertexFormat.BLOCK;
        int[] vdata = quad.getVertices();
        int byteStart = 0;
        ModelFace<T> face = face();
        for (int i = 0; i < 4; i++)
        {
            ModelFaceVertex<T> vertex = face.vertex();
            for (VertexFormatElement element : fmt.getElements())
            {
                if (element.getUsage() != VertexFormatElement.Usage.PADDING)
                {
                    double[] values = Utils.extractData(vdata, byteStart, element);
                    vertex.element(element, values);
                }
                byteStart += element.getType().getSize() * element.getElementCount();
            }
            vertex.end();
        }
        return face.end();
    }

    public ModelFace<T> face()
    {
        var face = new ModelFace<>(this);
        faces.add(face);
        return face;
    }

    public ModelGroup<T> end()
    {
        return parent;
    }
}
