package com.modularmc.ten.lib.wrapper;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;

public class SyncedIntArray {

    private final int[] data;

    public SyncedIntArray(int size) {
        data = new int[size];
    }

    public int get(int index) {
        return data[index];
    }

    public void set(int index, int value) {
        data[index] = value;
    }

    public void translate(int index, int add) {
        data[index] += add;
    }

    public void translate(int index, int add, int max) {
        data[index] = Math.min(data[index] + add, max);
    }

    public void translate(int index, int add, int min, int max) {
        data[index] = Math.max(min, Math.min(data[index] + add, max));
    }

    public int size() {
        return data.length;
    }

    public void write(FriendlyByteBuf buf) {
        for (int v : data) {
            buf.writeInt(v);
        }
    }

    public void read(FriendlyByteBuf buf) {
        for (int i = 0; i < data.length; i++) {
            data[i] = buf.readInt();
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
    }
}
