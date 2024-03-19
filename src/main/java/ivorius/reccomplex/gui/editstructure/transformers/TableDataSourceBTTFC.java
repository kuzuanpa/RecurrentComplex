/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editstructure.transformers;

import ivorius.reccomplex.gui.GuiValidityStateIndicator;
import ivorius.reccomplex.gui.TableDataSourceExpression;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.structures.generic.transformers.TransformerNatural;
import ivorius.reccomplex.structures.generic.transformers.TransformerTFC;
import ivorius.reccomplex.utils.IvTranslations;
import ivorius.reccomplex.utils.scale.Scales;
import net.minecraft.block.Block;

/**
 * Created by lukas on 05.06.14.
 */
public class TableDataSourceBTTFC extends TableDataSourceSegmented implements TableCellPropertyListener
{
    private TransformerTFC transformer;

    public TableDataSourceBTTFC(TransformerTFC transformer)
    {
        this.transformer = transformer;

        addManagedSection(0, TableDataSourceExpression.constructDefault("Sources", transformer.sourceMatcher));
    }

    public static TableCellString elementForBlock(String id, Block block)
    {
        TableCellString element = new TableCellString(id, Block.blockRegistry.getNameForObject(block));
        element.setShowsValidityState(true);
        setStateForBlockTextfield(element);
        return element;
    }

    public static void setStateForBlockTextfield(TableCellString elementString)
    {
        elementString.setValidityState(stateForBlock(elementString.getPropertyValue()));
    }

    public static GuiValidityStateIndicator.State stateForBlock(String blockID)
    {
        return Block.blockRegistry.containsKey(blockID) ? GuiValidityStateIndicator.State.VALID : GuiValidityStateIndicator.State.INVALID;
    }

    public TransformerTFC getTransformer()
    {
        return transformer;
    }

    public void setTransformer(TransformerTFC transformer)
    {
        this.transformer = transformer;
    }

    @Override
    public int numberOfSegments()
    {
        return 2;
    }

    @Override
    public int sizeOfSegment(int segment)
    {
        return segment == 1 ? 2 : super.sizeOfSegment(segment);
    }

    @Override
    public TableElement elementForIndexInSegment(GuiTable table, int index, int segment)
    {
        if (segment == 1)
        {
            switch (index)
            {
            }
        }

        return super.elementForIndexInSegment(table, index, segment);
    }

    @Override
    public void valueChanged(TableCellPropertyDefault cell)
    {
        if (cell.getID() != null)
        {
            switch (cell.getID())
            {
            }
        }
    }
}
