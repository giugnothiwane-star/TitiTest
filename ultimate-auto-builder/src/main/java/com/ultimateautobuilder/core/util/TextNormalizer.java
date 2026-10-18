package com.ultimateautobuilder.core.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Normalisation des textes de recherche : minuscules, sans accents, espaces compactés. */
public final class TextNormalizer {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9]+");

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        String stripped = DIACRITICS.matcher(decomposed).replaceAll("");
        String lower = stripped.toLowerCase(Locale.ROOT).replace('œ', 'o').replace('æ', 'a');
        return NON_WORD.matcher(lower).replaceAll(" ").trim();
    }

    public static String[] tokens(String text) {
        String n = normalize(text);
        return n.isEmpty() ? new String[0] : n.split(" ");
    }
}
