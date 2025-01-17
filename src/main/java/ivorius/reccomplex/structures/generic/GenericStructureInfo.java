/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.structures.generic;

import com.bioxx.tfc.Core.TFC_Climate;
import com.bioxx.tfc.Core.TFC_Core;
import com.bioxx.tfc.WorldGen.DataLayer;
import com.bioxx.tfc.WorldGen.TFCProvider;
import com.bioxx.tfc.api.TFCBlocks;
import com.google.gson.*;
import cpw.mods.fml.common.FMLLog;
import ivorius.ivtoolkit.blocks.BlockCoord;
import ivorius.ivtoolkit.blocks.IvBlockCollection;
import ivorius.ivtoolkit.tools.IvWorldData;
import ivorius.ivtoolkit.tools.NBTTagLists;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.blocks.GeneratingTileEntity;
import ivorius.reccomplex.blocks.RCBlocks;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.json.NbtToJson;
import ivorius.reccomplex.structures.*;
import ivorius.reccomplex.structures.generic.gentypes.MazeGenerationInfo;
import ivorius.reccomplex.structures.generic.gentypes.NaturalGenerationInfo;
import ivorius.reccomplex.structures.generic.gentypes.StructureGenerationInfo;
import ivorius.reccomplex.structures.generic.matchers.BlockMatcher;
import ivorius.reccomplex.structures.generic.matchers.DependencyMatcher;
import ivorius.reccomplex.structures.generic.transformers.*;
import ivorius.reccomplex.utils.*;
import ivorius.ivtoolkit.tools.Pairs;
import ivorius.reccomplex.worldgen.inventory.InventoryGenerationHandler;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static com.bioxx.tfc.WorldGen.TFCChunkProviderGenerate.*;
import static com.bioxx.tfc.WorldGen.TFCChunkProviderGenerate.rockLayer3;

/**
 * Created by lukas on 24.05.14.
 */
public class GenericStructureInfo implements StructureInfo<GenericStructureInfo.InstanceData>, Cloneable
{
    public static final int LATEST_VERSION = 3;
    public static final int MAX_GENERATING_LAYERS = 30;
    public final List<StructureGenerationInfo> generationInfos = new ArrayList<>();
    public final List<Transformer> transformers = new ArrayList<>();
    public final DependencyMatcher dependencies = new DependencyMatcher("");
    public NBTTagCompound worldDataCompound;
    public boolean rotatable;
    public boolean mirrorable;
    public Metadata metadata = new Metadata();
    public JsonObject customData;

    public static GenericStructureInfo createDefaultStructure()
    {
        GenericStructureInfo genericStructureInfo = new GenericStructureInfo();
        genericStructureInfo.rotatable = false;
        genericStructureInfo.mirrorable = false;

        genericStructureInfo.transformers.add(new TransformerNaturalAir(BlockMatcher.of(RecurrentComplex.specialRegistry, RCBlocks.genericSpace, 1), TransformerNaturalAir.DEFAULT_NATURAL_EXPANSION_DISTANCE, TransformerNaturalAir.DEFAULT_NATURAL_EXPANSION_RANDOMIZATION));
        genericStructureInfo.transformers.add(new TransformerNegativeSpace(BlockMatcher.of(RecurrentComplex.specialRegistry, RCBlocks.genericSpace, 0)));
        genericStructureInfo.transformers.add(new TransformerNatural(BlockMatcher.of(RecurrentComplex.specialRegistry, RCBlocks.genericSolid, 0), TransformerNatural.DEFAULT_NATURAL_EXPANSION_DISTANCE, TransformerNatural.DEFAULT_NATURAL_EXPANSION_RANDOMIZATION));
        genericStructureInfo.transformers.add(new TransformerReplace(BlockMatcher.of(RecurrentComplex.specialRegistry, RCBlocks.genericSolid, 1)).replaceWith(new WeightedBlockState(null, BlockStates.fromMetadata(Blocks.air, 0), "")));

        genericStructureInfo.generationInfos.add(new NaturalGenerationInfo());

        return genericStructureInfo;
    }

