package gigaherz.dumpmodel.builders;

import java.io.File;

public interface IBuilder<
        B extends IBuilder<B, P, G, F, V, M>,
        P extends IBuilderPart<B, P, G, F, V, M>,
        G extends IBuilderGroup<B, P, G, F, V, M>,
        F extends IBuilderFace<B, P, G, F, V, M>,
        V extends IBuilderVertex<B, P, G, F, V, M>,
        M extends IMaterial<M>>
{
    P part(String name);

    void save(File file);

    M newMaterial(String path);
}
