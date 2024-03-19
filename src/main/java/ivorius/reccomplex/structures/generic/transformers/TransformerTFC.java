/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.structures.generic.transformers;

import com.bioxx.tfc.WorldGen.DataLayer;
import com.google.gson.*;
import ivorius.ivtoolkit.blocks.BlockCoord;
import ivorius.ivtoolkit.random.WeightedSelector;
import ivorius.ivtoolkit.tools.MCRegistry;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.gui.editstructure.transformers.TableDataSourceBTReplace;
import ivorius.reccomplex.gui.editstructure.transformers.TableDataSourceBTTFC;
import ivorius.reccomplex.gui.table.TableDataSource;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.structures.StructureLoadContext;
import ivorius.reccomplex.structures.StructurePrepareContext;
import ivorius.reccomplex.structures.StructureSpawnContext;
import ivorius.reccomplex.structures.generic.WeightedBlockState;
import ivorius.reccomplex.structures.generic.matchers.BlockMatcher;
import ivorius.reccomplex.structures.generic.presets.WeightedBlockStatePresets;
import ivorius.reccomplex.utils.BlockStates;
import ivorius.reccomplex.utils.IBlockState;
import ivorius.reccomplex.utils.NBTNone;
import ivorius.reccomplex.utils.PresettedList;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by lukas on 25.05.14.
 */
public class TransformerTFC extends TransformerSingleBlock<NBTNone>
{
    public BlockMatcher sourceMatcher;

    public TransformerTFC(String sourceExpression)
    {
        this.sourceMatcher = new BlockMatcher(RecurrentComplex.specialRegistry, sourceExpression);
    }

    @Override
    public boolean matches(NBTNone instanceData, IBlockState state)
    {
        return sourceMatcher.apply(state);
    }

    @Override
    public void transformBlock(NBTNone instanceData, Phase phase, StructureSpawnContext context, BlockCoord coord, IBlockState sourceState, DataLayer[][] TFCDataLayers)
    {

    }

    @Override
    public NBTNone prepareInstanceData(StructurePrepareContext context)
    {
        return new NBTNone();
    }

    @Override
    public NBTNone loadInstanceData(StructureLoadContext context, NBTBase nbt)
    {
        return new NBTNone();
    }

    @Override
    public String getDisplayString()
    {
        return "TFC: " + sourceMatcher.getDisplayString();
    }

    @Override
    public TableDataSource tableDataSource(TableNavigator navigator, TableDelegate delegate)
    {
        return new TableDataSourceBTTFC(this);
    }

    @Override
    public boolean generatesInPhase(NBTNone instanceData, Phase phase)
    {
        return phase == Phase.BEFORE;
    }

    public static class Serializer implements JsonDeserializer<TransformerTFC>, JsonSerializer<TransformerTFC>
    {
        private MCRegistry registry;
        private Gson gson;

        public Serializer(MCRegistry registry)
        {
            this.registry = registry;
            gson = new GsonBuilder().registerTypeAdapter(WeightedBlockState.class, new WeightedBlockState.Serializer(registry)).create();
        }

        public static String readLegacyMatcher(JsonObject jsonObject, String blockKey, String metadataKey)
        {
            if (jsonObject.has(blockKey))
            {
                String sourceBlock = JsonUtils.getJsonObjectStringFieldValue(jsonObject, blockKey);
                int sourceMeta = JsonUtils.getJsonObjectIntegerFieldValueOrDefault(jsonObject, metadataKey, -1);
                return sourceMeta >= 0 ? String.format("%s & %s%d", sourceBlock, BlockMatcher.METADATA_PREFIX, sourceMeta) : sourceBlock;
            }

            return null;
        }

        @Override
        public TransformerTFC deserialize(JsonElement jsonElement, Type par2Type, JsonDeserializationContext context)
        {

            JsonObject jsonObject = JsonUtils.getJsonElementAsJsonObject(jsonElement, "transformerTFC");

            String expression = TransformerReplace.Serializer.readLegacyMatcher(jsonObject, "source", "sourceMetadata"); // Legacy
            if (expression == null)
                expression = JsonUtils.getJsonObjectStringFieldValueOrDefault(jsonObject, "sourceExpression", "");

            return new TransformerTFC(expression);
        }

        @Override
        public JsonElement serialize(TransformerTFC transformer, Type par2Type, JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("sourceExpression", transformer.sourceMatcher.getExpression());

            return jsonObject;
        }
    }
}
