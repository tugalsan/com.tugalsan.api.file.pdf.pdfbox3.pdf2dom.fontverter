package org.mabb.fontverter.cff;

import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.fontbox.cff.CFFCIDFont;
import org.apache.fontbox.cff.CFFFont;
import org.apache.fontbox.cff.CFFType1Font;
import org.apache.fontbox.cff.CIDKeyedType2CharString;
import org.apache.fontbox.cff.Type2CharString;
import org.apache.fontbox.cff.Type2CharStringParser;
import org.apache.fontbox.type1.Type1CharStringReader;

public class CffFontPatchUtils {

    public static record Result(Type2CharString charStr, List<Object> type2Sequence) {

        public static Result of(Type2CharString charStr, List<Object> type2Sequence) {
            return new Result(charStr, type2Sequence);
        }
    }

    public static Result getType2CharString(CFFFont font, int cidOrGid) {
        if (font instanceof CFFType1Font _font) {
            return CFFType1Font_getType2CharString(_font, cidOrGid);
        }
        if (font instanceof CFFCIDFont _font) {
            return CFFCIDFont_getType2CharString(_font, cidOrGid);
        }
        return null;
    }

    private static Result CFFType1Font_getType2CharString(CFFType1Font font, int gid) {
        String name = "GID+" + gid; // for debugging only
        return CFFType1Font_getType2CharString(font, gid, name);
    }

    // Returns the Type 2 charstring for the given GID, with name for debugging
    private static Result CFFType1Font_getType2CharString(CFFType1Font font, int gid, String name) {
        return TGS_UnSafe.call(() -> {
            var field_charStringCache = font.getClass().getDeclaredField("charStringCache");
            field_charStringCache.setAccessible(true);
            var charStringCache = (Map<Integer, Type2CharString>) field_charStringCache.get("charStringCache");

            var type2 = charStringCache.get(gid);
            List<Object> type2seq = null;
            if (type2 == null) {

                var field_charStrings = font.getClass().getDeclaredField("charStrings");
                field_charStrings.setAccessible(true);
                var charStrings = (byte[][]) field_charStrings.get("charStrings");

                byte[] bytes = null;
                if (gid < charStrings.length) {
                    bytes = charStrings[gid];
                }
                if (bytes == null) {
                    bytes = charStrings[0]; // .notdef
                }

                var method_getParser = font.getClass().getDeclaredMethod("getParser");
                method_getParser.setAccessible(true);
                var parser = (Type2CharStringParser) method_getParser.invoke(font);

                var field_globalSubrIndex = font.getClass().getDeclaredField("globalSubrIndex");
                field_globalSubrIndex.setAccessible(true);
                var globalSubrIndex = (byte[][]) field_globalSubrIndex.get("globalSubrIndex");

                var method_getLocalSubrIndex = font.getClass().getDeclaredMethod("getLocalSubrIndex");
                method_getLocalSubrIndex.setAccessible(true);
                var getLocalSubrIndex = (byte[][]) method_getLocalSubrIndex.invoke(font, gid);

                type2seq = parser.parse(bytes, globalSubrIndex, getLocalSubrIndex, name);

                var field_reader = font.getClass().getDeclaredField("reader");
                field_reader.setAccessible(true);
                var reader = (Type1CharStringReader) field_reader.get("reader");

                var method_getDefaultWidthX = font.getClass().getDeclaredMethod("getDefaultWidthX");
                method_getDefaultWidthX.setAccessible(true);
                var getDefaultWidthX = (Integer) method_getDefaultWidthX.invoke(font, gid);

                var method_getNominalWidthX = font.getClass().getDeclaredMethod("getNominalWidthX");
                method_getNominalWidthX.setAccessible(true);
                var getNominalWidthX = (Integer) method_getNominalWidthX.invoke(font, gid);

                type2 = new Type2CharString(reader, font.getName(), name, gid, type2seq, getDefaultWidthX, getNominalWidthX);
                charStringCache.put(gid, type2);
            }
            return Result.of(type2, type2seq);
        });
    }

    private static Result CFFCIDFont_getType2CharString(CFFCIDFont font, int cid) {
        return TGS_UnSafe.call(() -> {
            var field_charStringCache = font.getClass().getDeclaredField("charStringCache");
            field_charStringCache.setAccessible(true);
            var charStringCache = (Map<Integer, CIDKeyedType2CharString>) field_charStringCache.get("charStringCache");

            var type2 = charStringCache.get(cid);
            List<Object> type2seq = null;
            if (type2 == null) {
                var gid = font.getCharset().getGIDForCID(cid);

                var field_charStrings = font.getClass().getDeclaredField("charStrings");
                field_charStrings.setAccessible(true);
                var charStrings = (byte[][]) field_charStrings.get("charStrings");

                byte[] bytes = null;
                if (gid < charStrings.length) {
                    bytes = charStrings[gid];
                }
                if (bytes == null) {
                    bytes = charStrings[0]; // .notdef
                }

                var method_getParser = font.getClass().getDeclaredMethod("getParser");
                method_getParser.setAccessible(true);
                var parser = (Type2CharStringParser) method_getParser.invoke(font);

                var field_globalSubrIndex = font.getClass().getDeclaredField("globalSubrIndex");
                field_globalSubrIndex.setAccessible(true);
                var globalSubrIndex = (byte[][]) field_globalSubrIndex.get("globalSubrIndex");

                var method_getLocalSubrIndex = font.getClass().getDeclaredMethod("getLocalSubrIndex");
                method_getLocalSubrIndex.setAccessible(true);
                var getLocalSubrIndex = (byte[][]) method_getLocalSubrIndex.invoke(font, gid);

                type2seq = parser.parse(bytes, globalSubrIndex, getLocalSubrIndex, String.format(Locale.US, "%04x", cid));

                var field_reader = font.getClass().getDeclaredField("reader");
                field_reader.setAccessible(true);
                var reader = (Type1CharStringReader) field_reader.get("reader");

                var method_getDefaultWidthX = font.getClass().getDeclaredMethod("getDefaultWidthX");
                method_getDefaultWidthX.setAccessible(true);
                var getDefaultWidthX = (Integer) method_getDefaultWidthX.invoke(font, gid);

                var method_getNominalWidthX = font.getClass().getDeclaredMethod("getNominalWidthX");
                method_getNominalWidthX.setAccessible(true);
                var getNominalWidthX = (Integer) method_getNominalWidthX.invoke(font, gid);

                type2 = new CIDKeyedType2CharString(reader, font.getName(), cid, gid, type2seq, getDefaultWidthX, getNominalWidthX);
                charStringCache.put(cid, type2);
            }
            return Result.of(type2, type2seq);
        });
    }
}
