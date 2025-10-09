package cn.iinti.malenia2.api.mitm.http

interface HttpHeaders {
    String get(String name)

    String get(String name, String defaultValue)

    Integer getInt(String name)

    Integer getInt(String name, int defaultValue)

    Long getTimeMillis(String name)

    Long getTimeMillis(String name, long defaultValue)

    List<String> getAll(String name)

    Iterator<Map.Entry<String, String>> iterator()

    boolean contains(String name)

    boolean isEmpty()

    int size()

    Set<String> names()

    HttpHeaders add(String name, Object value)

    HttpHeaders add(String name, Iterable<?> values)

    HttpHeaders add(HttpHeaders headers)

    HttpHeaders addInt(String name, int value)

    HttpHeaders set(String name, Object value)

    HttpHeaders set(String name, Iterable<?> values)

    HttpHeaders set(HttpHeaders headers)

    HttpHeaders remove(String name)

    HttpHeaders clear()

    boolean contains(String name, String value, boolean ignoreCase)

    boolean containsValue(String name, String value, boolean ignoreCase)

    String getAsString(String name)

    List<String> getAllAsString(String name)

    String toString()

    HttpHeaders copy()
}