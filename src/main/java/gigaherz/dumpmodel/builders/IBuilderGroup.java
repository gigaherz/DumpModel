package gigaherz.dumpmodel.builders;

import net.minecraft.client.renderer.block.model.BakedQuad;

public interface IBuilderGroup<
        B extends IBuilder<B, P, G, F, V, M>,
        P extends IBuilderPart<B, P, G, F, V, M>,
        G extends IBuilderGroup<B, P, G, F, V, M>,
        F extends IBuilderFace<B, P, G, F, V, M>,
        V extends IBuilderVertex<B, P, G, F, V, M>,
        M extends IMaterial<M>>
{
    G addQuad(BakedQuad quad);

    F face();

    P end();

    G setMaterial(M texName);
}
