package epic.verify.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {

    private Json() {
    }

    public static abstract class Value {
    }

    public static final class Null extends Value {
        public static final Null INSTANCE = new Null();

        private Null() {
        }
    }

    public static final class Bool extends Value {
        public final boolean value;

        public Bool(boolean v) {
            this.value = v;
        }
    }

    public static final class Num extends Value {
        public final String raw;

        public Num(String r) {
            this.raw = r;
        }

        public int intValue() {
            try {
                return Integer.parseInt(raw);
            } catch (Exception ignored) {
            }
            return (int) longValue();
        }

        public long longValue() {
            try {
                return Long.parseLong(raw);
            } catch (Exception ignored) {
            }
            try {
                return (long) Double.parseDouble(raw);
            } catch (Exception ignored) {
            }
            return 0L;
        }

        public double doubleValue() {
            try {
                return Double.parseDouble(raw);
            } catch (Exception ignored) {
            }
            return 0d;
        }
    }

    public static final class Str extends Value {
        public final String value;

        public Str(String v) {
            this.value = v;
        }
    }

    public static final class Arr extends Value {
        public final List<Value> items = new ArrayList<Value>();
    }

    public static final class Obj extends Value {
        public final Map<String, Value> map = new LinkedHashMap<String, Value>();

        public boolean has(String key) {
            return map.containsKey(key);
        }

        public Value get(String key) {
            return map.get(key);
        }

        public String getString(String key) {
            Value v = map.get(key);
            if (v == null || v instanceof Null) return null;
            if (v instanceof Str) return ((Str) v).value;
            if (v instanceof Num) return ((Num) v).raw;
            if (v instanceof Bool) return String.valueOf(((Bool) v).value);
            return v.toString();
        }

        public String optString(String key, String def) {
            String s = getString(key);
            return s == null ? def : s;
        }

        public int getInt(String key) {
            Value v = map.get(key);
            if (v instanceof Num) return ((Num) v).intValue();
            if (v instanceof Str) {
                try {
                    return Integer.parseInt(((Str) v).value);
                } catch (Exception ignored) {
                }
            }
            return 0;
        }

        public long getLong(String key) {
            Value v = map.get(key);
            if (v instanceof Num) return ((Num) v).longValue();
            if (v instanceof Str) {
                try {
                    return Long.parseLong(((Str) v).value);
                } catch (Exception ignored) {
                }
            }
            return 0L;
        }

        public boolean getBoolean(String key) {
            Value v = map.get(key);
            if (v instanceof Bool) return ((Bool) v).value;
            if (v instanceof Num) return ((Num) v).intValue() != 0;
            if (v instanceof Str) return "true".equalsIgnoreCase(((Str) v).value);
            return false;
        }

        public Obj getObject(String key) {
            Value v = map.get(key);
            return v instanceof Obj ? (Obj) v : null;
        }

        public Arr getArray(String key) {
            Value v = map.get(key);
            return v instanceof Arr ? (Arr) v : null;
        }
    }

    public static Value parse(String text) {
        if (text == null) return null;
        Parser p = new Parser(text);
        Value v = p.parseValue();
        p.skipWs();
        if (!p.atEnd()) throw new IllegalArgumentException("trailing content at pos " + p.pos);
        return v;
    }

public static String toString(Value v) {
        if (v == null || v instanceof Null) return "null";
        if (v instanceof Bool) return String.valueOf(((Bool) v).value);
        if (v instanceof Num) return ((Num) v).raw;
        if (v instanceof Str) return quote(((Str) v).value);
        if (v instanceof Arr) {
            Arr a = (Arr) v;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < a.items.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toString(a.items.get(i)));
            }
            return sb.append(']').toString();
        }
        Obj o = (Obj) v;
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Value> e : o.map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(quote(e.getKey())).append(':').append(toString(e.getValue()));
        }
        return sb.append('}').toString();
    }

