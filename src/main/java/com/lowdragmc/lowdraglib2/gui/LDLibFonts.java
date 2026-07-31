package com.lowdragmc.lowdraglib2.gui;

import com.lowdragmc.lowdraglib2.LDLib2;
import lombok.experimental.UtilityClass;
import net.minecraft.resources.Identifier;

/**
 * Font identifiers usable from anywhere, including style definitions built on the server.
 * <p>
 * Deliberately holds nothing but ids. The renderer behind them lives in
 * {@link com.lowdragmc.lowdraglib2.client.font.LDFonts}, because {@code Font} and everything reachable from it
 * is client only and this class is referenced by common UI code (a {@code Style} naming a font is built on
 * both sides).
 */
@UtilityClass
public final class LDLibFonts {
    public static Identifier JETBRAINS_MONO_BOLD = LDLib2.id("jetbrains_mono_bold");
}
