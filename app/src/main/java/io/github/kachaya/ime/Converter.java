/*
 * Copyright 2023 kachaya
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.kachaya.ime;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.regex.Pattern;

public class Converter {

    private static final String[][] romajiTable = {
            {"bb", "っb"},
            {"cc", "っc"},
            {"dd", "っd"},
            {"ff", "っf"},
            {"gg", "っg"},
            {"hh", "っh"},
            {"jj", "っj"},
            {"kk", "っk"},
            {"ll", "っl"},
            {"mm", "っm"},
            {"pp", "っp"},
            {"qq", "っq"},
            {"rr", "っr"},
            {"ss", "っs"},
            {"tt", "っt"},
            {"vv", "っv"},
            {"ww", "っw"},
            {"xx", "っx"},
            {"yy", "っy"},
            {"zz", "っz"},

            {"n'", "ん"},
            {"nn", "ん"},

            {"nb", "んb"},
            {"nc", "んc"},
            {"nd", "んd"},
            {"nf", "んf"},
            {"ng", "んg"},
            {"nh", "んh"},
            {"nj", "んj"},
            {"nk", "んk"},
            {"nl", "んl"},
            {"nm", "んm"},
            {"np", "んp"},
            {"nr", "んr"},
            {"ns", "んs"},
            {"nt", "んt"},
            {"nv", "んv"},
            {"nw", "んw"},
            {"nx", "んx"},
            {"nz", "んz"},

            // 以下、ローマ字が長い順
            {"ltsu", "っ"},
            {"xtsu", "っ"},

            {"bya", "びゃ"}, {"byi", "びぃ"}, {"byu", "びゅ"}, {"bye", "びぇ"}, {"byo", "びょ"},
            {"cha", "ちゃ"}, {"chi", "ち"}, {"chu", "ちゅ"}, {"che", "ちぇ"}, {"cho", "ちょ"},
            {"cya", "ちゃ"}, {"cyi", "ちぃ"}, {"cyu", "ちゅ"}, {"cye", "ちぇ"}, {"cyo", "ちょ"},
            {"dha", "でゃ"}, {"dhi", "でぃ"}, {"dhu", "でゅ"}, {"dhe", "でぇ"}, {"dho", "でょ"},
            {"dya", "ぢゃ"}, {"dyi", "ぢぃ"}, {"dyu", "ぢゅ"}, {"dye", "ぢぇ"}, {"dyo", "ぢょ"},
            {"fya", "ふゃ"}, {"fyi", "ふぃ"}, {"fyu", "ふゅ"}, {"fye", "ふぇ"}, {"fyo", "ふょ"},
            {"gya", "ぎゃ"}, {"gyi", "ぎぃ"}, {"gyu", "ぎゅ"}, {"gye", "ぎぇ"}, {"gyo", "ぎょ"},
            {"hya", "ひゃ"}, {"hyi", "ひぃ"}, {"hyu", "ひゅ"}, {"hye", "ひぇ"}, {"hyo", "ひょ"},
            {"jya", "じゃ"}, {"jyi", "じぃ"}, {"jyu", "じゅ"}, {"jye", "じぇ"}, {"jyo", "じょ"},
            {"kya", "きゃ"}, {"kyi", "きぃ"}, {"kyu", "きゅ"}, {"kye", "きぇ"}, {"kyo", "きょ"},
            {"lya", "ゃ"}, {"lyi", "ぃ"}, {"lyu", "ゅ"}, {"lye", "ぇ"}, {"lyo", "ょ"},
            {"mya", "みゃ"}, {"myi", "みぃ"}, {"myu", "みゅ"}, {"mye", "みぇ"}, {"myo", "みょ"},
            {"nya", "にゃ"}, {"nyi", "にぃ"}, {"nyu", "にゅ"}, {"nye", "にぇ"}, {"nyo", "にょ"},
            {"pya", "ぴゃ"}, {"pyi", "ぴぃ"}, {"pyu", "ぴゅ"}, {"pye", "ぴぇ"}, {"pyo", "ぴょ"},
            {"rya", "りゃ"}, {"ryi", "りぃ"}, {"ryu", "りゅ"}, {"rye", "りぇ"}, {"ryo", "りょ"},
            {"sha", "しゃ"}, {"shi", "し"}, {"shu", "しゅ"}, {"she", "しぇ"}, {"sho", "しょ"},
            {"sya", "しゃ"}, {"syi", "しぃ"}, {"syu", "しゅ"}, {"sye", "しぇ"}, {"syo", "しょ"},
            {"tha", "てゃ"}, {"thi", "てぃ"}, {"thu", "てゅ"}, {"the", "てぇ"}, {"tho", "てょ"},
            {"tsa", "つぁ"}, {"tsi", "つぃ"}, {"tsu", "つ"}, {"tse", "つぇ"}, {"tso", "つぉ"},
            {"tya", "ちゃ"}, {"tyi", "ちぃ"}, {"tyu", "ちゅ"}, {"tye", "ちぇ"}, {"tyo", "ちょ"},
            {"vya", "ゔゃ"}, {"vyi", "ゔぃ"}, {"vyu", "ゔゅ"}, {"vye", "ゔぇ"}, {"vyo", "ゔょ"},
            {"xya", "ゃ"}, {"xyi", "ぃ"}, {"xyu", "ゅ"}, {"xye", "ぇ"}, {"xyo", "ょ"},
            {"zya", "じゃ"}, {"zyi", "じぃ"}, {"zyu", "じゅ"}, {"zye", "じぇ"}, {"zyo", "じょ"},

            {"lka", "ゕ"}, {"lke", "ゖ"},
            {"wyi", "ゐ"}, {"wye", "ゑ"},
            {"xka", "ゕ"}, {"xke", "ゖ"},

            {"ltu", "っ"},
            {"lwa", "ゎ"},
            {"xtu", "っ"},
            {"xwa", "ゎ"},

            {"ba", "ば"}, {"bi", "び"}, {"bu", "ぶ"}, {"be", "べ"}, {"bo", "ぼ"},
            {"ca", "か"}, {"ci", "し"}, {"cu", "く"}, {"ce", "せ"}, {"co", "こ"},
            {"da", "だ"}, {"di", "ぢ"}, {"du", "づ"}, {"de", "で"}, {"do", "ど"},
            {"fa", "ふぁ"}, {"fi", "ふぃ"}, {"fu", "ふ"}, {"fe", "ふぇ"}, {"fo", "ふぉ"},
            {"ga", "が"}, {"gi", "ぎ"}, {"gu", "ぐ"}, {"ge", "げ"}, {"go", "ご"},
            {"ha", "は"}, {"hi", "ひ"}, {"hu", "ふ"}, {"he", "へ"}, {"ho", "ほ"},
            {"ja", "じゃ"}, {"ji", "じ"}, {"ju", "じゅ"}, {"je", "じぇ"}, {"jo", "じょ"},
            {"ka", "か"}, {"ki", "き"}, {"ku", "く"}, {"ke", "け"}, {"ko", "こ"},
            {"la", "ぁ"}, {"li", "ぃ"}, {"lu", "ぅ"}, {"le", "ぇ"}, {"lo", "ぉ"},
            {"ma", "ま"}, {"mi", "み"}, {"mu", "む"}, {"me", "め"}, {"mo", "も"},
            {"na", "な"}, {"ni", "に"}, {"nu", "ぬ"}, {"ne", "ね"}, {"no", "の"},
            {"pa", "ぱ"}, {"pi", "ぴ"}, {"pu", "ぷ"}, {"pe", "ぺ"}, {"po", "ぽ"},
            {"qa", "くぁ"}, {"qi", "くぃ"}, {"qu", "く"}, {"qe", "くぇ"}, {"qo", "くぉ"},
            {"ra", "ら"}, {"ri", "り"}, {"ru", "る"}, {"re", "れ"}, {"ro", "ろ"},
            {"sa", "さ"}, {"si", "し"}, {"su", "す"}, {"se", "せ"}, {"so", "そ"},
            {"ta", "た"}, {"ti", "ち"}, {"tu", "つ"}, {"te", "て"}, {"to", "と"},
            {"va", "ゔぁ"}, {"vi", "ゔぃ"}, {"vu", "ゔ"}, {"ve", "ゔぇ"}, {"vo", "ゔぉ"},
            {"wa", "わ"}, {"wi", "うぃ"}, {"wu", "う"}, {"we", "うぇ"}, {"wo", "を"},
            {"xa", "ぁ"}, {"xi", "ぃ"}, {"xu", "ぅ"}, {"xe", "ぇ"}, {"xo", "ぉ"},
            {"ya", "や"}, {"yi", "い"}, {"yu", "ゆ"}, {"ye", "いぇ"}, {"yo", "よ"},
            {"za", "ざ"}, {"zi", "じ"}, {"zu", "ず"}, {"ze", "ぜ"}, {"zo", "ぞ"},

            {"a", "あ"}, {"i", "い"}, {"u", "う"}, {"e", "え"}, {"o", "お"},

            {"-", "ー"},

            {",", "、"},
            {".", "。"},
            {"!", "！"},
            {"?", "？"},
            {"/", "・"},
            {"[", "「"},
            {"]", "」"},
    };

    private static final HashMap<Character, String> halfKatakanaMap = new HashMap<Character, String>() {
        {
            put('あ', "ｱ");
            put('い', "ｲ");
            put('う', "ｳ");
            put('え', "ｴ");
            put('お', "ｵ");
            put('か', "ｶ");
            put('き', "ｷ");
            put('く', "ｸ");
            put('け', "ｹ");
            put('こ', "ｺ");
            put('さ', "ｻ");
            put('し', "ｼ");
            put('す', "ｽ");
            put('せ', "ｾ");
            put('そ', "ｿ");
            put('た', "ﾀ");
            put('ち', "ﾁ");
            put('つ', "ﾂ");
            put('て', "ﾃ");
            put('と', "ﾄ");
            put('な', "ﾅ");
            put('に', "ﾆ");
            put('ぬ', "ﾇ");
            put('ね', "ﾈ");
            put('の', "ﾉ");
            put('は', "ﾊ");
            put('ひ', "ﾋ");
            put('ふ', "ﾌ");
            put('へ', "ﾍ");
            put('ほ', "ﾎ");
            put('ま', "ﾏ");
            put('み', "ﾐ");
            put('む', "ﾑ");
            put('め', "ﾒ");
            put('も', "ﾓ");
            put('や', "ﾔ");
            put('ゆ', "ﾕ");
            put('よ', "ﾖ");
            put('ら', "ﾗ");
            put('り', "ﾘ");
            put('る', "ﾙ");
            put('れ', "ﾚ");
            put('ろ', "ﾛ");
            put('わ', "ﾜ");
            put('を', "ｦ");
            put('ん', "ﾝ");

            put('が', "ｶﾞ");
            put('ぎ', "ｷﾞ");
            put('ぐ', "ｸﾞ");
            put('げ', "ｹﾞ");
            put('ご', "ｺﾞ");
            put('ざ', "ｻﾞ");
            put('じ', "ｼﾞ");
            put('ず', "ｽﾞ");
            put('ぜ', "ｾﾞ");
            put('ぞ', "ｿﾞ");
            put('だ', "ﾀﾞ");
            put('ぢ', "ﾁﾞ");
            put('づ', "ﾂﾞ");
            put('で', "ﾃﾞ");
            put('ど', "ﾄﾞ");
            put('ば', "ﾊﾞ");
            put('び', "ﾋﾞ");
            put('ぶ', "ﾌﾞ");
            put('べ', "ﾍﾞ");
            put('ぼ', "ﾎﾞ");
            put('ぱ', "ﾊﾟ");
            put('ぴ', "ﾋﾟ");
            put('ぷ', "ﾌﾟ");
            put('ぺ', "ﾍﾟ");
            put('ぽ', "ﾎﾟ");

            put('ゔ', "ｳﾞ");

            put('ぁ', "ｧ");
            put('ぃ', "ｨ");
            put('ぅ', "ｩ");
            put('ぇ', "ｪ");
            put('ぉ', "ｫ");
            put('ゃ', "ｬ");
            put('ゅ', "ｭ");
            put('ょ', "ｮ");
            put('っ', "ｯ");
            put('ー', "ｰ");
            put('、', "､");
            put('。', "｡");
            put('「', "｢");
            put('」', "｣");
            put('゛', "ﾞ");
            put('゜', "ﾟ");
            put('・', "･");
        }
    };

    // ローマ字をひらがなへ変換
    public static String romajiToHiragana(@NonNull CharSequence cs) {
        String s = cs.toString().toLowerCase();
        for (String[] pair : romajiTable) {
            s = s.replaceAll(Pattern.quote(pair[0]), pair[1]);  // "."のためにquote
        }
        return s;
    }

    // 全角英数へ変換
    public static char toWideLatin(char ch) {
        if (ch == '\u0020') {
            return '\u3000';    // 全角スペース
        }
        if (ch == '\u00A5') {   // 「\」
            return '￥';
        }
        if (ch > '\u0020' && ch < '\u007F') {
            return (char) ((ch - '\u0020') + '\uFF00');
        }
        return ch;
    }

    // 全角英数へ変換
    @NonNull
    public static String toWideLatin(@NonNull CharSequence cs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cs.length(); i++) {
            sb.append(toWideLatin(cs.charAt(i)));
        }
        return sb.toString();
    }

    // 全角カタカナへ変換
    public static char toWideKatakana(char ch) {
        if (ch >= 'ぁ' && ch <= 'ゖ') {
            return (char) (ch - 'ぁ' + 'ァ');
        }
        return ch;
    }

    // 全角カタカナへ変換
    public static String toWideKatakana(CharSequence cs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cs.length(); i++) {
            sb.append(toWideKatakana(cs.charAt(i)));
        }
        return sb.toString();
    }

    // 半角カタカナへ変換
    public static String toHalfKatakana(CharSequence cs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cs.length(); i++) {
            char key = cs.charAt(i);
            String val = halfKatakanaMap.get(key);
            if (val != null) {
                sb.append(val);
            } else {
                sb.append(key);
            }
        }
        return sb.toString();
    }


}
