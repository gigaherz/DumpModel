package gigaherz.dumpmodel.builders;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

public interface IModelGroup<
        B extends IModelBuilder<B, P, G, F, V, M>,
        P extends IModelGroup<B, P, G, F, V, M>,
        G extends IModelMesh<B, P, G, F, V, M>,
        F extends IModelFace<B, P, G, F, V, M>,
        V extends IModelFaceVertex<B, P, G, F, V, M>,
        M extends IMaterial<M>>
{
    G group(@Nullable Direction side);

    G group(String name, @Nullable Direction side);

    B end();
}
