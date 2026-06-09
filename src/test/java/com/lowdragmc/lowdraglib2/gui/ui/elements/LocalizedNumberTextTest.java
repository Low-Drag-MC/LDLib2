package com.lowdragmc.lowdraglib2.gui.ui.elements;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizedNumberTextTest {

    @Test
    void parsesCommaDecimalForLocaleUsingCommaDecimalSeparator() {
        withFormatLocale(Locale.GERMANY, () -> {
            assertEquals(1.51f, LocalizedNumberText.parseFloat("1,51"), 0.0001f);
            assertEquals(1.51d, LocalizedNumberText.parseDouble("1,51"), 0.0001d);
            assertEquals("1.51", LocalizedNumberText.canonicalizeFloat("1,51"));
            assertEquals("1.51", LocalizedNumberText.canonicalizeDouble("1,51"));
        });
    }

    @Test
    void keepsDotDecimalInputCompatibleInCommaDecimalLocales() {
        withFormatLocale(Locale.GERMANY, () -> {
            assertEquals(1.51f, LocalizedNumberText.parseFloat("1.51"), 0.0001f);
            assertEquals(1.51d, LocalizedNumberText.parseDouble("1.51"), 0.0001d);
        });
    }

    @Test
    void rejectsPartiallyParsedLocalizedNumbers() {
        withFormatLocale(Locale.GERMANY, () ->
                assertThrows(NumberFormatException.class, () -> LocalizedNumberText.parseDouble("1,2,3")));
    }

    @Test
    void doesNotTreatCommaAsDecimalSeparatorForDotDecimalLocales() {
        withFormatLocale(Locale.US, () ->
                assertThrows(NumberFormatException.class, () -> LocalizedNumberText.parseDouble("1,5")));
    }

    @Test
    void acceptsLocaleDecimalSeparatorAsFloatingPointCharacter() {
        withFormatLocale(Locale.GERMANY, () -> {
            assertTrue(LocalizedNumberText.isFloatingPointCharacter(','));
            assertTrue(LocalizedNumberText.isFloatingPointCharacter('.'));
        });
    }

    private static void withFormatLocale(Locale locale, Runnable runnable) {
        var previousLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, locale);
        try {
            runnable.run();
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previousLocale);
        }
    }
}
