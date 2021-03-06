package com.github.huangp.entityunit.util;

import com.google.common.base.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * @author Patrick Huang
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HasAnnotationPredicate<A extends AnnotatedElement> implements Predicate<A> {
    private final Class<? extends Annotation> annotationClass;

    public static <A extends AnnotatedElement> Predicate<A> has(Class<? extends Annotation> annotationClass) {
        return new HasAnnotationPredicate<A>(annotationClass);
    }

    @Override
    public boolean apply(A input) {
        return input.isAnnotationPresent(annotationClass);
    }
}
