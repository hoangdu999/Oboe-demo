package com.example.Oboe.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * Chuyển đổi kana (hiragana / katakana) sang romaji để hỗ trợ tìm kiếm.
 * Ký tự không phải kana (kanji, chữ Latinh, dấu câu...) được giữ nguyên.
 */
public final class RomajiUtils {

    private RomajiUtils() {}

    private static final Map<String, String> DIGRAPHS = new HashMap<>();
    private static final Map<Character, String> SINGLES = new HashMap<>();

    static {
        String[][] digraphs = {
                {"きゃ", "kya"}, {"きゅ", "kyu"}, {"きょ", "kyo"},
                {"しゃ", "sha"}, {"しゅ", "shu"}, {"しょ", "sho"},
                {"ちゃ", "cha"}, {"ちゅ", "chu"}, {"ちょ", "cho"},
                {"にゃ", "nya"}, {"にゅ", "nyu"}, {"にょ", "nyo"},
                {"ひゃ", "hya"}, {"ひゅ", "hyu"}, {"ひょ", "hyo"},
                {"みゃ", "mya"}, {"みゅ", "myu"}, {"みょ", "myo"},
                {"りゃ", "rya"}, {"りゅ", "ryu"}, {"りょ", "ryo"},
                {"ぎゃ", "gya"}, {"ぎゅ", "gyu"}, {"ぎょ", "gyo"},
                {"じゃ", "ja"}, {"じゅ", "ju"}, {"じょ", "jo"},
                {"ぢゃ", "ja"}, {"ぢゅ", "ju"}, {"ぢょ", "jo"},
                {"びゃ", "bya"}, {"びゅ", "byu"}, {"びょ", "byo"},
                {"ぴゃ", "pya"}, {"ぴゅ", "pyu"}, {"ぴょ", "pyo"},
                {"ふぁ", "fa"}, {"ふぃ", "fi"}, {"ふぇ", "fe"}, {"ふぉ", "fo"}
        };
        for (String[] d : digraphs) {
            DIGRAPHS.put(d[0], d[1]);
        }

        String[][] singles = {
                {"あ", "a"}, {"い", "i"}, {"う", "u"}, {"え", "e"}, {"お", "o"},
                {"か", "ka"}, {"き", "ki"}, {"く", "ku"}, {"け", "ke"}, {"こ", "ko"},
                {"が", "ga"}, {"ぎ", "gi"}, {"ぐ", "gu"}, {"げ", "ge"}, {"ご", "go"},
                {"さ", "sa"}, {"し", "shi"}, {"す", "su"}, {"せ", "se"}, {"そ", "so"},
                {"ざ", "za"}, {"じ", "ji"}, {"ず", "zu"}, {"ぜ", "ze"}, {"ぞ", "zo"},
                {"た", "ta"}, {"ち", "chi"}, {"つ", "tsu"}, {"て", "te"}, {"と", "to"},
                {"だ", "da"}, {"ぢ", "ji"}, {"づ", "zu"}, {"で", "de"}, {"ど", "do"},
                {"な", "na"}, {"に", "ni"}, {"ぬ", "nu"}, {"ね", "ne"}, {"の", "no"},
                {"は", "ha"}, {"ひ", "hi"}, {"ふ", "fu"}, {"へ", "he"}, {"ほ", "ho"},
                {"ば", "ba"}, {"び", "bi"}, {"ぶ", "bu"}, {"べ", "be"}, {"ぼ", "bo"},
                {"ぱ", "pa"}, {"ぴ", "pi"}, {"ぷ", "pu"}, {"ぺ", "pe"}, {"ぽ", "po"},
                {"ま", "ma"}, {"み", "mi"}, {"む", "mu"}, {"め", "me"}, {"も", "mo"},
                {"や", "ya"}, {"ゆ", "yu"}, {"よ", "yo"},
                {"ら", "ra"}, {"り", "ri"}, {"る", "ru"}, {"れ", "re"}, {"ろ", "ro"},
                {"わ", "wa"}, {"ゐ", "i"}, {"ゑ", "e"}, {"を", "wo"}, {"ん", "n"},
                {"ぁ", "a"}, {"ぃ", "i"}, {"ぅ", "u"}, {"ぇ", "e"}, {"ぉ", "o"},
                {"ゃ", "ya"}, {"ゅ", "yu"}, {"ょ", "yo"},
                {"ゔ", "vu"}
        };
        for (String[] s : singles) {
            SINGLES.put(s[0].charAt(0), s[1]);
        }
    }

    /**
     * Chuyển chuỗi kana sang romaji. Kanji và ký tự khác được giữ nguyên.
     */
    public static String toRomaji(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String s = katakanaToHiragana(input);
        StringBuilder out = new StringBuilder(s.length() * 2);
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);

            // Sokuon っ: nhân đôi phụ âm đầu của âm tiếp theo
            if (c == 'っ') {
                String next = romajiAt(s, i + 1);
                if (!next.isEmpty()) {
                    char cons = next.charAt(0);
                    if ("aeiou".indexOf(cons) < 0) {
                        out.append(cons);
                    }
                }
                i++;
                continue;
            }

            // Trường âm ー: lặp lại nguyên âm trước đó
            if (c == 'ー') {
                if (out.length() > 0) {
                    char last = out.charAt(out.length() - 1);
                    if ("aeiou".indexOf(last) >= 0) {
                        out.append(last);
                    }
                }
                i++;
                continue;
            }

            // Âm ghép (2 ký tự)
            if (i + 1 < s.length()) {
                String digraph = DIGRAPHS.get(s.substring(i, i + 2));
                if (digraph != null) {
                    out.append(digraph);
                    i += 2;
                    continue;
                }
            }

            // Âm đơn
            String single = SINGLES.get(c);
            if (single != null) {
                out.append(single);
            } else {
                // Kanji / chữ Latinh / dấu câu: giữ nguyên
                out.append(c);
            }
            i++;
        }
        return out.toString();
    }

    /**
     * Kiểm tra keyword (romaji, không phân biệt hoa thường) có xuất hiện trong
     * bất kỳ nguồn tiếng Nhật nào (sau khi đã chuyển sang romaji) hay không.
     */
    public static boolean matchesRomaji(String keyword, Iterable<String> japaneseSources) {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        String kw = keyword.toLowerCase().trim();
        for (String source : japaneseSources) {
            if (source == null || source.isBlank()) {
                continue;
            }
            if (toRomaji(source).toLowerCase().contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static String romajiAt(String s, int index) {
        if (index >= s.length()) {
            return "";
        }
        if (index + 1 < s.length()) {
            String digraph = DIGRAPHS.get(s.substring(index, index + 2));
            if (digraph != null) {
                return digraph;
            }
        }
        String single = SINGLES.get(s.charAt(index));
        return single != null ? single : "";
    }

    private static String katakanaToHiragana(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= 0x30A1 && c <= 0x30F6) {
                sb.append((char) (c - 0x60));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
