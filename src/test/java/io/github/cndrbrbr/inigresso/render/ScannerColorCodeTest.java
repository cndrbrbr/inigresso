package io.github.cndrbrbr.inigresso.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScannerColorCodeTest {

    /** CraftMapCanvas.drawText liest den Code mit Byte.parseByte – jeder Bytewert muss das überstehen. */
    @Test
    void jederFarbwertLaesstSichWiederEinlesen() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            String code = ScannerRenderer.colorCode((byte) i);
            assertEquals('§', code.charAt(0));
            assertEquals(';', code.charAt(code.length() - 1));
            assertEquals((byte) i, Byte.parseByte(code.substring(1, code.length() - 1)));
        }
    }
}
