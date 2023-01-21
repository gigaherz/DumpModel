package gigaherz.dumpmodel.builders;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjModelWriter extends ModelWriter<ObjModelWriter>
{
    public static ObjModelWriter begin()
    {
        return new ObjModelWriter();
    }

    private ObjModelWriter()
    {
    }

    private String formatIndices(ModelFaceVertex<ObjModelWriter> vtx)
    {
        boolean hasP = vtx.indices().containsKey(DefaultVertexFormat.ELEMENT_POSITION);
        boolean hasT = vtx.indices().containsKey(DefaultVertexFormat.ELEMENT_UV0);
        boolean hasN = vtx.indices().containsKey(DefaultVertexFormat.ELEMENT_NORMAL);

        var str = (hasP ? String.format(Locale.ROOT, "%d", vtx.indices().get(DefaultVertexFormat.ELEMENT_POSITION) + 1) : "");
        str += "/" + (hasT ? String.format(Locale.ROOT, "%d", vtx.indices().get(DefaultVertexFormat.ELEMENT_UV0) + 1) : "");
        str += "/" + (hasN ? String.format(Locale.ROOT, "%d", vtx.indices().get(DefaultVertexFormat.ELEMENT_NORMAL) + 1) : "");
        return str;
    }

    public void save(Path file)
    {
        var pathWithoutExtension = FilenameUtils.removeExtension(file.toFile().getAbsolutePath());
        var matLib = pathWithoutExtension + ".mtl";

        var matLibName = FilenameUtils.getName(matLib);

        try (OutputStream output = new FileOutputStream(file.toFile());
             OutputStreamWriter writer = new OutputStreamWriter(output))
        {
            if (materialLibrary().size() > 0)
            {
                writer.write(String.format("mtllib %s\n", matLibName));
            }

            Map<VertexFormatElement, Integer> counts = new HashMap<>();

            for(var group : groups())
            {
                writer.write(String.format("g %s\n", group.name()));

                for (var mesh : group.meshes())
                {
                    writer.write(String.format("o %s\n", mesh.name()));
                    writer.write(String.format("usemtl %s\n", mesh.material().name()));

                    // position + color
                    int cp = counts.getOrDefault(DefaultVertexFormat.ELEMENT_POSITION, 0);
                    int rp = mesh.requireElements().getOrDefault(DefaultVertexFormat.ELEMENT_POSITION, 0);

                    int cc = counts.getOrDefault(DefaultVertexFormat.ELEMENT_COLOR, 0);
                    int rc = mesh.requireElements().getOrDefault(DefaultVertexFormat.ELEMENT_COLOR, 0);

                    boolean doColors = (rc-cc) == (rp-cp);
                    for(int ip=cp, ic=cc; ip<rp;ip++, ic++)
                    {
                        var vp = elementDatas().get(DefaultVertexFormat.ELEMENT_POSITION).get(ip);
                        if (doColors)
                        {
                            var vc = elementDatas().get(DefaultVertexFormat.ELEMENT_COLOR).get(ic);
                            writer.write(String.format("v %f %f %f %f %f %f\n", vp[0], vp[1], vp[2], vc[0]/255.0f, vc[1]/255.0f, vc[2]/255.0f));
                        }
                        else
                        {
                            writer.write(String.format("v %f %f %f\n", vp[0], vp[1], vp[2]));
                        }
                    }
                    counts.put(DefaultVertexFormat.ELEMENT_POSITION, rp);
                    if(rc != cc) counts.put(DefaultVertexFormat.ELEMENT_COLOR, rc);

                    // tex coord
                    int ct = counts.getOrDefault(DefaultVertexFormat.ELEMENT_UV0, 0);
                    int rt = mesh.requireElements().getOrDefault(DefaultVertexFormat.ELEMENT_UV0, 0);

                    for(int i=ct; i<rt;i++)
                    {
                        var vt = elementDatas().get(DefaultVertexFormat.ELEMENT_UV0).get(i);
                        writer.write(String.format("vt %f %f\n", vt[0], 1-vt[1]));
                    }
                    counts.put(DefaultVertexFormat.ELEMENT_UV0, rp);

                    // normal
                    int cn = counts.getOrDefault(DefaultVertexFormat.ELEMENT_NORMAL, 0);
                    int rn = mesh.requireElements().getOrDefault(DefaultVertexFormat.ELEMENT_NORMAL, 0);

                    for(int i=cn; i<rn;i++)
                    {
                        var vn = elementDatas().get(DefaultVertexFormat.ELEMENT_NORMAL).get(i);
                        writer.write(String.format("vn %f %f %f\n", vn[0], vn[1], vn[2]));
                    }
                    counts.put(DefaultVertexFormat.ELEMENT_NORMAL, rp);

                    // faces
                    for(var face : mesh.faces())
                    {
                        var f = face.vertices().stream().map(this::formatIndices).collect(Collectors.joining(" "));
                        writer.write(String.format("f %s\n", f));
                    }
                }
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (materialLibrary().size() > 0)
        {
            try (OutputStream output = new FileOutputStream(matLib);
                 OutputStreamWriter writer = new OutputStreamWriter(output))
            {
                for(var mat : materialLibrary().values())
                {
                    writer.write(String.format("newmtl %s\n", mat.name()));
                    if (mat.texture() != null)
                    {
                        writer.write(String.format("map_Kd %s\n", mat.texture()));
                    }
                    else
                    {
                        writer.write("Kd 1 1 1\n");
                    }
                    /*else if (mat.r() != 1 || mat.g() != 1 || mat.b() != 1)
                    {
                        writer.write(String.format("Kd %s %s %s\n", mat.r(), mat.g(), mat.b()));
                    }
                    if (mat.a() != 1)
                    {
                        writer.write(String.format("opacity %s\n", mat.a()));
                    }*/
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

}
