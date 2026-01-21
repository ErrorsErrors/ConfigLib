package de.exlll.configlib;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ClassToInstanceMapAliasSerializer<B>
        implements Serializer<ClassToInstanceMap<B>, Map<String, Map<?, ?>>> {

    private final SerializerContext ctx;
    private final AliesProvider aliesProvider;

    public ClassToInstanceMapAliasSerializer(SerializerContext ctx) {
        this.ctx = ctx;
        this.aliesProvider = null;
    }

    public ClassToInstanceMapAliasSerializer(SerializerContext ctx, AliesProvider aliesProvider) {
        this.ctx = ctx;
        this.aliesProvider = aliesProvider;
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

        MutableClassToInstanceMap<B> map = MutableClassToInstanceMap.create(new LinkedHashMap<>());
        ConfigurationProperties props = ctx.properties();

        for (Map.Entry<String, Map<?, ?>> e : section.entrySet()) {
            try {
                Class<? extends B> type = resolve(e.getKey());
                final var serializer = TypeSerializer.newSerializerFor(type, props);
                B el = serializer.deserialize(e.getValue());
                map.put(type, el);
            } catch (Exception ignored) {}
        }

        return map;
    }

    private String shorten(Class<?> c) {
        if (aliesProvider == null) {
            return c.getName();
        }
        String alias = aliesProvider.getAlias(c);
        return alias != null ? alias : c.getName();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends B> resolve(String shortKey) {
        if (aliesProvider == null) {
            return (Class<? extends B>) Reflect.getClassByName(shortKey);
        }
        Class<?> type = aliesProvider.getType(shortKey);
        return (Class<? extends B>) Objects.requireNonNullElseGet(type, () -> Reflect.getClassByName(shortKey));
    }

    public interface AliesProvider {
        String getAlias(Class<?> type);
        Class<?> getType(String alias);
    }
}
