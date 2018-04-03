package com.project.springjta.doubleentry.tools;

import static com.yanimetaxas.realitycheck.Reality.checkThat;

import com.yanimetaxas.realitycheck.Reality;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.Assert;

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

