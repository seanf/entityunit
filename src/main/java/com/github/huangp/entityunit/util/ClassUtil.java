package com.github.huangp.entityunit.util;

import com.github.huangp.entityunit.entity.EntityClass;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Patrick Huang
 */
@Slf4j
public final class ClassUtil {
    private ClassUtil() {
    }

    /**
     * Return all non-static and non-transient fields.
     *
     * @param type
     *         class to work with
     * @return list of fields
     */
    public static List<Field> getInstanceFields(Class type) {
        List<Field> fields = Lists.newArrayList(type.getDeclaredFields());
        return ImmutableList.copyOf(Iterables.filter(fields, InstanceFieldPredicate.PREDICATE));
    }

    public static List<Field> getAllDeclaredFields(Class clazz) {
        List<Field> fields = Lists.newArrayList(clazz.getDeclaredFields());
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null) {
            fields.addAll(Lists.newArrayList(superClass.getDeclaredFields()));
            superClass = superClass.getSuperclass();
        }
        return ImmutableList.copyOf(Iterables.filter(fields, InstanceFieldPredicate.PREDICATE));
    }

    public static Map<String, PropertyDescriptor> getPropertyDescriptors(Class clazz) {
        try {
            PropertyDescriptor[] propDesc = Introspector.getBeanInfo(clazz, clazz.getSuperclass()).getPropertyDescriptors();
            return Maps.uniqueIndex(Lists.newArrayList(propDesc), new Function<PropertyDescriptor, String>() {
                @Override
                public String apply(PropertyDescriptor input) {
                    return input.getName();
                }
            });
        } catch (IntrospectionException e) {
            throw Throwables.propagate(e);
        }
    }

    public static <T> Optional<T> tryFindPublicConstants(final Class<T> type, T instance) throws IllegalAccessException {
        List<Field> fields = Lists.newArrayList(type.getDeclaredFields());
        Optional<Field> found = Iterables.tryFind(fields, new Predicate<Field>() {
            @Override
            public boolean apply(Field input) {
                int mod = input.getModifiers();
                Class<?> fieldType = input.getType();
                return fieldType.equals(type) && Modifier.isPublic(mod) && Modifier.isStatic(mod);
            }
        });
        if (found.isPresent()) {
            return Optional.of((T) found.get().get(instance));
        }
        return Optional.absent();
    }

    public static <T> Constructor<T> findMostArgsConstructor(Class<T> type) {
        List<Constructor<?>> constructors = Lists.newArrayList(type.getDeclaredConstructors());

        // sort by number of parameters in descending order
        Collections.sort(constructors, new Comparator<Constructor<?>>() {
            @Override
            public int compare(Constructor<?> o1, Constructor<?> o2) {
                return o2.getParameterTypes().length - o1.getParameterTypes().length;
            }
        });

        return (Constructor<T>) constructors.get(0);
    }

    // TODO when guava reflection is not @Beta, refactor this to use that
    public static <T> List<Settable> getConstructorParameters(Constructor<T> constructor, Class<?> ownerType) {
        Type[] parameterTypes = constructor.getGenericParameterTypes();
        Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
        List<Settable> params = Lists.newArrayList();
        for (int i = 0; i < parameterTypes.length; i++) {
            Parameter parameterWrap = new Parameter(parameterTypes[i], i, parameterAnnotations[i]);
            Settable settable = SettableParameter.from(ownerType, parameterWrap);
            params.add(settable);
        }
        return params;
    }

    public static <T> T invokeNoArgConstructor(Class<T> type) {
        try {
            Constructor<T> constructor = type.getConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static boolean isAccessTypeIsField(Class clazz) {
        Annotation access = clazz.getAnnotation(Access.class);
        if (access != null) {
            AccessType accessType = ((Access) access).value();
            return accessType == AccessType.FIELD;
        }
        Optional<Field> fieldAnnotatedById = Iterables.tryFind(getAllDeclaredFields(clazz), HasAnnotationPredicate.has(Id.class));
        return fieldAnnotatedById.isPresent();
    }

    public static boolean isCollection(Type type) {
        return Collection.class.isAssignableFrom(getRawType(type));
    }

    public static Class<?> getRawType(Type type) {
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        return (Class<?>) type;
    }

    public static boolean isMap(Type type) {
        return Map.class.isAssignableFrom(getRawType(type));
    }

    public static boolean isPrimitive(Type type) {
        return getRawType(type).isPrimitive();
    }

    public static boolean isEnum(Type type) {
        return getRawType(type).isEnum();
    }

    public static boolean isEntity(Type type) {
        return getRawType(type).isAnnotationPresent(Entity.class);
    }

    public static boolean isArray(Type type) {
        return type instanceof GenericArrayType || getRawType(type).isArray();
    }

    public static <T> T findEntity(Iterable<Object> entities, Class<T> typeToFind) {
        return (T) Iterables.find(entities, Predicates.instanceOf(typeToFind));
    }

    public static <T> T invokeGetter(Object entity, Method method) {
        try {
            method.setAccessible(true);
            T result = (T) method.invoke(entity);
            return result;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static Method getterMethod(Class type, String name) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(type, Object.class);
            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                if (propertyDescriptor.getName().equals(name)) {
                    return propertyDescriptor.getReadMethod();
                }
            }
        } catch (IntrospectionException e) {
            throw Throwables.propagate(e);
        }
        return null;
    }

    public static boolean isUnsaved(Object entity) {
        Settable idSettable = getIdentityField(entity);
        try {
            return idSettable.valueIn(entity) == null;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static Settable getIdentityField(Object entity) {
        return Iterables.find(EntityClass.from(entity.getClass()).getElements(), HasAnnotationPredicate.has(Id.class));
    }

    public static void setValue(Settable settable, Object owner, Object value) {
        final String simpleName = settable.getSimpleName();

        try {
            Field field = Iterables.find(getAllDeclaredFields(owner.getClass()), new Predicate<Field>() {
                @Override
                public boolean apply(Field input) {
                    return input.getName().equals(simpleName);
                }
            });
            field.setAccessible(true);
            field.set(owner, value);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    static <T> T getFieldValue(Object ownerInstance, Field field) {
        try {
            field.setAccessible(true);
            return (T) field.get(ownerInstance);
        }
        catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }

    public static String getEntityName(Class<?> entityType) {
        Preconditions.checkArgument(entityType.isAnnotationPresent(Entity.class));
        Entity entityAnnotation = entityType.getAnnotation(Entity.class);
        if (!Strings.isNullOrEmpty(entityAnnotation.name())) {
            return entityAnnotation.name();
        } else {
            return entityType.getSimpleName();
        }
    }

    private static enum InstanceFieldPredicate implements Predicate<Field> {
        PREDICATE;

        @Override
        public boolean apply(Field input) {
            int mod = input.getModifiers();
            return !Modifier.isStatic(mod) && !Modifier.isTransient(mod);
        }
    }
}
