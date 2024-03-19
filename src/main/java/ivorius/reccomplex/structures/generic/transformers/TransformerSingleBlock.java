/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.structures.generic.transformers;

import com.bioxx.tfc.WorldGen.DataLayer;
import ivorius.ivtoolkit.blocks.BlockCoord;
import ivorius.ivtoolkit.blocks.IvBlockCollection;
import ivorius.ivtoolkit.tools.IvWorldData;
import ivorius.reccomplex.structures.StructureSpawnContext;
import ivorius.reccomplex.utils.IBlockState;
import ivorius.reccomplex.utils.BlockStates;
import ivorius.reccomplex.utils.NBTStorable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Created by lukas on 17.09.14.
 */
public abstract class TransformerSingleBlock<S extends NBTStorable> implements Transformer<S>
{
    @Override
    public boolean skipGeneration(S instanceData, IBlockState state)
    {
        return matches(instanceData, state);
    }

    @Override
    public void transform(S instanceData, Phase phase, StructureSpawnContext context, IvWorldData worldData, List<Pair<Transformer, NBTStorable>> transformers, DataLayer[][] TFCDataLayers)
    {
        IvBlockCollection blockCollection = worldData.blockCollection;
        int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
        BlockCoord lowerCoord = context.lowerCoord();

        for (BlockCoord sourceCoord : blockCollection)
        {
            BlockCoord worldCoord = context.transform.apply(sourceCoord, areaSize).add(lowerCoord);

            if (context.includes(worldCoord))
            {
                IBlockState state = BlockStates.at(blockCollection, sourceCoord);

                if (matches(instanceData, state))
                    transformBlock(instanceData, Phase.BEFORE, context, worldCoord, state, TFCDataLayers);
            }
        }
    }

    public abstract boolean matches(S instanceData, IBlockState state);

    public abstract void transformBlock(S instanceData, Phase phase, StructureSpawnContext context, BlockCoord coord, IBlockState sourceState, DataLayer[][] TFCDataLayers);
}
