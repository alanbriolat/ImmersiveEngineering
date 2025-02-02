/*
 * BluSunrize
 * Copyright (c) 2019
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.metal;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.WireApi;
import blusunrize.immersiveengineering.common.IETileTypes;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

public class FeedthroughBlock extends ConnectorBlock<FeedthroughTileEntity>
{
	public FeedthroughBlock()
	{
		super("feedthrough", IETileTypes.FEEDTHROUGH);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		// TODO Axis instead of FACING_ALL?
		builder.add(IEProperties.FACING_ALL, BlockStateProperties.WATERLOGGED);
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving)
	{
		BlockEntity tile = world.getBlockEntity(pos);
		if(tile instanceof FeedthroughTileEntity&&!world.isClientSide&&newState.getBlock()!=state.getBlock())
		{
			FeedthroughTileEntity feedthrough = (FeedthroughTileEntity)tile;
			if(!feedthrough.currentlyDisassembling)
			{
				Direction dir = feedthrough.getFacing();
				//TODO sign is probably wrong somewhere
				BlockPos centerPos = pos.relative(dir, -feedthrough.offset);
				//Middle block last, since that has the actual connections
				for(int offset : new int[]{-1, 1, 0})
				{
					if(offset==feedthrough.offset)
						continue;
					BlockPos posForOffset = centerPos.relative(dir, offset);
					BlockEntity tileAtOffset = world.getBlockEntity(posForOffset);
					if(tileAtOffset instanceof FeedthroughTileEntity)
						((FeedthroughTileEntity)tileAtOffset).currentlyDisassembling = true;
					if(offset==0)
						world.setBlockAndUpdate(posForOffset, feedthrough.stateForMiddle);
					else
					{
						BlockState connector = WireApi.INFOS.get(feedthrough.reference).conn.get()
								.setValue(IEProperties.FACING_ALL, offset < 0?dir: dir.getOpposite());
						ConnectionPoint cpOnFeedthrough = new ConnectionPoint(centerPos,
								FeedthroughTileEntity.getIndexForOffset(offset));
						GlobalWireNetwork global = GlobalWireNetwork.getNetwork(world);
						List<Connection> removedConnections = new ArrayList<>();
						global.removeAllConnectionsAt(cpOnFeedthrough, removedConnections::add);
						world.setBlockAndUpdate(posForOffset, connector);
						ConnectionPoint newEnd = new ConnectionPoint(posForOffset, 0);
						for(Connection c : removedConnections)
							global.addConnection(new Connection(c.type, newEnd, c.getOtherEnd(cpOnFeedthrough)));
					}
				}
			}
		}
		super.onRemove(state, world, pos, newState, isMoving);
	}

	@Override
	public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items)
	{
		//NOP
	}
}