    private static boolean isBiomeAllTypes(BiomeGenBase biomeGenBase, List<BiomeDictionary.Type> types)
    {
        for (BiomeDictionary.Type type : types)
        {
            if (!BiomeDictionary.isBiomeOfType(biomeGenBase, type))
                return false;
        }

        return true;
    }

    @Override
    public int[] structureBoundingBox()
    {
        if (worldDataCompound == null)
            return new int[]{0, 0, 0};

        NBTTagCompound compound = worldDataCompound.getCompoundTag("blockCollection");
        return new int[]{compound.getInteger("width"), compound.getInteger("height"), compound.getInteger("length")};
    }

    @Override
    public boolean isRotatable()
    {
        return rotatable;
    }
    @Override
    public boolean isMirrorable()
    {
        return mirrorable;
    }
    public DataLayer defaultIfNull(DataLayer[] nullCheck){return nullCheck==null||nullCheck[18] == null ? DataLayer.GRANITE : nullCheck[18];}

    public IvBlockCollection transformVanillaBlocksToTFC(IvBlockCollection originalBlocks, World world, BlockCoord structureLowCoord,DataLayer[][] dataLayers){
        try {
            int i = TFC_Core.getRockLayerFromHeight(world, structureLowCoord.x, structureLowCoord.y, structureLowCoord.z);
            DataLayer layer = i == 0 ? defaultIfNull(dataLayers[0]) : i == 1 ? defaultIfNull(dataLayers[1]) : defaultIfNull(dataLayers[2]);
            Block grass            = TFC_Core.getTypeForGrassWithRain(layer.data1, dataLayers[4][18] == null ? DataLayer.RAIN_125.floatdata1 : dataLayers[4][18].floatdata1);
            Block dirt             = TFC_Core.getBuildingDirt(TFC_Core.getTypeForDirtFromGrass(grass));
            Block stone            = TFC_Core.getBuildingStone(layer.block);
            Block stoneSmooth      = TFC_Core.getSmoothStone(layer.block);
            Block stoneCobble      = TFC_Core.getBuildingCobble(layer.block);
            Block stoneBrick       = TFC_Core.getStoneBrick(layer.block);
            Block stoneChiseled    = stoneSmooth;
            Block stoneCrackBrick  = stoneBrick;
            Block stoneCobbleMossy = stoneCobble;
            Block stoneMossyBrick  = stoneBrick;
            Block wallCobble       = TFC_Core.getCobbleWall(layer.block);

            Block sand = TFC_Core.getTypeForSand(layer.data1);
            Block gravel = TFC_Core.getTypeForGravel(layer.data1);
            originalBlocks.forEach(blockCoord->{
                     if(originalBlocks.getBlock(blockCoord)==Blocks.stone                                                    ){originalBlocks.setBlock(blockCoord,stone);               originalBlocks.setMetadata(blockCoord,(byte)layer.data2);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.dirt                                                     ){originalBlocks.setBlock(blockCoord,dirt);                originalBlocks.setMetadata(blockCoord,(byte)layer.data1);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.sand                                                     ){originalBlocks.setBlock(blockCoord,sand);                originalBlocks.setMetadata(blockCoord,(byte)layer.data1);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.gravel                                                   ){originalBlocks.setBlock(blockCoord,gravel);              originalBlocks.setMetadata(blockCoord,(byte)layer.data1);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.cobblestone                                              ){originalBlocks.setBlock(blockCoord,stoneCobble);         originalBlocks.setMetadata(blockCoord,(byte)layer.data2);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.stonebrick && originalBlocks.getMetadata(blockCoord) == 0){originalBlocks.setBlock(blockCoord,stoneBrick);          originalBlocks.setMetadata(blockCoord,(byte)layer.data2);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.stonebrick && originalBlocks.getMetadata(blockCoord) == 1){originalBlocks.setBlock(blockCoord,stoneMossyBrick);     originalBlocks.setMetadata(blockCoord,(byte)layer.data2);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.stonebrick && originalBlocks.getMetadata(blockCoord) == 2){originalBlocks.setBlock(blockCoord,stoneCrackBrick);     originalBlocks.setMetadata(blockCoord,(byte)layer.data2);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.stonebrick && originalBlocks.getMetadata(blockCoord) == 3){originalBlocks.setBlock(blockCoord,stoneChiseled);       originalBlocks.setMetadata(blockCoord,(byte)layer.data2);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.cobblestone_wall                                         ){originalBlocks.setBlock(blockCoord,wallCobble);          originalBlocks.setMetadata(blockCoord,(byte)layer.data2);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.mossy_cobblestone                                        ){originalBlocks.setBlock(blockCoord,stoneCobbleMossy);    originalBlocks.setMetadata(blockCoord,(byte)layer.data2);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.grass                                                    ){originalBlocks.setBlock(blockCoord,grass);               originalBlocks.setMetadata(blockCoord,(byte)layer.data1);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.tallgrass  && originalBlocks.getMetadata(blockCoord) == 1){originalBlocks.setBlock(blockCoord, TFCBlocks.tallGrass);originalBlocks.setMetadata(blockCoord,(byte) 0);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.tallgrass  && originalBlocks.getMetadata(blockCoord) == 2){originalBlocks.setBlock(blockCoord, TFCBlocks.tallGrass);originalBlocks.setMetadata(blockCoord,(byte) 1);}
                else if(originalBlocks.getBlock(blockCoord)==Blocks.vine                                                     ){originalBlocks.setBlock(blockCoord, TFCBlocks.vine);}
            });
        }catch (NullPointerException e){
            e.printStackTrace();
            return null;
        }
        return originalBlocks;
    }

