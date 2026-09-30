package gg.nulls.library;

import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LuaHighlight {
    private static final Pattern TOKENS=Pattern.compile("--\\[\\[[\\s\\S]*?\\]\\]|--[^\\n]*|\\[\\[[\\s\\S]*?\\]\\]|\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*'|\\b(?:and|break|do|else|elseif|end|false|for|function|if|in|local|nil|not|or|repeat|return|then|true|until|while|continue|export|type)\\b|\\b(?:0x[0-9a-fA-F]+|\\d+(?:\\.\\d+)?)\\b");
    private LuaHighlight(){}
    static void apply(Spannable text,boolean dark){for(ForegroundColorSpan span:text.getSpans(0,text.length(),ForegroundColorSpan.class))text.removeSpan(span);Matcher m=TOKENS.matcher(text);int count=0;while(m.find()&&count++<12000){String token=m.group();int color;if(token.startsWith("--"))color=dark?0xff8daa8b:0xff658267;else if(token.startsWith("\"")||token.startsWith("'")||token.startsWith("[["))color=dark?0xffe5b975:0xffa15b28;else if(Character.isDigit(token.charAt(0)))color=dark?0xffc4a5ed:0xff7452a6;else color=dark?0xff8eb9eb:0xff345fba;text.setSpan(new ForegroundColorSpan(color),m.start(),m.end(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);}}
}
