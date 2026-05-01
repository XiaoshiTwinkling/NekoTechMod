package com.nekotech.block.entity.api.electrical;

public interface IElectricalMachine {
    float getNekoFlux();

    void setNekoFlux(float value);

    default void resetNekoFlux() {
        setNekoFlux(0f);
    }

    default float getMaxNekoFlux() {
        return Float.MAX_VALUE;
    }

    default void addFlux(float addingFlux){
        float NekoFlux = getNekoFlux();
        if(NekoFlux + addingFlux > getMaxNekoFlux()){
            setNekoFlux(getMaxNekoFlux());
        } else {
            setNekoFlux(NekoFlux + addingFlux);
        }
    }

    default void receiveFlux(float receiveFlux){
        float NekoFlux = getNekoFlux();
        if(NekoFlux - receiveFlux < 0){
            setNekoFlux(0.0f);
        } else {
            setNekoFlux(NekoFlux - receiveFlux);
        }
    }
}
