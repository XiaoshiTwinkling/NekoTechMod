package com.nekotech.block.entity.api.electrical.conductor;

import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.item.custom.component.FluxInputerItem;
import com.nekotech.item.custom.component.FluxOutputerItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Map;

/**
 * 扫描端口，识别输入输出接口
 */
public class PortScanner {

    public void scanPorts(World world, ConductorNode node) {
        BlockEntity be = world.getBlockEntity(node.pos);

        if (be instanceof ComponentAdaptation ca) {
            // 检查自身安装的零件
            scanSelfPorts(world, node, ca);
        }

        // 检查邻居对准的零件
        scanNeighborPorts(world, node);
    }

    private void scanSelfPorts(World world, ConductorNode node, ComponentAdaptation ca) {
        for (Map.Entry<Direction, Item> entry : ca.getAttachedComponents().entrySet()) {
            Item item = entry.getValue();

            if (item instanceof FluxInputerItem inputer) {
                Port port = new Port(Port.Type.OUTPUT, inputer.getInputSpeed(), // 改为 OUTPUT
                        node.pos, true, node.pos, "flux_inputer");
                node.outputPort = port;
            } else if (item instanceof FluxOutputerItem outputer) {
                Port port = new Port(Port.Type.INPUT, outputer.getOutputSpeed(), // 改为 INPUT
                        node.pos, true, node.pos, "flux_outputer");
                node.inputPort = port;
            }
        }
    }

    private void scanNeighborPorts(World world, ConductorNode node) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = node.pos.offset(dir);
            BlockEntity neighborBe = world.getBlockEntity(neighborPos);

            if (neighborBe instanceof ComponentAdaptation neighborCa) {
                Item neighborItem = neighborCa.getComponent(dir.getOpposite());

                if (neighborItem instanceof FluxInputerItem inputer) {
                    Port port = new Port(Port.Type.INPUT, inputer.getInputSpeed(), // 改为 INPUT
                            node.pos, false, neighborPos, "neighbor_inputer");
                    node.inputPort = port;

                } else if (neighborItem instanceof FluxOutputerItem outputer) {
                    Port port = new Port(Port.Type.OUTPUT, outputer.getOutputSpeed(), // 改为 OUTPUT
                            node.pos, false, neighborPos, "neighbor_outputer");
                    node.outputPort = port;
                }
            }
        }
    }
}
