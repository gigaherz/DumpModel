package gigaherz.dumpmodel.builders;

import java.io.File;

public interface IModelBuilder<
        B extends IModelBuilder<B, P, G, F, V, M>,
        P extends IModelGroup<B, P, G, F, V, M>,
        G extends IModelMesh<B, P, G, F, V, M>,
        F extends IModelFace<B, P, G, F, V, M>,
        V extends IModelFaceVertex<B, P, G, F, V, M>,
        M extends IMaterial<M>>
{
    P part(String name);

    void save(File file);

    M newMaterial(String path);
    M newMaterial(String path, float r, float g, float b, float a);
}
