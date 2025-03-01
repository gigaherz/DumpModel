package gigaherz.dumpmodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class TransformingConsumer implements VertexConsumer
{
    private final VertexConsumer buffer;
    private final PoseStack.Pose pose;

    public TransformingConsumer(VertexConsumer buffer, PoseStack.Pose pose)
    {
        this.buffer = buffer;
        this.pose = pose;
    }

    @Override
    public VertexConsumer addVertex(float p_350761_, float p_350704_, float p_350711_)
    {
        return buffer.addVertex(pose, p_350761_, p_350704_, p_350711_);
    }

    @Override
    public VertexConsumer setNormal(float p_86005_, float p_86006_, float p_86007_)
    {
        return buffer.setNormal(pose, p_86005_, p_86006_, p_86007_);
    }

    @Override
    public VertexConsumer setColor(int p_85973_, int p_85974_, int p_85975_, int p_85976_)
    {
        return buffer.setColor(p_85973_, p_85974_, p_85975_, p_85976_);
    }

    @Override
    public VertexConsumer setUv(float p_85948_, float p_85949_)
    {
        return buffer.setUv(p_85948_, p_85949_);
    }

    @Override
    public VertexConsumer setUv1(int p_85971_, int p_85972_)
    {
        return buffer.setUv1(p_85971_, p_85972_);
    }

    @Override
    public VertexConsumer setUv2(int p_86010_, int p_86011_)
    {
        return buffer.setUv2(p_86010_, p_86011_);
    }
}
