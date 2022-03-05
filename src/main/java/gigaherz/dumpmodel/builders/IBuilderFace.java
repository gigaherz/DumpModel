package gigaherz.dumpmodel.builders;

public interface IBuilderFace<
        B extends IBuilder<B, P, G, F, V>,
        P extends IBuilderPart<B, P, G, F, V>,
        G extends IBuilderGroup<B, P, G, F, V>,
        F extends IBuilderFace<B, P, G, F, V>,
        V extends IBuilderVertex<B, P, G, F, V>>
{
    V vertex();

    G end();
}
