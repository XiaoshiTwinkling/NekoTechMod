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
            Direction side = entry.getKey();
            Item item = entry.getValue();

            if (item instanceof FluxInputerItem inputer) {
                // 自身输入零件：能量从自身机器流向导体组
                Port port = new Port(Port.Type.INPUT, inputer.getInputSpeed(),
                        node.pos, true, node.pos, "flux_inputer");
                node.inputPort = port;
            } else if (item instanceof FluxOutputerItem outputer) {
                // 自身输出零件：能量从导体组流向自身机器
                Port port = new Port(Port.Type.OUTPUT, outputer.getOutputSpeed(),
                        node.pos, true, node.pos, "flux_outputer");
                node.outputPort = port;
            }
        }
    }

    private void scanNeighborPorts(World world, ConductorNode node) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = node.pos.offset(dir);
            BlockEntity neighbor = world.getBlockEntity(neighborPos);

            if (neighbor instanceof ComponentAdaptation neighborCa) {
                Item item = neighborCa.getComponent(dir);
                if (item == null) continue;

                if (item instanceof FluxInputerItem inputer) {
                    // 邻居的输入零件对准我：能量从我流向邻居机器，所以我是输出端
                    Port port = new Port(Port.Type.OUTPUT, inputer.getInputSpeed(),
                            neighborPos, false, node.pos, "flux_inputer");
                    node.outputPort = port;
                } else if (item instanceof FluxOutputerItem outputer) {
                    // 邻居的输出零件对准我：能量从邻居机器流向我，所以我是输入端
                    Port port = new Port(Port.Type.INPUT, outputer.getOutputSpeed(),
                            neighborPos, false, node.pos, "flux_outputer");
                    node.inputPort = port;
                }
            }
        }
    }
}