public static String toPrettyString(Value v) {
        return pretty(v, 0);
    }

    private static String pretty(Value v, int depth) {
        if (v instanceof Obj) {
            Obj o = (Obj) v;
            if (o.map.isEmpty()) return "{}";
            StringBuilder sb = new StringBuilder("{\n");
            int i = 0;
            for (Map.Entry<String, Value> e : o.map.entrySet()) {
                sb.append(indent(depth + 1)).append(quote(e.getKey())).append(": ").append(pretty(e.getValue(), depth + 1));
                if (++i < o.map.size()) sb.append(',');
                sb.append('\n');
            }
            return sb.append(indent(depth)).append('}').toString();
        }
        if (v instanceof Arr) {
            Arr a = (Arr) v;
            if (a.items.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < a.items.size(); i++) {
                sb.append(indent(depth + 1)).append(pretty(a.items.get(i), depth + 1));
                if (i < a.items.size() - 1) sb.append(',');
                sb.append('\n');
            }
            return sb.append(indent(depth)).append(']').toString();
        }
        return toString(v);
    }

    private static String indent(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append("  ");
        return sb.toString();
    }

    private static String quote(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append('\\').append('u').append(String.format("%04x", (int) c));
                    } else {
                        sb.append(c);   
                    }
            }
        }
        return sb.append('"').toString();
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        boolean atEnd() {
            return pos >= s.length();
        }

        char peek() {
            return s.charAt(pos);
        }

        void skipWs() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }

        Value parseValue() {
            skipWs();
            char c = s.charAt(pos);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return new Str(parseString());
                case 't':
                    expect("true");
                    return new Bool(true);
                case 'f':
                    expect("false");
                    return new Bool(false);
                case 'n':
                    expect("null");
                    return Null.INSTANCE;
                default:
                    return parseNumber();
            }
        }

        Obj parseObject() {
            pos++;
            Obj o = new Obj();
            skipWs();
            if (pos < s.length() && peek() == '}') {
                pos++;
                return o;
            }
            while (true) {
                skipWs();
                if (peek() != '"') throw new IllegalArgumentException("expected key at pos " + pos);
                String key = parseString();
                skipWs();
                if (peek() != ':') throw new IllegalArgumentException("expected ':' at pos " + pos);
                pos++;
                Value v = parseValue();
                o.map.put(key, v);
                skipWs();
                char c = peek();
                if (c == ',') {
                    pos++;
                    continue;
                }
                if (c == '}') {
                    pos++;
                    return o;
                }
                throw new IllegalArgumentException("expected ',' or '}' at pos " + pos);
            }
        }

        Arr parseArray() {
            pos++;
            Arr a = new Arr();
            skipWs();
            if (pos < s.length() && peek() == ']') {
                pos++;
                return a;
            }
            while (true) {
                a.items.add(parseValue());
                skipWs();
                char c = peek();
                if (c == ',') {
                    pos++;
                    continue;
                }
                if (c == ']') {
                    pos++;
                    return a;
                }
                throw new IllegalArgumentException("expected ',' or ']' at pos " + pos);
            }
        }

        String parseString() {
            if (peek() != '"') throw new IllegalArgumentException("expected '\"' at pos " + pos);
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= s.length()) break;
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 <= s.length()) {
                                String hex = s.substring(pos, pos + 4);
                                try {
                                    sb.append((char) Integer.parseInt(hex, 16));
                                } catch (Exception ignored) {
                                    sb.append('u');
                                }
                                pos += 4;
                            } else {
                                sb.append('u');
                            }
                            break;
                        default:
                            sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("unterminated string");
        }

        Num parseNumber() {
            int start = pos;
            if (pos < s.length() && (peek() == '-' || peek() == '+')) pos++;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') pos++;
                else break;
            }
            String num = s.substring(start, pos);
            if (num.length() == 0) throw new IllegalArgumentException("invalid number at pos " + start);
            return new Num(num);
        }

        void expect(String lit) {
            if (s.startsWith(lit, pos)) {
                pos += lit.length();
            } else {
                throw new IllegalArgumentException("unexpected token at pos " + pos);
            }
        }
    }
}
