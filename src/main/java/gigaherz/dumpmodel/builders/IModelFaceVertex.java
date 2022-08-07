package gigaherz.dumpmodel.builders;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public interface IModelFaceVertex<
        B extends IModelBuilder<B, P, G, F, V, M>,
        P extends IModelGroup<B, P, G, F, V, M>,
        G extends IModelMesh<B, P, G, F, V, M>,
        F extends IModelFace<B, P, G, F, V, M>,
        V extends IModelFaceVertex<B, P, G, F, V, M>,
        M extends IMaterial<M>>
{
    V position(double x, double y, double z);

    V tex(double u, double v);

    V normal(double x, double y, double z);

    V color(double r, double g, double b, double a);

    V element(VertexFormatElement element, double... values);

    F end();
}