    @Override
    public void generate(final StructureSpawnContext context, InstanceData instanceData)
    {
        World world = context.world;
        Random random = context.random;
        IvWorldData worldData = constructWorldData(world);
        DataLayer[][] data = null;

        // The world initializes the block event array after it generates the world - in the constructor
        // This hackily sets the field to a temporary value. Yay.
        if (world instanceof WorldServer)
            RCAccessorWorldServer.ensureBlockEventArray((WorldServer) world); // Hax

        IvBlockCollection blockCollection = worldData.blockCollection;
        if(world.provider instanceof TFCProvider){
            if (TFC_Climate.getCacheManager(world) == null) return;

            DataLayer[] rockLayer1i = TFC_Climate.getCacheManager(world).loadRockLayerGeneratorData(rockLayer1, context.lowerCoord().x,context.lowerCoord().z, 16, 16, 0);
            DataLayer[] rockLayer2i = TFC_Climate.getCacheManager(world).loadRockLayerGeneratorData(rockLayer2, context.lowerCoord().x,context.lowerCoord().z, 16, 16, 1);
            DataLayer[] rockLayer3i = TFC_Climate.getCacheManager(world).loadRockLayerGeneratorData(rockLayer3, context.lowerCoord().x,context.lowerCoord().z, 16, 16, 2);
            DataLayer[] evtLayeri = TFC_Climate.getCacheManager(world).loadEVTLayerGeneratorData(evtLayer, context.lowerCoord().x,context.lowerCoord().z, 16, 16);
            DataLayer[] rainfallLayeri = TFC_Climate.getCacheManager(world).loadRainfallLayerGeneratorData(rainfallLayer, context.lowerCoord().x,context.lowerCoord().z, 16, 16);
            DataLayer[] stabilityLayeri = TFC_Climate.getCacheManager(world).loadStabilityLayerGeneratorData(stabilityLayer, context.lowerCoord().x,context.lowerCoord().z, 16, 16);
            DataLayer[] drainageLayeri = TFC_Climate.getCacheManager(world).loadDrainageLayerGeneratorData(drainageLayer, context.lowerCoord().x,context.lowerCoord().z, 16, 16);

            data= new DataLayer[][]{rockLayer1i,rockLayer2i,rockLayer3i,evtLayeri,rainfallLayeri,stabilityLayeri,drainageLayeri};
            blockCollection= transformVanillaBlocksToTFC(blockCollection,world,context.lowerCoord(),data);
        }
        //if any Exception happened when doing TFC transform, cancel the generates.
        if(blockCollection==null)return;
        int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
        BlockCoord origin = context.lowerCoord();

        Map<BlockCoord, TileEntity> tileEntities = new HashMap<>();
        for (TileEntity tileEntity : worldData.tileEntities)
        {
            BlockCoord key = new BlockCoord(tileEntity);
            tileEntities.put(key, tileEntity);
            IvWorldData.setTileEntityPosForGeneration(tileEntity, context.transform.apply(key, areaSize).add(origin));
        }

        List<Pair<Transformer, NBTStorable>> transformers = Pairs.of(this.transformers, instanceData.transformers);

        if (!context.generateAsSource)
        {
            for (Pair<Transformer, NBTStorable> pair : transformers)
            {
                Transformer transformer = pair.getLeft();
                NBTStorable transformerData = pair.getRight();
                if (transformer.generatesInPhase(transformerData, Transformer.Phase.BEFORE))
                    transformer.transform(transformerData, Transformer.Phase.BEFORE, context, worldData, transformers, data);
            }
        }

        for (int pass = 0; pass < 2; pass++)
        {
            for (BlockCoord sourceCoord : blockCollection)
            {
                IBlockState state = BlockStates.at(blockCollection, sourceCoord);

                BlockCoord worldPos = context.transform.apply(sourceCoord, areaSize).add(origin);
                if (context.includes(worldPos) && RecurrentComplex.specialRegistry.isSafe(state.getBlock()))
                {
                    TileEntity tileEntity = tileEntities.get(sourceCoord);

                    if (pass == getPass(state) && (context.generateAsSource || !skips(transformers, state)))
                    {
                        if (context.generateAsSource || !(tileEntity instanceof GeneratingTileEntity) || ((GeneratingTileEntity) tileEntity).shouldPlaceInWorld(context, instanceData.tileEntities.get(sourceCoord)))
                        {
                            if (context.setBlock(worldPos, state) && world.getBlock(worldPos.x, worldPos.y, worldPos.z) == state.getBlock())
                            {
                                if (tileEntity != null && RecurrentComplex.specialRegistry.isSafe(tileEntity))
                                {
                                    world.setBlockMetadataWithNotify(worldPos.x, worldPos.y, worldPos.z, BlockStates.getMetadata(state), 2); // TODO Figure out why some blocks (chests, furnace) need this

                                    world.setTileEntity(worldPos.x, worldPos.y, worldPos.z, tileEntity);
                                    tileEntity.updateContainingBlockInfo();


                                    if (!context.generateAsSource)
                                    {
                                        if (tileEntity instanceof IInventory)
                                        {
                                            IInventory inventory = (IInventory) tileEntity;
                                            InventoryGenerationHandler.generateAllTags(inventory, RecurrentComplex.specialRegistry.itemHidingMode(), random);
                                        }
                                    }
                                }
                                context.transform.rotateBlock(world, worldPos, state.getBlock());
                            }
                        }
                        else
                            context.setBlock(worldPos, BlockStates.defaultState(Blocks.air)); // Replace with air
                    }
                }
            }
        }

        if (!context.generateAsSource)
        {
            for (Pair<Transformer, NBTStorable> pair : transformers)
            {
                Transformer transformer = pair.getLeft();
                NBTStorable transformerData = pair.getRight();
                if (transformer.generatesInPhase(transformerData, Transformer.Phase.AFTER))
                    transformer.transform(transformerData, Transformer.Phase.AFTER, context, worldData, transformers, data);
            }
        }

        for (Entity entity : worldData.entities)
        {
            IvWorldData.transformEntityPosForGeneration(entity, context.transform, areaSize);
            IvWorldData.moveEntityForGeneration(entity, origin);

            if (context.includes(entity.posX, entity.posY, entity.posZ))
            {
                RCAccessorEntity.setEntityUniqueID(entity, UUID.randomUUID());
                world.spawnEntityInWorld(entity);
            }
        }

        if (!context.generateAsSource && context.generationLayer < MAX_GENERATING_LAYERS)
        {
            tileEntities.entrySet().stream().filter(entry -> entry.getValue() instanceof GeneratingTileEntity).forEach(entry -> ((GeneratingTileEntity) entry.getValue()).generate(context, instanceData.tileEntities.get(entry.getKey())));
        }
        else
        {
            RecurrentComplex.logger.warn("Structure generated with over " + MAX_GENERATING_LAYERS + " layers; most likely infinite loop!");
        }
    }

