package com.nekotech.block.entity.machines.api;

public interface IElectricalMachine {
    float getNekoFlux();

    void setNekoFlux(float value);

    default void resetNekoFlux() {
        setNekoFlux(0f);
    }

    default float getMaxNekoFlux() {
        return Float.MAX_VALUE;
    }
}
