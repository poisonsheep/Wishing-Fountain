package io.github.poisonsheep.wishingfountain.block.multiblock;

import com.google.common.collect.Lists;

import java.util.List;

public final class MultiBlockManager {

    private static final List<IMultiBlock> MULTI_BLOCK_LIST = Lists.newArrayList();

    private MultiBlockManager() {}

    public static void init() {
        if (MULTI_BLOCK_LIST.isEmpty()) {
            MULTI_BLOCK_LIST.add(new WFMultiBlock());
        }
    }

    public static List<IMultiBlock> getMultiBlockList() {
        return MULTI_BLOCK_LIST;
    }

    public static void add(IMultiBlock multiBlock) {
        MULTI_BLOCK_LIST.add(multiBlock);
    }
}
