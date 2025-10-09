package cn.iinti.malenia2.api.mitm.data
/**
 * Created by koush on 5/27/13.
 */
class Multimap extends LinkedHashMap<String, List<String>> implements Iterable<NameValuePair> {
    private Multimap() {
    }

    static Multimap create() {
        new Multimap()
    }

    private List<String> newList() {
        return new ArrayList<>()
    }

    String getString(String name) {
        List<String> ret = get(name)
        if (ret == null || ret.size() == 0)
            return null
        return ret.get(0)
    }

    void add(String name, String value) {
        List<String> ret = get(name)
        if (ret == null) {
            ret = newList()
            put(name, ret)
        }
        ret.add(value)
    }

    void put(String name, String value) {
        List<String> ret = newList()
        ret.add(value)
        put(name, ret)
    }

    Multimap(List<NameValuePair> pairs) {
        for (NameValuePair pair : pairs)
            add(pair.getName(), pair.getValue())
    }

    Multimap(Multimap m) {
        putAll(m)
    }

    interface StringDecoder {
        String decode(String s)
    }

    static Multimap parse(String value, String delimiter, boolean unquote, StringDecoder decoder) {
        Multimap map = new Multimap()
        if (value == null)
            return map
        String[] parts = value.split(delimiter)
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            String key = pair[0].trim();
            String v = null
            if (pair.length > 1)
                v = pair[1]
            if (unquote && v != null && v.endsWith("\"") && v.startsWith("\""))
                v = v.substring(1, v.length() - 1)
            if (decoder != null) {
                key = decoder.decode(key)
                v = decoder.decode(v)
            }
            map.add(key, v)
        }
        map
    }

    static Multimap parseSemicolonDelimited(String header) {
        return parse(header, ";", true, null)
    }

    static Multimap parseCommaDelimited(String header) {
        return parse(header, ",", true, null)
    }

//    private static final StringDecoder QUERY_DECODER = new StringDecoder() {
//        @Override
//        public String decode(String s) {
//            return Uri.decode(s);
//        }
//    };
//
//    public static Multimap parseQuery(String query) {
//        return parse(query, "&", false, QUERY_DECODER);
//    }

    private static final StringDecoder URL_DECODER = new StringDecoder() {
        @Override
        String decode(String s) {
            return URLDecoder.decode(s)
        }
    }

    static Multimap parseUrlEncoded(String query) {
        return parse(query, "&", false, URL_DECODER)
    }


    @Override
    Iterator<NameValuePair> iterator() {
        ArrayList<NameValuePair> ret = new ArrayList<>()
        for (String name : keySet()) {
            List<String> values = get(name)
            for (String value : values) {
                ret.add(new NameValuePair(name, value))
            }
        }
        return ret.iterator()
    }
}