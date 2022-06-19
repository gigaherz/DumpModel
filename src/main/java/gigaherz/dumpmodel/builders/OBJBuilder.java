package gigaherz.dumpmodel.builders;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import gigaherz.dumpmodel.Utils;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class OBJBuilder
        implements IBuilder<OBJBuilder, OBJBuilder.Part, OBJBuilder.Part.Group, OBJBuilder.Part.Group.Face, OBJBuilder.Part.Group.Face.Vertex, SimpleMaterial>
{
    public static OBJBuilder begin()
    {
        return new OBJBuilder();
    }

    private final List<VertexFormatElement> elements = new ArrayList<>();
    private final Map<VertexFormatElement, String> elementPrefixes = new HashMap<>();
    private final Map<VertexFormatElement, Integer> elementCounts = new HashMap<>();
    private final List<String> lines = new ArrayList<>();
    private final Map<String, SimpleMaterial> materialLibrary = new HashMap<>();

    private OBJBuilder()
    {
        defineElement("v", DefaultVertexFormat.ELEMENT_POSITION, true);
        defineElement("vt", DefaultVertexFormat.ELEMENT_UV0, true);
        //defineElement("vt1", DefaultVertexFormat.ELEMENT_UV1, false);
        //defineElement("vt2", DefaultVertexFormat.ELEMENT_UV2, false);
        defineElement("vn", DefaultVertexFormat.ELEMENT_NORMAL, true);
        //defineElement("vc", DefaultVertexFormat.ELEMENT_COLOR, false);
    }

    private void defineElement(String id, VertexFormatElement element, boolean isStandard)
    {
        if (isStandard) elements.add(element);
        elementPrefixes.put(element, id);
    }

    private String getOrCreateElementPrefix(VertexFormatElement element)
    {
        return elementPrefixes.get(element);
        /*return elementPrefixes.computeIfAbsent(element, e ->
        {
            String ename = e.getUsage().getName().replace(" ", "");
            if (e.getIndex() != 0)
                ename += e.getIndex();
            return ename;
        });*/
    }

    public Part part(String name)
    {
        return new Part(name);
    }

    public void save(File file)
    {
        var pathWithoutExtension = FilenameUtils.removeExtension(file.getAbsolutePath());
        var matLib = pathWithoutExtension + ".mtl";

        try (OutputStream output = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(output))
        {
            if (materialLibrary.size() > 0)
            {
                writer.write(String.format("mtllib %s\n", matLib));
            }
            writer.write(String.join("\n", lines));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (materialLibrary.size() > 0)
        {
            try (OutputStream output = new FileOutputStream(matLib);
                 OutputStreamWriter writer = new OutputStreamWriter(output))
            {
                for(var mat : materialLibrary.values())
                {
                    writer.write(String.format("newmtl %s\n", mat.name()));
                    if (mat.r() != 1 || mat.g() != 1 || mat.b() != 1)
                    {
                        writer.write(String.format("Kd %s %s %s\n", mat.r(), mat.g(), mat.b()));
                    }
                    if (mat.texture() != null)
                    {
                        writer.write(String.format("map_Kd %s\n", mat.texture()));
                    }
                    if (mat.a() != 1)
                    {
                        writer.write(String.format("opacity %s\n", mat.a()));
                    }
                    writer.write("\n");
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    public SimpleMaterial newMaterial(String tex)
    {
        return newMaterial(tex, 1,1,1,1);
    }

    @Override
    public SimpleMaterial newMaterial(String tex, float r, float g, float b, float a)
    {
        var autoname = "Mat_" + materialLibrary.size();
        var mat = new SimpleMaterial(autoname, tex, r,g,b,a);
        materialLibrary.put(autoname, mat);
        return mat;
    }

    public class Part
            implements IBuilderPart<OBJBuilder, OBJBuilder.Part, OBJBuilder.Part.Group, OBJBuilder.Part.Group.Face, OBJBuilder.Part.Group.Face.Vertex, SimpleMaterial>
    {
        private Part(String name)
        {
            lines.add(String.format("g %s", name));
        }

        public Group group(@Nullable Direction side)
        {
            return group(side == null ? "general" : side.toString(), side);
        }

        public Group group(String name, @Nullable Direction side)
        {
            return new Group(name, side);
        }

        public OBJBuilder end()
        {
            return OBJBuilder.this;
        }

        public class Group
                implements IBuilderGroup<OBJBuilder, OBJBuilder.Part, OBJBuilder.Part.Group, OBJBuilder.Part.Group.Face, OBJBuilder.Part.Group.Face.Vertex, SimpleMaterial>
        {
            public Group(String name, @Nullable Direction side)
            {
                lines.add(String.format("o %s", name));
                if (side != null)
                    lines.add(String.format("o_Side %s", side));
            }

            @Override
            public Group setMaterial(SimpleMaterial mat)
            {
                lines.add(String.format("usemtl %s", mat.name()));
                return this;
            }

            public Group addQuad(BakedQuad quad)
            {
                VertexFormat fmt = DefaultVertexFormat.BLOCK;
                int[] vdata = quad.getVertices();
                int byteStart = 0;
                Face face = face();
                for (int i = 0; i < 4; i++)
                {
                    Face.Vertex vertex = face.vertex();
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

            public Face face()
            {
                return new Face();
            }

            public Part end()
            {
                return Part.this;
            }

            public class Face
                    implements IBuilderFace<OBJBuilder, OBJBuilder.Part, OBJBuilder.Part.Group, OBJBuilder.Part.Group.Face, OBJBuilder.Part.Group.Face.Vertex, SimpleMaterial>
            {
                List<Map<VertexFormatElement, Integer>> indices = Lists.newArrayList();
                List<String> vertices = Lists.newArrayList();

                private void addVertex(String formatIndices)
                {
                    vertices.add(formatIndices);
                }

                public Vertex vertex()
                {
                    Map<VertexFormatElement, Integer> indices0 = new HashMap<>();
                    indices.add(indices0);
                    return new Vertex(indices0);
                }

                public Group end()
                {
                    if (vertices.size() > 0)
                    {
                        lines.add("f " + String.join(" ", vertices));
                    }
                    return Group.this;
                }

                public class Vertex
                        implements IBuilderVertex<OBJBuilder, OBJBuilder.Part, OBJBuilder.Part.Group, OBJBuilder.Part.Group.Face, OBJBuilder.Part.Group.Face.Vertex, SimpleMaterial>
                {
                    private final Map<VertexFormatElement, Integer> indices0;

                    public Vertex(Map<VertexFormatElement, Integer> indices0)
                    {
                        this.indices0 = indices0;
                    }

                    public Vertex position(float x, float y, float z)
                    {
                        return element(DefaultVertexFormat.ELEMENT_POSITION, x, y, z);
                    }

                    public Vertex tex(float u, float v)
                    {
                        return element(DefaultVertexFormat.ELEMENT_UV0, u, v);
                    }

                    public Vertex normal(float x, float y, float z)
                    {
                        return element(DefaultVertexFormat.ELEMENT_NORMAL, x, y, z);
                    }

                    public Vertex element(VertexFormatElement element, double... values)
                    {
                        if (indices0.containsKey(element))
                            throw new IllegalStateException("This element has already been assigned!");

                        String prefix = getOrCreateElementPrefix(element);
                        if (prefix != null)
                        {
                            lines.add(String.format("%s %s", prefix, Arrays.stream(values).mapToObj(Double::toString).collect(Collectors.joining(" "))));
                            indices0.put(element, elementCounts.getOrDefault(element, 1));
                            elementCounts.compute(element, (key, val) -> val == null ? 2 : val + 1);
                        }
                        return this;
                    }

                    public Face end()
                    {
                        addVertex(formatIndices());
                        return Face.this;
                    }

                    private String formatIndices()
                    {
                        List<String> t = Lists.newArrayList();
                        for (VertexFormatElement element : elements)
                        {
                            Integer b = indices0.get(element);
                            if (b != null)
                            {
                                t.add(Integer.toString(b));
                            }
                            else
                            {
                                t.add("");
                            }
                        }
                        return String.join("/", t);
                    }
                }
            }
        }
    }
}
