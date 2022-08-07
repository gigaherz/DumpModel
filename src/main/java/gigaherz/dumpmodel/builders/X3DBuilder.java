package gigaherz.dumpmodel.builders;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.datafixers.util.Pair;
import gigaherz.dumpmodel.Utils;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class X3DBuilder
        implements IBuilder<X3DBuilder, X3DBuilder.Part, X3DBuilder.Part.Group, X3DBuilder.Part.Group.Face, X3DBuilder.Part.Group.Face.Vertex, X3DBuilder.Material>
{
    private final List<Pair<String, Part>> parts = new ArrayList<>();
    private final List<Pair<String, Material>> materials = new ArrayList<>();

    @Override
    public void save(File file)
    {

    }

    @Override
    public X3DBuilder.Part part(String name)
    {
        var part = new Part();
        parts.add(Pair.of(name, part));
        return part;
    }

    @Override
    public X3DBuilder.Material newMaterial(String path)
    {
        var part = new Material(path);
        materials.add(Pair.of(path, part));
        return part;
    }

    @Override
    public X3DBuilder.Material newMaterial(String path, float r, float g, float b, float a)
    {
        return null;
    }

    public class Part
            implements IBuilderPart<X3DBuilder, Part, Part.Group, Part.Group.Face, Part.Group.Face.Vertex, Material>
    {
        private final List<Pair<String, Group>> groups = new ArrayList<>();

        public Part()
        {
        }

        @Override
        public Group group(@Nullable Direction side)
        {
            var group = new Group();
            groups.add(Pair.of("direction_" + (side != null ? side.toString() : "general"), group));
            return group;
        }

        @Override
        public Group group(String name, @Nullable Direction side)
        {
            var group = new Group();
            groups.add(Pair.of(name + "_direction_" + (side != null ? side.toString() : "general"), group));
            return group;
        }

        @Override
        public X3DBuilder end()
        {
            return X3DBuilder.this;
        }

        public class Group implements IBuilderGroup<X3DBuilder, Part, Group, Group.Face, Group.Face.Vertex, Material>
        {
            private final List<Face> faces = new ArrayList<>();
            private Material material;

            public Group()
            {
            }

            @Override
            public Group addQuad(BakedQuad quad)
            {
                VertexFormat fmt = DefaultVertexFormat.BLOCK;
                int[] vdata = quad.getVertices();
                int byteStart = 0;
                var face = face();
                for (int i = 0; i < 4; i++)
                {
                    var vertex = face.vertex();
                    for (VertexFormatElement element : fmt.getElements())
                    {
                        double[] values = Utils.extractData(vdata, byteStart, element);
                        switch (element.getUsage())
                        {
                            case POSITION:
                                if (element.getIndex() == 0)
                                    vertex.position(values[0], values[1], values[2]);
                                else
                                    vertex.element(element, values);
                                break;
                            case NORMAL:
                                if (element.getIndex() == 0)
                                    vertex.normal(values[0], values[1], values[2]);
                                else
                                    vertex.element(element, values);
                                break;
                            case UV:
                                if (element.getIndex() == 0)
                                    vertex.tex(values[0], values[1]);
                                else
                                    vertex.element(element, values);
                                break;
                            case COLOR:
                                if (element.getIndex() == 0)
                                    vertex.color(values[0], values[1], values[2], values[3]);
                                else
                                    vertex.element(element, values);
                                break;
                            case PADDING:
                                break;
                            default:
                                vertex.element(element, values);
                                break;
                        }
                        byteStart += element.getType().getSize() * element.getElementCount();
                    }
                    face = vertex.end();
                }
                return face.end();
            }

            @Override
            public Face face()
            {
                var face = new Face();
                faces.add(face);
                return face;
            }

            @Override
            public Part end()
            {
                return Part.this;
            }

            @Override
            public Group setMaterial(Material texName)
            {
                material = texName;
                return this;
            }

            public class Face implements IBuilderFace<X3DBuilder, Part, Group, Face, Face.Vertex, Material>
            {
                @Override
                public Vertex vertex()
                {
                    return null;
                }

                @Override
                public Group end()
                {
                    return Group.this;
                }

                public class Vertex implements IBuilderVertex<X3DBuilder, Part, Group, Face, Vertex, Material>
                {
                    @Override
                    public Vertex position(double x, double y, double z)
                    {
                        return null;
                    }

                    @Override
                    public Vertex tex(double u, double v)
                    {
                        return null;
                    }

                    @Override
                    public Vertex normal(double x, double y, double z)
                    {
                        return null;
                    }

                    public Vertex color(double r, double g, double b, double a)
                    {
                        return null;
                    }

                    @Override
                    public Vertex element(VertexFormatElement element, double... values)
                    {
                        return null;
                    }

                    @Override
                    public Face end()
                    {
                        return Face.this;
                    }
                }
            }
        }
    }

    public class Material implements IMaterial<Material>
    {
        private final String path;

        public Material(String path)
        {
            this.path = path;
        }
    }
}
