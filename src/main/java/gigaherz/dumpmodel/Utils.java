package gigaherz.dumpmodel;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import gigaherz.dumpmodel.builders.BOBBuilder;
import gigaherz.dumpmodel.builders.OBJBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Random;

public class Utils
{
    private static Direction[] DIRECTIONS = {
            null, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN
    };

    public static void dumpToOBJ(File file, String name, BakedModel model, @Nullable BlockState state, IModelData modelData)
    {
        OBJBuilder builder = OBJBuilder.begin();

        OBJBuilder.Part part = builder.part(name);

        for (Direction dir : DIRECTIONS)
        {
            OBJBuilder.Part.Group group = part.group(dir);

            for (BakedQuad quad : model.getQuads(state, dir, new Random(42), modelData))
            {
                group.addQuad(quad);
            }
        }

        builder.save(file);
    }

    public static void dumpToBOB(File file, String name, BakedModel model, @Nullable BlockState state, IModelData modelData)
    {
        BOBBuilder builder = BOBBuilder.begin();

        BOBBuilder.Part part = builder.part(name);

        for (Direction dir : DIRECTIONS)
        {
            BOBBuilder.Part.Group group = part.group(dir);

            for (BakedQuad quad : model.getQuads(state, dir, new Random(42), modelData))
            {
                group.addQuad(quad);
            }
        }

        builder.save(file);
    }

    public static String formatIndices(List<Integer> indices, boolean hasTex)
    {
        if (indices.size() == 1)
            return String.format("%d", indices.get(0));
        if (indices.size() == 2 && hasTex)
            return String.format("%d/%d", indices.get(0), indices.get(1));
        if (indices.size() == 2)
            return String.format("%d//%d", indices.get(0), indices.get(1));
        if (indices.size() >= 3)
            return String.format("%d/%d/%d", indices.get(0), indices.get(1), indices.get(2));
        return "1";
    }

