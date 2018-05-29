package com.yanimetaxas.bookkeeping.tools;

import static com.yanimetaxas.realitycheck.Reality.checkThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * @author yanimetaxas
 */
public class CoverageTool {

  public static void testPrivateConstructor(Class<?> c) {
    Constructor<?>[] declaredConstructors = c.getDeclaredConstructors();
    checkThat(declaredConstructors.length).isEqualTo(1);
    Constructor<?> declaredConstructor = declaredConstructors[0];
    checkThat(declaredConstructor.getModifiers() | Modifier.PRIVATE).isEqualTo(Modifier.PRIVATE);
    try {
      declaredConstructor.setAccessible(true);
      declaredConstructor.newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
    }
  }
}

