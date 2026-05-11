package com.nekotech.genetics;

/**
 * 一对等位基因
 */
public class Genotype {
    public final Allele first;
    public final Allele second;

    public Genotype(Allele a, Allele b) {
        this.first = a;
        this.second = b;
    }

    /**
     * 对应性状是显性还是隐性
     * @return 显隐性
     */
    public boolean expressesDominant() {
        return first.isDominant() || second.isDominant();
    }
}
