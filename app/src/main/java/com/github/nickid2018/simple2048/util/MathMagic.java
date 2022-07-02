package com.github.nickid2018.simple2048.util;

public class MathMagic {

    public static int twoBitHeight(long number) {
        int result = -1;
        while (number != 0) {
            result++;
            number >>>= 1;
        }
        return result;
    }
}
