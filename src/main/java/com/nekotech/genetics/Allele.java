package com.nekotech.genetics;

/**
 * 表示等位基因
 * @param gene 基因の位点
 * @param symbol 基因的记号 Aa Bb
 */
public record Allele(Gene gene, String symbol) {
    public boolean isDominant() {
        return symbol.equals(symbol.toUpperCase());
    }
}