    @Override
    public InstanceData prepareInstanceData(StructurePrepareContext context)
    {
        InstanceData instanceData = new InstanceData();

        if (!context.generateAsSource)
        {
            IvWorldData worldData = constructWorldData(null);
            IvBlockCollection blockCollection = worldData.blockCollection;

            int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
            BlockCoord origin = context.lowerCoord();

            instanceData.transformers.addAll(transformers.stream().map(transformer -> transformer.prepareInstanceData(context)).collect(Collectors.toList()));

            worldData.tileEntities.stream().filter(tileEntity -> tileEntity instanceof GeneratingTileEntity).forEach(tileEntity -> {
                BlockCoord key = new BlockCoord(tileEntity);
                IvWorldData.setTileEntityPosForGeneration(tileEntity, context.transform.apply(key, areaSize).add(origin));
                instanceData.tileEntities.put(key, (NBTStorable) ((GeneratingTileEntity) tileEntity).prepareInstanceData(context));
            });
        }

        return instanceData;
    }

    @Override
    public InstanceData loadInstanceData(StructureLoadContext context, final NBTBase nbt)
    {
        InstanceData instanceData = new InstanceData();
        instanceData.readFromNBT(context, nbt, transformers, constructWorldData(null));
        return instanceData;
    }

    private boolean skips(List<Pair<Transformer, NBTStorable>> transformers, final IBlockState state)
    {
        return transformers.stream().anyMatch(input -> input.getLeft().skipGeneration(input.getRight(), state));
    }

