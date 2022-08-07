package gigaherz.dumpmodel.builders;

import net.minecraft.client.renderer.block.model.BakedQuad;

public interface IModelMesh<
        B extends IModelBuilder<B, P, G, F, V, M>,
        P extends IModelGroup<B, P, G, F, V, M>,
        G extends IModelMesh<B, P, G, F, V, M>,
        F extends IModelFace<B, P, G, F, V, M>,
        V extends IModelFaceVertex<B, P, G, F, V, M>,
        M extends IMaterial<M>>
{
    G addQuad(BakedQuad quad);

    F face();

    P end();

    G setMaterial(M texName);
}
