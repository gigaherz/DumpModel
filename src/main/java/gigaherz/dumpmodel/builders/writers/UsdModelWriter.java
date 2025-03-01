package gigaherz.dumpmodel.builders.writers;

import com.mojang.blaze3d.vertex.VertexFormatElement;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class UsdModelWriter extends ModelWriter<UsdModelWriter>
{
    public static UsdModelWriter begin()
    {
        return new UsdModelWriter();
    }

    private UsdModelWriter()
    {
    }

    @Override
    public void save(Path file)
    {
        try (OutputStream output = new FileOutputStream(file.toFile());
             OutputStreamWriter writer = new OutputStreamWriter(output))
        {
            writePreamble(writer);

            writeMaterials(writer, file);

            writeScene(writer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void writePreamble(OutputStreamWriter writer) throws IOException
    {
        writer.write("#usda 1.0\n");
        writer.write("(\n");
        writer.write("    doc = \"DumpModel for Minecraft Forge v1.0\"\n");
        writer.write("    metersPerUnit = 1\n");
        writer.write("    upAxis = \"Z\"\n");
        writer.write(")\n");
        writer.write("\n");
        writer.write("def Xform \"Camera\"\n");
        writer.write("{\n");
        writer.write("    matrix4d xformOp:transform = ( (0.6859206557273865, 0.7276763319969177, 0, 0), (-0.32401347160339355, 0.305420845746994, 0.8953956365585327, 0), (0.6515582203865051, -0.6141703724861145, 0.44527140259742737, 0), (7.358891487121582, -6.925790786743164, 4.958309173583984, 1) )\n");
        writer.write("    uniform token[] xformOpOrder = [\"xformOp:transform\"]\n");
        writer.write("\n");
        writer.write("    def Camera \"Camera\"\n");
        writer.write("    {\n");
        writer.write("        float2 clippingRange = (0.1, 100)\n");
        writer.write("        float focalLength = 50\n");
        writer.write("        float horizontalAperture = 36\n");
        writer.write("        float horizontalApertureOffset = 0\n");
        writer.write("        token projection = \"perspective\"\n");
        writer.write("        float verticalAperture = 20.25\n");
        writer.write("        float verticalApertureOffset = 0\n");
        writer.write("    }\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("def Xform \"Light\"\n");
        writer.write("{\n");
        writer.write("    matrix4d xformOp:transform = ( (-0.29086464643478394, 0.9551711678504944, -0.05518905818462372, 0), (-0.7711008191108704, -0.1998833566904068, 0.6045247316360474, 0), (0.5663931965827942, 0.21839119493961334, 0.7946722507476807, 0), (4.076245307922363, 1.0054539442062378, 5.903861999511719, 1) )\n");
        writer.write("    uniform token[] xformOpOrder = [\"xformOp:transform\"]\n");
        writer.write("\n");
        writer.write("    def DistantLight \"Light\"\n");
        writer.write("    {\n");
        writer.write("        color3f inputs:color = (1, 1, 1)\n");
        writer.write("        float inputs:intensity = 1\n");
        writer.write("        float inputs:specular = 1\n");
        writer.write("    }\n");
        writer.write("}\n");
        writer.write("\n");
    }

    private void writeScene(OutputStreamWriter writer) throws IOException
    {
        var posDatas = this.elementDatas().get(VertexFormatElement.POSITION);
        var nrmDatas = this.elementDatas().get(VertexFormatElement.NORMAL);
        var uv0Datas = this.elementDatas().get(VertexFormatElement.UV0);
        var clrDatas = this.elementDatas().get(VertexFormatElement.COLOR);

        for (var group : groups())
        {
            writer.write("def Xform \"" + group.name().replaceAll("[^a-zA-Z0-9_]", "_") + "\"\n");
            writer.write("{\n");
            writer.write("    matrix4d xformOp:transform = ( (1, 0, 0, 0), (0, 1, 0, 0), (0, 0, 1, 0), (0, 0, 0, 1) )\n");
            writer.write("    uniform token[] xformOpOrder = [\"xformOp:transform\"]\n");
            writer.write("\n");
            for (var mesh : group.meshes())
            {
                writer.write("    def Mesh \"" + mesh.name().replaceAll("[^a-zA-Z0-9_]", "_") + "\"\n");
                writer.write("    {\n");
                writer.write("        uniform bool doubleSided = 1\n");

                var counts = new ArrayList<Integer>();
                var indices = new ArrayList<Integer>();
                var positions = new ArrayList<double[]>();
                var normals = new ArrayList<double[]>();
                var uvs = new ArrayList<double[]>();
                var colors = new ArrayList<double[]>();
                for (var face : mesh.faces())
                {
                    var vts = face.vertices();
                    if (vts.size() < 3) continue;
                    counts.add(vts.size());
                    for (var vtx : vts)
                    {
                        var ix = vtx.indices();
                        indices.add(positions.size());
                        positions.add(posDatas.get(ix.get(VertexFormatElement.POSITION)));
                        normals.add(nrmDatas.get(ix.get(VertexFormatElement.NORMAL)));
                        uvs.add(uv0Datas.get(ix.get(VertexFormatElement.UV0)));

                        var clr = clrDatas != null ? clrDatas.get(ix.get(VertexFormatElement.COLOR)) : new double[]{1, 1, 1, 1};

                        clr[0] *= (1 / 255.0f);
                        clr[1] *= (1 / 255.0f);
                        clr[2] *= (1 / 255.0f);
                        clr[3] *= (1 / 255.0f);

                        colors.add(clr);
                    }
                }

                writer.write("        int[] faceVertexCounts = [" + counts.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]\n");
                writer.write("        int[] faceVertexIndices = [" + indices.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]\n");

                writer.write("        rel material:binding = </_materials/" + mesh.material().name() + ">\n");

                writer.write("        normal3f[] normals = [" + normals.stream().map(n -> String.format("(%f, %f, %f)", n[0], -n[2], n[1])).collect(Collectors.joining(", ")) + "] (\n");
                writer.write("            interpolation = \"faceVarying\"\n");
                writer.write("        )\n");

                writer.write("        point3f[] points = [" + positions.stream().map(n -> String.format("(%f, %f, %f)", n[0], -n[2], n[1])).collect(Collectors.joining(", ")) + "]\n");

                writer.write("        texCoord2f[] primvars:UVMap = [" + uvs.stream().map(n -> String.format("(%f, %f)", n[0], n[1])).collect(Collectors.joining(", ")) + "] (\n");
                writer.write("            interpolation = \"faceVarying\"\n");
                writer.write("        )\n");

                writer.write("        color4f[] colors = [" + colors.stream().map(n -> String.format("(%f, %f, %f, %f)", n[1], n[2], n[3], n[0])).collect(Collectors.joining(", ")) + "] (\n");
                writer.write("            interpolation = \"faceVarying\"\n");
                writer.write("        )\n");

                writer.write("        uniform token subdivisionScheme = \"none\"\n");
                writer.write("    }\n");
                writer.write("\n");
            }
            writer.write("}\n");
            writer.write("\n");
        }
    }

    private void writeMaterials(OutputStreamWriter writer, Path file) throws IOException
    {
        writer.write("def \"_materials\"\n");
        writer.write("{\n");

        for (var mat : materialLibrary().values())
        {
            String matName = mat.name().replaceAll("[^a-zA-Z0-9_]", "_");
            writer.write("    def Material \"" + matName + "\"\n");
            writer.write("    {\n");
            writer.write("        def Scope \"preview\"\n");
            writer.write("        {\n");
            writer.write("            def Shader \"uvmap\"\n");
            writer.write("            {\n");
            writer.write("                uniform token info:id = \"UsdPrimvarReader_float2\"\n");
            writer.write("                token inputs:varname = \"UVMap\"\n");
            writer.write("                float2 outputs:result\n");
            writer.write("            }\n");
            writer.write("\n");
            writer.write("            def Shader \"Image_Texture\"\n");
            writer.write("            {\n");
            writer.write("                uniform token info:id = \"UsdUVTexture\"\n");

            var tx = file.getParent().relativize(Paths.get(mat.mat().texture()));

            writer.write("                asset inputs:file = @." + File.separator + tx.toString() + "@\n");
            writer.write("                token inputs:sourceColorSpace = \"sRGB\"\n");
            writer.write("                float2 inputs:st.connect = </_materials/" + matName + "/preview/uvmap.outputs:result>\n");
            writer.write("                float3 outputs:rgb\n");
            writer.write("            }\n");
            writer.write("\n");
            writer.write("            def Shader \"Diffuse_BSDF\"\n");
            writer.write("            {\n");
            writer.write("                uniform token info:id = \"UsdPreviewSurface\"\n");
            writer.write("                float3 inputs:diffuseColor.connect = </_materials/" + matName + "/preview/Image_Texture.outputs:rgb>\n");
            writer.write("                float inputs:roughness = 0.5\n");
            writer.write("                token outputs:surface\n");
            writer.write("            }\n");
            writer.write("        }\n");
            writer.write("\n");
            writer.write("        token outputs:surface.connect = </_materials/" + matName + "/preview/Diffuse_BSDF.outputs:surface>\n");
            writer.write("    }\n");
        }

        writer.write("}\n");
    }
}
