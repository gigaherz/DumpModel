package gigaherz.dumpmodel.builders;

import com.google.common.collect.Lists;
import gigaherz.dumpmodel.Utils;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.Direction;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class OBJBuilder implements IBuilder<OBJBuilder, OBJBuilder.Part, OBJBuilder.Part.Group, OBJBuilder.Part.Group.Face, OBJBuilder.Part.Group.Face.Vertex>
{
    public static OBJBuilder begin()
    {
        return new OBJBuilder();
    }

    private final List<VertexFormatElement> elements = new ArrayList<>();
    private final Map<VertexFormatElement, String> elementPrefixes = new HashMap<>();
    private final Map<VertexFormatElement, Integer> elementCounts = new HashMap<>();
    private final List<String> lines = new ArrayList<>();

    private OBJBuilder()
    {
        defineElement("v", DefaultVertexFormats.POSITION_3F, true);
        defineElement("vt", DefaultVertexFormats.TEX_2F, true);
        defineElement("vt1", DefaultVertexFormats.TEX_2S, false);
        defineElement("vt2", DefaultVertexFormats.TEX_2SB, false);
        defineElement("vn", DefaultVertexFormats.NORMAL_3B, true);
        defineElement("vc", DefaultVertexFormats.COLOR_4UB, false);
    }

    private void defineElement(String id, VertexFormatElement element, boolean isStandard)
    {
        if (isStandard) elements.add(element);
        elementPrefixes.put(element, id);
    }

    private String getOrCreateElementPrefix(VertexFormatElement element)
    {
        return elementPrefixes.computeIfAbsent(element, e ->
        {
            String ename = e.getUsage().getDisplayName().replace(" ", "");
            if (e.getIndex() != 0)
                ename += e.getIndex();
            return ename;
        });
    }

    public Part part(String name)
    {
        return new Part(name);
    }

    public void save(File file)
    {
        try(OutputStream output = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(output))
        {
            writer.write(String.join("\n",lines));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public class Part implements IBuilderPart<OBJBuilder, OBJBuilder.Part, OBJBuilder.Part.Group, OBJBuilder.Part.Group.Face, OBJBuilder.Part.Group.Face.Vertex>
    {
        private Part(String name)
        {
            lines.add(String.format("o %s", name));
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

        public class Group implements IBuilderGroup<OBJBuilder, OBJBuilder.Part, OBJBuilder.Part.Group, OBJBuilder.Part.Group.Face, OBJBuilder.Part.Group.Face.Vertex>
        {
            public Group(String name, @Nullable Direction side)
            {
                lines.add(String.format("g %s", name));
                if (side != null)
                    lines.add(String.format("g_Side %s", side));
            }

            public Group addQuad(BakedQuad quad)
            {
                VertexFormat fmt = DefaultVertexFormats.BLOCK;
                int[] vdata = quad.getVertexData();
                int byteStart = 0;
                Face face = face();
                for(int i=0;i<4;i++)
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

            public class Face implements IBuilderFace<OBJBuilder, OBJBuilder.Part, OBJBuilder.Part.Group, OBJBuilder.Part.Group.Face, OBJBuilder.Part.Group.Face.Vertex>
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
                    if(vertices.size() > 0)
                    {
                        lines.add("f " + String.join(" ", vertices));
                    }
                    return Group.this;
                }

                public class Vertex implements IBuilderVertex<OBJBuilder, OBJBuilder.Part, OBJBuilder.Part.Group, OBJBuilder.Part.Group.Face, OBJBuilder.Part.Group.Face.Vertex>
                {
                    private final Map<VertexFormatElement, Integer> indices0;

                    public Vertex(Map<VertexFormatElement, Integer> indices0)
                    {
                        this.indices0 = indices0;
                    }

                    public Vertex position(float x, float y, float z)
                    {
                        return element(DefaultVertexFormats.POSITION_3F, x, y, z);
                    }

                    public Vertex tex(float u, float v)
                    {
                        return element(DefaultVertexFormats.TEX_2F, u, v);
                    }

                    public Vertex normal(float x, float y, float z)
                    {
                        return element(DefaultVertexFormats.NORMAL_3B, x, y, z);
                    }

                    public Vertex element(VertexFormatElement element, double... values)
                    {
                        if (indices0.containsKey(element))
                            throw new IllegalStateException("This element has already been assigned!");

                        String prefix = getOrCreateElementPrefix(element);
                        lines.add(String.format("%s %s", prefix, Arrays.stream(values).mapToObj(Double::toString).collect(Collectors.joining(" "))));
                        indices0.put(element, elementCounts.getOrDefault(element, 1));
                        elementCounts.compute(element, (key, val) -> val == null ? 2 : val+1);
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