    public IvWorldData constructWorldData(World world)
    {
        return new IvWorldData(worldDataCompound, world, RecurrentComplex.specialRegistry.itemHidingMode());
    }

    @Override
    public <I extends StructureGenerationInfo> List<I> generationInfos(Class<I> clazz)
    {
        return generationInfos.stream().filter(info -> clazz.isAssignableFrom(info.getClass())).map(info -> (I) info).collect(Collectors.toList());
    }

    @Override
    public StructureGenerationInfo generationInfo(String id)
    {
        for (StructureGenerationInfo info : generationInfos)
        {
            if (Objects.equals(info.id(), id))
                return info;
        }

        return null;
    }

    private int getPass(IBlockState state)
    {
        return (state.getBlock().isNormalCube() || state.getBlock().getMaterial() == Material.air) ? 0 : 1;
    }

    @Override
    public GenericStructureInfo copyAsGenericStructureInfo()
    {
        return copy();
    }

    @Override
    public boolean areDependenciesResolved()
    {
        return dependencies.apply();
    }

    @Override
    public String toString()
    {
        String s = StructureRegistry.INSTANCE.structureID(this);
        return s != null ? s : "Generic Structure";
    }

    public GenericStructureInfo copy()
    {
        GenericStructureInfo genericStructureInfo = StructureRegistry.INSTANCE.createStructureFromJSON(StructureRegistry.INSTANCE.createJSONFromStructure(this));
        genericStructureInfo.worldDataCompound = (NBTTagCompound) worldDataCompound.copy();
        return genericStructureInfo;
    }

