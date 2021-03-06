package com.github.huangp.entityunit.util;

import lombok.Delegate;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

/**
 * @author Patrick Huang
 */
public class SettableParameter implements Settable {
    @Delegate(types = AnnotatedElement.class)
    private final Parameter parameter;
    private final transient String simpleName;
    private final transient String fullName;

    private SettableParameter(Class<?> ownerType, Parameter parameter) {
        simpleName = parameter.toString();
        fullName = String.format(FULL_NAME_FORMAT, ownerType.getName(), simpleName);
        this.parameter = parameter;
    }

    public static Settable from(Class<?> ownerType, Parameter parameter) {
        return new SettableParameter(ownerType, parameter);
    }

    @Override
    public Type getType() {
        return parameter.getType();
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public String fullyQualifiedName() {
        return fullName;
    }

    @Override
    public <T> T valueIn(Object ownerInstance) {
        throw new UnsupportedOperationException("not supported");
    }
}
