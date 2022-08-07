package gigaherz.dumpmodel.builders;

public interface IModelFace<
        B extends IModelBuilder<B, P, G, F, V, M>,
        P extends IModelGroup<B, P, G, F, V, M>,
        G extends IModelMesh<B, P, G, F, V, M>,
        F extends IModelFace<B, P, G, F, V, M>,
        V extends IModelFaceVertex<B, P, G, F, V, M>,
        M extends IMaterial<M>>
{
    V vertex();

    G end();
}
