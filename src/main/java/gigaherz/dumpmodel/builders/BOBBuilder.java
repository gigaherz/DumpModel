package gigaherz.dumpmodel.builders;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import gigaherz.dumpmodel.Utils;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BOBBuilder
        implements IBuilder<BOBBuilder, BOBBuilder.Part, BOBBuilder.Part.Group, BOBBuilder.Part.Group.Face, BOBBuilder.Part.Group.Face.Vertex>
{
    public static BOBBuilder begin()
    {
        return new BOBBuilder();
    }

    private final List<VertexFormatElement> elements = new ArrayList<>();
    private final Map<VertexFormatElement, ListTag> elementLists = new HashMap<>();
    private final CompoundTag data = new CompoundTag();
    private final CompoundTag meta;
    private final CompoundTag materials;
    private final ListTag parts;
    private final ListTag lists;

    private BOBBuilder()
    {

        this.meta = new CompoundTag();
        data.put("meta", meta); // TODO

        this.materials = new CompoundTag();
        data.put("materials", materials);

        this.lists = new ListTag();
        data.put("lists", lists);

        this.parts = new ListTag();
        data.put("parts", parts);

        defineElement("v", DefaultVertexFormat.ELEMENT_POSITION);
        defineElement("vt", DefaultVertexFormat.ELEMENT_UV0);
        defineElement("vn", DefaultVertexFormat.ELEMENT_NORMAL);
    }

    private void defineElement(String id, VertexFormatElement element)
    {
        elements.add(element);
        elementLists.put(element, createElementList(id));
    }

    private ListTag getOrCreateElementList(VertexFormatElement element)
    {
        return elementLists.computeIfAbsent(element, e ->
        {
            elements.add(e);
            String ename = e.getUsage().getName().replace(" ", "");
            if (e.getIndex() != 0)
                ename += e.getIndex();
            return createElementList(ename);
        });
    }

    private ListTag createElementList(String id)
    {
        ListTag list = new ListTag();
        CompoundTag listData = new CompoundTag();
        listData.putString("id", id);
        listData.put("v", list);
        lists.add(listData);
        return list;
    }

    @Override
    public Part part(String name)
    {
        CompoundTag part = new CompoundTag();
        parts.add(part);
        return new Part(part, name);
    }

    @Override
    public void save(File file)
    {
        try
        {
            NbtIo.write(data, file);
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
            NbtIo.writeCompressed(data, file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public class Part
            implements IBuilderPart<BOBBuilder, BOBBuilder.Part, BOBBuilder.Part.Group, BOBBuilder.Part.Group.Face, BOBBuilder.Part.Group.Face.Vertex>
    {
        private final ListTag groups;

        private Part(CompoundTag part, String name)
        {
            part.putString("n", name);
            this.groups = new ListTag();
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
            CompoundTag group = new CompoundTag();
            groups.add(group);
            return new Group(group, name, side);
        }

        @Override
        public BOBBuilder end()
        {
            return BOBBuilder.this;
        }

        public class Group
                implements IBuilderGroup<BOBBuilder, BOBBuilder.Part, BOBBuilder.Part.Group, BOBBuilder.Part.Group.Face, BOBBuilder.Part.Group.Face.Vertex>
        {
            private final ListTag faces;

            public Group(CompoundTag group, String name, @Nullable Direction side)
            {
                group.putString("n", name);
                group.putInt("d", side == null ? -1 : side.ordinal());

                this.faces = new ListTag();
                group.put("f", faces);
            }

            @Override
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
                ListTag face = new ListTag();
                faces.add(face);
                return new Face(face);
            }

            @Override
            public Part end()
            {
                return Part.this;
            }

            public class Face
                    implements IBuilderFace<BOBBuilder, BOBBuilder.Part, BOBBuilder.Part.Group, BOBBuilder.Part.Group.Face, BOBBuilder.Part.Group.Face.Vertex>
            {
                private final ListTag vertices;

                public Face(ListTag face)
                {
                    this.vertices = new ListTag();
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

                public class Vertex
                        implements IBuilderVertex<BOBBuilder, BOBBuilder.Part, BOBBuilder.Part.Group, BOBBuilder.Part.Group.Face, BOBBuilder.Part.Group.Face.Vertex>
                {
                    private final Map<VertexFormatElement, Integer> indices0 = new HashMap<>();

                    @Override
                    public Vertex position(float x, float y, float z)
                    {
                        return element(DefaultVertexFormat.ELEMENT_POSITION, x, y, z);
                    }

                    @Override
                    public Vertex tex(float u, float v)
                    {
                        return element(DefaultVertexFormat.ELEMENT_UV0, u, v);
                    }

                    @Override
                    public Vertex normal(float x, float y, float z)
                    {
                        return element(DefaultVertexFormat.ELEMENT_NORMAL, x, y, z);
                    }

                    @Override
                    public Vertex element(VertexFormatElement element, double... values)
                    {
                        if (indices0.containsKey(element))
                            throw new IllegalStateException("This element has already been assigned!");

                        ListTag list = getOrCreateElementList(element);

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

                    private ListTag formatIndices()
                    {
                        ListTag t = new ListTag();
                        for (VertexFormatElement element : elements)
                        {
                            Integer b = indices0.get(element);
                            if (b != null)
                            {
                                t.add(IntTag.valueOf(b));
                            }
                            else
                            {
                                t.add(IntTag.valueOf(-1));
                            }
                        }
                        return t;
                    }
                }
            }
        }
    }
}
