package com.nekotech.genetics;

import net.minecraft.entity.passive.CatEntity;

import java.util.EnumMap;
import java.util.Map;

/**
 * 一只猫的全部基因
 */
public class CatGenetics {
    private final Map<Gene, Genotype> genes = new EnumMap<>(Gene.class);

    public Genotype get(Gene gene) {
        return genes.get(gene);
    }

    public void set(Gene gene, Genotype genotype) {
        genes.put(gene, genotype);
    }

    public void applyAll(CatEntity cat) {
        for (var entry : genes.entrySet()) {
            if (entry.getValue().expressesDominant()) {
                Trait trait = TraitRegistry.get(entry.getKey(), entry.getValue());
                if (trait != null) {
                    trait.applyTo(cat);
                }
            }
        }
    }
}
