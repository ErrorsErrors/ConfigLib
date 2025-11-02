package de.exlll.configlib;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClassToInstanceMapSerializer<B>
        implements Serializer<ClassToInstanceMap<B>, Map<String, Map<?, ?>>> {

    private final SerializerContext ctx;
    private final String prefix;
    private final String suffix;

    public ClassToInstanceMapSerializer(SerializerContext ctx) {
        this(ctx, null, null);
    }

    public ClassToInstanceMapSerializer(SerializerContext ctx, @Nullable String prefix, @Nullable String suffix) {
        this.ctx = ctx;
        this.prefix = prefix == null ? "" : prefix;
        this.suffix = suffix == null ? "" : suffix;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Map<?, ?>> serialize(ClassToInstanceMap<B> value) {
        if (value == null) return null;

        Map<String, Map<?, ?>> result = new LinkedHashMap<>();
        value.forEach((clazz, element) -> {
            String key = shorten(clazz);

            final var serializer = (TypeSerializer<B, ConfigurationElement<?>>) TypeSerializer
                    .newSerializerFor(clazz, ctx.properties());
            result.put(key, serializer.serialize(element));
        });
        return result;
    }

    @Override
    public ClassToInstanceMap<B> deserialize(Map<String, Map<?, ?>> section) {
        if (section == null) return null;

        MutableClassToInstanceMap<B> map = MutableClassToInstanceMap.create();
        ConfigurationProperties props = ctx.properties();

        for (Map.Entry<String, Map<?, ?>> e : section.entrySet()) {
            try {
                Class<? extends B> type = resolve(e.getKey());
                final var serializer = TypeSerializer.newSerializerFor(type, props);
                B el = serializer.deserialize(e.getValue());
                map.put(type, el);
            } catch (Exception ignored) { }
        }

        return map;
    }

    private String shorten(Class<?> c) {
        String name = c.getName();
        if (!name.startsWith(prefix) || !name.endsWith(suffix)) {
            throw new IllegalStateException(
                    "Невозможно сократить класс '" + name + "' при prefix:'" + prefix + "', suffix:'" + suffix + "'");
        }
        name = name.substring(prefix.length());
        name = name.substring(0, name.length() - suffix.length());
        return name;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends B> resolve(String shortKey) {
        String full = prefix + shortKey + suffix;
        try {
            return (Class<? extends B>) Class.forName(full);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                    "Не удалось найти класс '" + full + "' при разборе YAML", ex);
        }
    }
}
