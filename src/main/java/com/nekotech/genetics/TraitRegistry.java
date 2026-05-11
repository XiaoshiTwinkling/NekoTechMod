package com.nekotech.genetics;

import com.nekotech.genetics.traits.SpeedTrait;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 注册基因和性状
 */
public class TraitRegistry {

    private static final Map<Gene, Function<Genotype, Trait>> REGISTRY =
            new EnumMap<>(Gene.class);


    public static void register(Gene gene, Function<Genotype, Trait> provider) {
        REGISTRY.put(gene, provider);
    }

    @Nullable
    public static Trait get(Gene gene, Genotype genotype) {
        Function<Genotype, Trait> provider = REGISTRY.get(gene);
        if (provider == null) return null;
        return provider.apply(genotype);
    }


    public static void init() {
        registerSpeed();
    }

    private static void registerSpeed() {
//        register(Gene.SPEED1, genotype -> {
//            if (genotype.expressesDominant()) {
//                return new SpeedTrait(0.06);
//            } else {
//                return new SpeedTrait(-0.02);
//            }
//        });
    }
}
