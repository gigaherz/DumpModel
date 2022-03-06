package gigaherz.dumpmodel.builders;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public interface IBuilderVertex<
        B extends IBuilder<B, P, G, F, V, M>,
        P extends IBuilderPart<B, P, G, F, V, M>,
        G extends IBuilderGroup<B, P, G, F, V, M>,
        F extends IBuilderFace<B, P, G, F, V, M>,
        V extends IBuilderVertex<B, P, G, F, V, M>,
        M extends IMaterial<M>>
{
    V position(float x, float y, float z);

    V tex(float u, float v);

    V normal(float x, float y, float z);

    V element(VertexFormatElement element, double... values);

    F end();
}
