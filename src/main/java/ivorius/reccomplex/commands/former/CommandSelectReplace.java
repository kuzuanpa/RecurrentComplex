/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.former;

import ivorius.ivtoolkit.blocks.BlockStates;
import ivorius.ivtoolkit.world.MockWorld;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.capability.SelectionOwner;
import ivorius.reccomplex.commands.CommandVirtual;
import ivorius.reccomplex.commands.RCCommands;
import ivorius.reccomplex.commands.parameters.*;
import ivorius.reccomplex.utils.expression.PositionedBlockExpression;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by lukas on 09.06.14.
 */
public class CommandSelectReplace extends CommandExpecting implements CommandVirtual
{
    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "replace";
    }

    @Override
    public Expect<?> expect()
    {
        return RCExpect.expectRC()
                .block().descriptionU("destination block").required()
                .block().descriptionU("source expression").required().repeat()
                .named("metadata", "m")
                .metadata();
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public void execute(MockWorld world, ICommandSender sender, String[] args) throws CommandException
    {
        RCParameters parameters = RCParameters.of(args, expect()::declare);

        Block dstBlock = parameters.get(0).block(sender).require();
        int[] dstMeta = parameters.get("metadata").metadatas().optional().orElse(new int[1]);
        List<IBlockState> dst = IntStream.of(dstMeta).mapToObj(m -> BlockStates.fromMetadata(dstBlock, m)).collect(Collectors.toList());

        PositionedBlockExpression matcher = parameters.get(1).rest(ParameterString.join()).expression(new PositionedBlockExpression(RecurrentComplex.specialRegistry)).require();

        SelectionOwner selectionOwner = RCCommands.getSelectionOwner(sender, null, true);
        RCCommands.assertSize(sender, selectionOwner);

        for (BlockPos coord : selectionOwner.getSelection())
        {
            if (matcher.evaluate(() -> PositionedBlockExpression.Argument.at(world, coord)))
            {
                IBlockState state = dst.get(world.rand().nextInt(dst.size()));
                world.setBlockState(coord, state, 3);
            }
        }
    }

}