    public static double[] extractData(int[] data, int byteStart, VertexFormatElement element)
    {
        switch (element.getType())
        {
            case FLOAT:
                switch (element.getElementCount())
                {
                    case 1:
                        return arrayOf(getFloatAt(data, byteStart));
                    case 2:
                        return arrayOf(getFloatAt(data, byteStart), getFloatAt(data, byteStart + 4));
                    case 3:
                        return arrayOf(getFloatAt(data, byteStart), getFloatAt(data, byteStart + 4), getFloatAt(data, byteStart + 8));
                    case 4:
                        return arrayOf(getFloatAt(data, byteStart), getFloatAt(data, byteStart + 4), getFloatAt(data, byteStart + 8), getFloatAt(data, byteStart + 12));
                }
                break;
            case UBYTE:
                switch (element.getElementCount())
                {
                    case 1:
                        return arrayOf(getUByteAt(data, byteStart) / 255.0f);
                    case 2:
                        return arrayOf(getUByteAt(data, byteStart) / 255.0f, getUByteAt(data, byteStart + 1) / 255.0f);
                    case 3:
                        return arrayOf(getUByteAt(data, byteStart) / 255.0f, getUByteAt(data, byteStart + 1) / 255.0f, getUByteAt(data, byteStart + 2) / 255.0f);
                    case 4:
                        return arrayOf(getUByteAt(data, byteStart) / 255.0f, getUByteAt(data, byteStart + 1) / 255.0f, getUByteAt(data, byteStart + 2) / 255.0f, getUByteAt(data, byteStart + 3) / 255.0f);
                }
                break;
            case BYTE:
                switch (element.getElementCount())
                {
                    case 1:
                        return arrayOf(getByteAt(data, byteStart) / 127.0f);
                    case 2:
                        return arrayOf(getByteAt(data, byteStart) / 127.0f, getByteAt(data, byteStart + 1) / 127.0f);
                    case 3:
                        return arrayOf(getByteAt(data, byteStart) / 127.0f, getByteAt(data, byteStart + 1) / 127.0f, getByteAt(data, byteStart + 2) / 127.0f);
                    case 4:
                        return arrayOf(getByteAt(data, byteStart) / 127.0f, getByteAt(data, byteStart + 1) / 127.0f, getByteAt(data, byteStart + 2) / 127.0f, getByteAt(data, byteStart + 3) / 127.0f);
                }
                break;
            case USHORT:
                switch (element.getElementCount())
                {
                    case 1:
                        return arrayOf(getUShortAt(data, byteStart));
                    case 2:
                        return arrayOf(getUShortAt(data, byteStart), getUShortAt(data, byteStart + 2));
                    case 3:
                        return arrayOf(getUShortAt(data, byteStart), getUShortAt(data, byteStart + 2), getUShortAt(data, byteStart + 4));
                    case 4:
                        return arrayOf(getUShortAt(data, byteStart), getUShortAt(data, byteStart + 2), getUShortAt(data, byteStart + 4), getUShortAt(data, byteStart + 6));
                }
                break;
            case SHORT:
                switch (element.getElementCount())
                {
                    case 1:
                        return arrayOf(getShortAt(data, byteStart));
                    case 2:
                        return arrayOf(getShortAt(data, byteStart), getShortAt(data, byteStart + 2));
                    case 3:
                        return arrayOf(getShortAt(data, byteStart), getShortAt(data, byteStart + 2), getShortAt(data, byteStart + 4));
                    case 4:
                        return arrayOf(getShortAt(data, byteStart), getShortAt(data, byteStart + 2), getShortAt(data, byteStart + 4), getShortAt(data, byteStart + 6));
                }
                break;
            case UINT:
                switch (element.getElementCount())
                {
                    case 1:
                        return arrayOf(getUIntAt(data, byteStart));
                    case 2:
                        return arrayOf(getUIntAt(data, byteStart), getUIntAt(data, byteStart + 4));
                    case 3:
                        return arrayOf(getUIntAt(data, byteStart), getUIntAt(data, byteStart + 4), getUIntAt(data, byteStart + 8));
                    case 4:
                        return arrayOf(getUIntAt(data, byteStart), getUIntAt(data, byteStart + 4), getUIntAt(data, byteStart + 8), getUIntAt(data, byteStart + 12));
                }
                break;
            case INT:
                switch (element.getElementCount())
                {
                    case 1:
                        return arrayOf(getIntAt(data, byteStart));
                    case 2:
                        return arrayOf(getIntAt(data, byteStart), getIntAt(data, byteStart + 4));
                    case 3:
                        return arrayOf(getIntAt(data, byteStart), getIntAt(data, byteStart + 4), getIntAt(data, byteStart + 8));
                    case 4:
                        return arrayOf(getIntAt(data, byteStart), getIntAt(data, byteStart + 4), getIntAt(data, byteStart + 8), getIntAt(data, byteStart + 12));
                }
                break;
        }
        return new double[0];
    }

    public static double[] arrayOf(double... floats)
    {
        return floats;
    }

    public static ListTag listOf(double... floats)
    {
        ListTag ret = new ListTag();
        for (double v : floats)
        {
            ret.add(FloatTag.valueOf((float) v));
        }
        return ret;
    }

    private static float getFloatAt(int[] data, int n)
    {
        return Float.intBitsToFloat(getIntAt(data, n));
    }

    private static long getUIntAt(int[] data, int n)
    {
        return (getUByteAt(data, n + 3) << 24) + (getUByteAt(data, n + 2) << 16) + (getUByteAt(data, n + 1) << 8) + getUByteAt(data, n);
    }

    private static int getIntAt(int[] data, int n)
    {
        return (getByteAt(data, n + 3) << 24) + (getUByteAt(data, n + 2) << 16) + (getUByteAt(data, n + 1) << 8) + getUByteAt(data, n);
    }

    private static int getUShortAt(int[] data, int n)
    {
        return (getUByteAt(data, n + 1) << 8) + getUByteAt(data, n);
    }

    private static int getShortAt(int[] data, int n)
    {
        return (getByteAt(data, n + 1) << 8) + getUByteAt(data, n);
    }

    private static int getUByteAt(int[] data, int n)
    {
        int idx = n / 4;
        int off = (n % 4) * 8;
        return (data[idx] >> off) & 0xFF;
    }

    private static int getByteAt(int[] data, int n)
    {
        int idx = n / 4;
        int off = 24 - (n % 4) * 8;
        return (data[idx] << off) >> 24;
    }
}
