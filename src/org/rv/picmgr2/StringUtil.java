package org.rv.picmgr2;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class StringUtil {

  public static boolean isBlank(String s) {
    if (s==null) return true;
    if (s.trim().length()==0) return true;
    return false;
  }

public static String safeChar(String input)
{
    char[] allowed = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_".toCharArray();
    char[] charArray = input.toString().toCharArray();
    StringBuilder result = new StringBuilder();
    for (char c : charArray)
    {
        for (char a : allowed)
        {
            if(c==a) result.append(a);
        }
    }
    return result.toString();
}

public static String stripAccents(final String input) {
        if(input == null) {
            return null;
        }
        final Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");//$NON-NLS-1$
        final StringBuilder decomposed = new StringBuilder(Normalizer.normalize(input, Normalizer.Form.NFD));
        convertRemainingAccentCharacters(decomposed);
        // Note that this doesn't correctly remove ligatures...
        return pattern.matcher(decomposed).replaceAll("");
    }

    private static void convertRemainingAccentCharacters(final StringBuilder decomposed) {
        for (int i = 0; i < decomposed.length(); i++) {
            if (decomposed.charAt(i) == '\u0141') {
                decomposed.deleteCharAt(i);
                decomposed.insert(i, 'L');
            } else if (decomposed.charAt(i) == '\u0142') {
                decomposed.deleteCharAt(i);
                decomposed.insert(i, 'l');
            }
        }
    }  
  
}