    public static class Serializer implements JsonDeserializer<GenericStructureInfo>, JsonSerializer<GenericStructureInfo>
    {
        public GenericStructureInfo deserialize(JsonElement jsonElement, Type par2Type, JsonDeserializationContext context)
        {
            JsonObject jsonobject = JsonUtils.getJsonElementAsJsonObject(jsonElement, "status");
            GenericStructureInfo structureInfo = new GenericStructureInfo();

            Integer version;
            if (jsonobject.has("version"))
            {
                version = JsonUtils.getJsonObjectIntegerFieldValue(jsonobject, "version");
            }
            else
            {
                version = LATEST_VERSION;
                RecurrentComplex.logger.warn("Structure JSON missing 'version', using latest (" + LATEST_VERSION + ")");
            }

            if (jsonobject.has("generationInfos"))
                Collections.addAll(structureInfo.generationInfos, context.<StructureGenerationInfo[]>deserialize(jsonobject.get("generationInfos"), StructureGenerationInfo[].class));

            if (version == 1)
                structureInfo.generationInfos.add(NaturalGenerationInfo.deserializeFromVersion1(jsonobject, context));

            {
                // Legacy version 2
                if (jsonobject.has("naturalGenerationInfo"))
                    structureInfo.generationInfos.add(NaturalGenerationInfo.getGson().fromJson(jsonobject.get("naturalGenerationInfo"), NaturalGenerationInfo.class));

                if (jsonobject.has("mazeGenerationInfo"))
                    structureInfo.generationInfos.add(MazeGenerationInfo.getGson().fromJson(jsonobject.get("mazeGenerationInfo"), MazeGenerationInfo.class));
            }

            if (jsonobject.has("transformers"))
                Collections.addAll(structureInfo.transformers, context.<Transformer[]>deserialize(jsonobject.get("transformers"), Transformer[].class));
            if (jsonobject.has("blockTransformers")) // Legacy
                Collections.addAll(structureInfo.transformers, context.<Transformer[]>deserialize(jsonobject.get("blockTransformers"), Transformer[].class));

            structureInfo.rotatable = JsonUtils.getJsonObjectBooleanFieldValueOrDefault(jsonobject, "rotatable", false);
            structureInfo.mirrorable = JsonUtils.getJsonObjectBooleanFieldValueOrDefault(jsonobject, "mirrorable", false);

            if (jsonobject.has("dependencyExpression"))
                structureInfo.dependencies.setExpression(JsonUtils.getJsonObjectStringFieldValue(jsonobject, "dependencyExpression"));
            else if (jsonobject.has("dependencies")) // Legacy
                structureInfo.dependencies.setExpression(DependencyMatcher.ofMods(context.<String[]>deserialize(jsonobject.get("dependencies"), String[].class)));

            if (jsonobject.has("worldData"))
                structureInfo.worldDataCompound = context.deserialize(jsonobject.get("worldData"), NBTTagCompound.class);
            else if (jsonobject.has("worldDataBase64"))
                structureInfo.worldDataCompound = NbtToJson.getNBTFromBase64(JsonUtils.getJsonObjectStringFieldValue(jsonobject, "worldDataBase64"));
            // And else it is taken out for packet size, or stored in the zip

            if (jsonobject.has("metadata")) // Else, use default
                structureInfo.metadata = context.deserialize(jsonobject.get("metadata"), Metadata.class);

            structureInfo.customData = JsonUtils.getJsonObjectFieldOrDefault(jsonobject, "customData", new JsonObject());

            return structureInfo;
        }

