package com.nekotech.block.entity.api.electrical.conductor;

import com.nekotech.NekoTechnology;
import com.nekotech.block.entity.api.component.ComponentAdaptation;
import com.nekotech.item.custom.component.FluxInputerItem;
import com.nekotech.item.custom.component.FluxOutputerItem;
import com.nekotech.item.custom.component.WirePoleItem;
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

            // 扫描接线柱配对
            scanWirePoles(world, node, ca);
        }

        // 检查邻居对准的零件
        scanNeighborPorts(world, node);
    }

    private void scanSelfPorts(World world, ConductorNode node, ComponentAdaptation ca) {
        for (Map.Entry<Direction, Item> entry : ca.getAttachedComponents().entrySet()) {
            Item item = entry.getValue();

            if (item instanceof FluxInputerItem inputer) {
                node.outputPort = new Port(Port.Type.OUTPUT, inputer.getInputSpeed(), // 改为 OUTPUT
                        node.pos, true, node.pos, "flux_inputer");
            } else if (item instanceof FluxOutputerItem outputer) {
                node.inputPort = new Port(Port.Type.INPUT, outputer.getOutputSpeed(), // 改为 INPUT
                        node.pos, true, node.pos, "flux_outputer");
            }
        }
    }

    /**
     * 扫描接线柱配对
     */
    private void scanWirePoles(World world, ConductorNode node, ComponentAdaptation ca) {
        for (Map.Entry<Direction, Item> entry : ca.getAttachedComponents().entrySet()) {
            Item item = entry.getValue();

            if (item instanceof WirePoleItem) {
                WirePoleItem.PairInfo pair = WirePoleItem.getPairInfo(world, node.pos, entry.getKey());
                if (pair != null) {
                    node.addVirtualConnection(pair.targetPos);
                }
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
                    node.inputPort = new Port(Port.Type.INPUT, inputer.getInputSpeed(), // 改为 INPUT
                            node.pos, false, neighborPos, "neighbor_inputer");

                } else if (neighborItem instanceof FluxOutputerItem outputer) {
                    node.outputPort = new Port(Port.Type.OUTPUT, outputer.getOutputSpeed(), // 改为 OUTPUT
                            node.pos, false, neighborPos, "neighbor_outputer");
                }
            }
        }
    }
}