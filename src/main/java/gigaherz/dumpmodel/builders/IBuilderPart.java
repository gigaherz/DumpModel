package gigaherz.dumpmodel.builders;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

public interface IBuilderPart<
        B extends IBuilder<B, P, G, F, V>,
        P extends IBuilderPart<B, P, G, F, V>,
        G extends IBuilderGroup<B, P, G, F, V>,
        F extends IBuilderFace<B, P, G, F, V>,
        V extends IBuilderVertex<B, P, G, F, V>>
{
    G group(@Nullable Direction side);

    G group(String name, @Nullable Direction side);

    B end();
}