        public JsonElement serialize(GenericStructureInfo structureInfo, Type par2Type, JsonSerializationContext context)
        {
            JsonObject jsonobject = new JsonObject();

            jsonobject.addProperty("version", LATEST_VERSION);

            jsonobject.add("generationInfos", context.serialize(structureInfo.generationInfos));
            jsonobject.add("transformers", context.serialize(structureInfo.transformers));

            jsonobject.addProperty("rotatable", structureInfo.rotatable);
            jsonobject.addProperty("mirrorable", structureInfo.mirrorable);

            jsonobject.add("dependencyExpression", context.serialize(structureInfo.dependencies.getExpression()));

            if (!RecurrentComplex.USE_ZIP_FOR_STRUCTURE_FILES && structureInfo.worldDataCompound != null)
            {
                if (RecurrentComplex.USE_JSON_FOR_NBT)
                    jsonobject.add("worldData", context.serialize(structureInfo.worldDataCompound));
                else
                    jsonobject.addProperty("worldDataBase64", NbtToJson.getBase64FromNBT(structureInfo.worldDataCompound));
            }

            jsonobject.add("metadata", context.serialize(structureInfo.metadata));
            jsonobject.add("customData", structureInfo.customData);

            return jsonobject;
        }
    }

    public static class InstanceData implements NBTStorable
    {
        public static final String KEY_TRANSFORMERS = "transformers";
        public static final String KEY_TILE_ENTITIES = "tileEntities";

        public final List<NBTStorable> transformers = new ArrayList<>();
        public final Map<BlockCoord, NBTStorable> tileEntities = new HashMap<>();

        protected static NBTBase getTileEntityTag(NBTTagCompound tileEntityCompound, BlockCoord coord)
        {
            return tileEntityCompound.getTag(getTileEntityKey(coord));
        }

        private static String getTileEntityKey(BlockCoord coord)
        {
            return String.format("%d,%d,%d", coord.x, coord.y, coord.z);
        }

        public void readFromNBT(StructureLoadContext context, NBTBase nbt, List<Transformer> transformers, IvWorldData worldData)
        {
            IvBlockCollection blockCollection = worldData.blockCollection;
            NBTTagCompound compound = nbt instanceof NBTTagCompound ? (NBTTagCompound) nbt : new NBTTagCompound();

            List<NBTTagCompound> transformerCompounds = NBTTagLists.compoundsFrom(compound, KEY_TRANSFORMERS);
            for (int i = 0; i < transformerCompounds.size(); i++)
            {
                NBTTagCompound transformerCompound = transformerCompounds.get(i);
                this.transformers.add(transformers.get(i).loadInstanceData(context, transformerCompound.getTag("data")));
            }

            int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
            BlockCoord origin = context.lowerCoord();

            NBTTagCompound tileEntityCompound = compound.getCompoundTag(InstanceData.KEY_TILE_ENTITIES);
            worldData.tileEntities.stream().filter(tileEntity -> tileEntity instanceof GeneratingTileEntity).forEach(tileEntity -> {
                BlockCoord key = new BlockCoord(tileEntity);
                IvWorldData.setTileEntityPosForGeneration(tileEntity, context.transform.apply(key, areaSize).add(origin));
                tileEntities.put(key, (NBTStorable) ((GeneratingTileEntity) tileEntity).loadInstanceData(context, getTileEntityTag(tileEntityCompound, key)));
            });
        }

        @Override
        public NBTBase writeToNBT()
        {
            NBTTagCompound compound = new NBTTagCompound();

            NBTTagList transformerDatas = new NBTTagList();
            for (NBTStorable transformerData : this.transformers)
            {
                NBTTagCompound transformerCompound = new NBTTagCompound();
                transformerCompound.setTag("data", transformerData.writeToNBT());
                transformerDatas.appendTag(transformerCompound);
            }
            compound.setTag(KEY_TRANSFORMERS, transformerDatas);

            NBTTagCompound tileEntityCompound = new NBTTagCompound();
            for (Map.Entry<BlockCoord, NBTStorable> entry : tileEntities.entrySet())
                tileEntityCompound.setTag(getTileEntityKey(entry.getKey()), entry.getValue().writeToNBT());
            compound.setTag(KEY_TILE_ENTITIES, tileEntityCompound);

            return compound;
        }
    }
}
