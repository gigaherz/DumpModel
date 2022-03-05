package gigaherz.dumpmodel.builders;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public interface IBuilderVertex<
        B extends IBuilder<B, P, G, F, V>,
        P extends IBuilderPart<B, P, G, F, V>,
        G extends IBuilderGroup<B, P, G, F, V>,
        F extends IBuilderFace<B, P, G, F, V>,
        V extends IBuilderVertex<B, P, G, F, V>>
{
    V position(float x, float y, float z);

    V tex(float u, float v);

    V normal(float x, float y, float z);

    V element(VertexFormatElement element, double... values);

    F end();
}
