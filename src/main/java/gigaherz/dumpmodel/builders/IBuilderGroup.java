package gigaherz.dumpmodel.builders;

import net.minecraft.client.renderer.block.model.BakedQuad;

public interface IBuilderGroup<
        B extends IBuilder<B, P, G, F, V>,
        P extends IBuilderPart<B, P, G, F, V>,
        G extends IBuilderGroup<B, P, G, F, V>,
        F extends IBuilderFace<B, P, G, F, V>,
        V extends IBuilderVertex<B, P, G, F, V>>
{
    G addQuad(BakedQuad quad);

    F face();

    P end();
}
