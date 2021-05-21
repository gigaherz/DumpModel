package gigaherz.dumpmodel.builders;

import com.google.common.collect.Lists;
import gigaherz.dumpmodel.Utils;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BOBBuilder implements IBuilder<BOBBuilder, BOBBuilder.Part, BOBBuilder.Part.Group, BOBBuilder.Part.Group.Face, BOBBuilder.Part.Group.Face.Vertex>
{
    public static BOBBuilder begin()
    {
        return new BOBBuilder();
    }

    private final List<VertexFormatElement> elements = new ArrayList<>();
    private final Map<VertexFormatElement, ListNBT> elementLists = new HashMap<>();
    private final CompoundNBT data = new CompoundNBT();
    private final CompoundNBT meta;
    private final CompoundNBT materials;
    private final ListNBT parts;
    private final ListNBT lists;

    private BOBBuilder()
    {

        this.meta = new CompoundNBT();
        data.put("meta", meta); // TODO

        this.materials = new CompoundNBT();
        data.put("materials", materials);

        this.lists = new ListNBT();
        data.put("lists", lists);

        this.parts = new ListNBT();
        data.put("parts", parts);

        defineElement("v", DefaultVertexFormats.POSITION_3F);
        defineElement("vt", DefaultVertexFormats.TEX_2F);
        defineElement("vn", DefaultVertexFormats.NORMAL_3B);
    }

    private void defineElement(String id, VertexFormatElement element)
    {
        elements.add(element);
        elementLists.put(element, createElementList(id));
    }

    private ListNBT getOrCreateElementList(VertexFormatElement element)
    {
        return elementLists.computeIfAbsent(element, e ->
        {
            elements.add(e);
            String ename = e.getUsage().getDisplayName().replace(" ", "");
            if (e.getIndex() != 0)
                ename += e.getIndex();
            return createElementList(ename);
        });
    }

    private ListNBT createElementList(String id)
    {
        ListNBT list = new ListNBT();
        CompoundNBT listData = new CompoundNBT();
        listData.putString("id", id);
        listData.put("v", list);
        lists.add(listData);
        return list;
    }

    @Override
    public Part part(String name)
    {
        CompoundNBT part = new CompoundNBT();
        parts.add(part);
        return new Part(part, name);
    }

    @Override
    public void save(File file)
    {
        try
        {
            CompressedStreamTools.write(data, file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void saveCompressed(File file)
    {
        try
        {
            CompressedStreamTools.writeCompressed(data, file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public class Part implements IBuilderPart<BOBBuilder, BOBBuilder.Part, BOBBuilder.Part.Group, BOBBuilder.Part.Group.Face, BOBBuilder.Part.Group.Face.Vertex>
    {
        private final ListNBT groups;

        private Part(CompoundNBT part, String name)
        {
            part.putString("n", name);
            this.groups = new ListNBT();
            part.put("g", groups);
        }

        @Override
        public Group group(@Nullable Direction side)
        {
            return group(side == null ? "general" : side.toString(), side);
        }

        @Override
        public Group group(String name, @Nullable Direction side)
        {
            CompoundNBT group = new CompoundNBT();
            groups.add(group);
            return new Group(group, name, side);
        }

        @Override
        public BOBBuilder end()
        {
            return BOBBuilder.this;
        }

        public class Group implements IBuilderGroup<BOBBuilder, BOBBuilder.Part, BOBBuilder.Part.Group, BOBBuilder.Part.Group.Face, BOBBuilder.Part.Group.Face.Vertex>
        {
            private final ListNBT faces;

            public Group(CompoundNBT group, String name, @Nullable Direction side)
            {
                group.putString("n", name);
                group.putInt("d", side == null ? -1 : side.ordinal());

                this.faces = new ListNBT();
                group.put("f", faces);
            }

            @Override
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
                            double[] elementValues0 = Utils.extractData(vdata, byteStart, element);
                            vertex.element(element, elementValues0);
                        }
                        byteStart += element.getType().getSize() * element.getElementCount();
                    }
                    vertex.end();
                }
                return face.end();
            }

            @Override
            public Face face()
            {
                ListNBT face = new ListNBT();
                faces.add(face);
                return new Face(face);
            }

            @Override
            public Part end()
            {
                return Part.this;
            }

            public class Face implements IBuilderFace<BOBBuilder, BOBBuilder.Part, BOBBuilder.Part.Group, BOBBuilder.Part.Group.Face, BOBBuilder.Part.Group.Face.Vertex>
            {
                private final ListNBT vertices;

                public Face(ListNBT face)
                {
                    this.vertices = new ListNBT();
                    face.add(vertices);
                }

                @Override
                public Vertex vertex()
                {
                    return new Vertex();
                }

                @Override
                public Group end()
                {
                    return Group.this;
                }

                public class Vertex implements IBuilderVertex<BOBBuilder, BOBBuilder.Part, BOBBuilder.Part.Group, BOBBuilder.Part.Group.Face, BOBBuilder.Part.Group.Face.Vertex>
                {
                    private final Map<VertexFormatElement, Integer> indices0 = new HashMap<>();

                    @Override
                    public Vertex position(float x, float y, float z)
                    {
                         return element(DefaultVertexFormats.POSITION_3F, x, y, z);
                    }

                    @Override
                    public Vertex tex(float u, float v)
                    {
                        return element(DefaultVertexFormats.TEX_2F, u, v);
                    }

                    @Override
                    public Vertex normal(float x, float y, float z)
                    {
                        return element(DefaultVertexFormats.NORMAL_3B, x, y, z);
                    }

                    @Override
                    public Vertex element(VertexFormatElement element, double... values)
                    {
                        if (indices0.containsKey(element))
                            throw new IllegalStateException("This element has already been assigned!");

                        ListNBT list = getOrCreateElementList(element);

                        indices0.put(element, list.size());
                        list.add(Utils.listOf(values));

                        return this;
                    }

                    @Override
                    public Face end()
                    {
                        vertices.add(formatIndices());
                        return Face.this;
                    }

                    private ListNBT formatIndices()
                    {
                        ListNBT t = new ListNBT();
                        for (VertexFormatElement element : elements)
                        {
                            Integer b = indices0.get(element);
                            if (b != null)
                            {
                                t.add(IntNBT.valueOf(b));
                            }
                            else
                            {
                                t.add(IntNBT.valueOf(-1));
                            }
                        }
                        return t;
                    }
                }
            }
        }
    }
}
