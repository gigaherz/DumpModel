package gigaherz.dumpmodel.builders;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

public interface IBuilderPart<
        B extends IBuilder<B, P, G, F, V, M>,
        P extends IBuilderPart<B, P, G, F, V, M>,
        G extends IBuilderGroup<B, P, G, F, V, M>,
        F extends IBuilderFace<B, P, G, F, V, M>,
        V extends IBuilderVertex<B, P, G, F, V, M>,
        M extends IMaterial<M>>
{
    G group(@Nullable Direction side);

    G group(String name, @Nullable Direction side);

    B end();
}
