package gyurix.minilib.utils;

import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class Reflection {
  public static final Map<Class, Field[]> allFieldCache = Collections.synchronizedMap(new WeakHashMap<>());
  public static final ReflectionFactory rf = ReflectionFactory.getReflectionFactory();

  /**
   * Compares two arrays of classes
   *
   * @param l1 - The first array of classes
   * @param l2 - The second array of classes
   * @return True if the classes matches in the 2 arrays, false otherwise
   */
  public static boolean classArrayCompare(Class[] l1, Class[] l2) {
    if (l1.length != l2.length) {
      return false;
    }
    for (int i = 0; i < l1.length; i++) {
      if (l1[i] != l2[i])
        return false;
    }
    return true;
  }

  /**
   * Compares two arrays of classes
   *
   * @param l1 - The first array of classes
   * @param l2 - The second array of classes
   * @return True if each of the second arrays classes is assignable from the first arrays classes
   */
  public static boolean classArrayCompareLight(Class[] l1, Class[] l2) {
    if (l1.length != l2.length) {
      return false;
    }
    for (int i = 0; i < l1.length; i++) {
      if (!Primitives.wrap(l2[i]).isAssignableFrom(Primitives.wrap(l1[i])))
        return false;
    }
    return true;
  }

  public static Field[] getAllFields(Class c) {
    Field[] fs = allFieldCache.get(c);
    if (fs != null)
      return fs;
    ArrayList<Field> out = new ArrayList<>();
    while (c != null) {
      for (Field f : c.getDeclaredFields()) {
        out.add(setFieldAccessible(f));
      }
      c = c.getSuperclass();
    }
    Field[] oa = new Field[out.size()];
    out.toArray(oa);
    allFieldCache.put(c, oa);
    return oa;
  }

  /**
   * Gets the constructor of the given class
   *
   * @param cl      - The class
   * @param classes - The parameters of the constructor
   * @return The found constructor or null if it was not found.
   */
  public static Constructor getConstructor(Class cl, Class... classes) {
    try {
      Constructor c = cl.getDeclaredConstructor(classes);
      c.setAccessible(true);
      return c;
    } catch (Throwable ignored) {
    }
    return null;
  }

  public static Object getEnum(Class enumType, String value) {
    try {
      return enumType.getMethod("valueOf", String.class).invoke(null, value);
    } catch (Throwable e) {
      return null;
    }
  }

  public static Field getField(Class clazz, String name) {
    try {
      return setFieldAccessible(clazz.getDeclaredField(name));
    } catch (Throwable e) {
      return null;
    }
  }

  public static Object getFieldData(Class clazz, String name) {
    return getFieldData(clazz, name, null);
  }

  public static Object getFieldData(Class clazz, String name, Object object) {
    try {
      return setFieldAccessible(clazz.getDeclaredField(name)).get(object);
    } catch (Throwable e) {
      return null;
    }
  }

  public static Field getFirstFieldOfType(Class clazz, Class type) {
    try {
      for (Field f : clazz.getDeclaredFields()) {
        if (f.getType().equals(type))
          return setFieldAccessible(f);
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  public static Class getInnerClass(Class cl, String name) {
    try {
      name = cl.getName() + "$" + name;
      for (Class c : cl.getDeclaredClasses())
        if (c.getName().equals(name))
          return c;
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return null;
  }

  public static Field getLastFieldOfType(Class clazz, Class type) {
    Field field = null;
    for (Field f : clazz.getDeclaredFields()) {
      if (f.getType().equals(type))
        field = f;
    }
    return setFieldAccessible(field);
  }

  public static Method getMethod(Class cl, String name, Class... args) {
    if (args.length == 0) {
      while (cl != null) {
        Method m = methodCheckNoArg(cl, name);
        if (m != null) {
          m.setAccessible(true);
          return m;
        }
        cl = cl.getSuperclass();
      }
    } else {
      while (cl != null) {
        Method m = methodCheck(cl, name, args);
        if (m != null) {
          m.setAccessible(true);
          return m;
        }
        cl = cl.getSuperclass();
      }
    }
    return null;
  }

  private static Method methodCheck(Class cl, String name, Class[] args) {
    try {
      return cl.getDeclaredMethod(name, args);
    } catch (Throwable e) {
      Method[] mtds = cl.getDeclaredMethods();
      for (Method met : mtds)
        if (classArrayCompare(args, met.getParameterTypes()) && met.getName().equals(name))
          return met;
      for (Method met : mtds)
        if (classArrayCompareLight(args, met.getParameterTypes()) && met.getName().equals(name))
          return met;
      for (Method met : mtds)
        if (classArrayCompare(args, met.getParameterTypes()) && met.getName().equalsIgnoreCase(name))
          return met;
      for (Method met : mtds)
        if (classArrayCompareLight(args, met.getParameterTypes()) && met.getName().equalsIgnoreCase(name))
          return met;
      return null;
    }
  }

  private static Method methodCheckNoArg(Class cl, String name) {
    try {
      return cl.getDeclaredMethod(name);
    } catch (Throwable e) {
      Method[] mtds = cl.getDeclaredMethods();
      for (Method met : mtds)
        if (met.getParameterTypes().length == 0 && met.getName().equalsIgnoreCase(name))
          return met;
      return null;
    }
  }

  /**
   * Constructs a new instance of the given class
   *
   * @param cl      - The class
   * @param classes - The parameters of the constructor
   * @param objs    - The objects, passed to the constructor
   * @return The object constructed, with the found constructor or null if there was an error.
   */
  public static Object newInstance(Class cl, Class[] classes, Object... objs) {
    try {
      Constructor c = cl.getDeclaredConstructor(classes);
      c.setAccessible(true);
      return c.newInstance(objs);
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Constructs a new instance of the given class
   *
   * @param cl - The class
   */
  public static Object newInstance(Class cl) {
    try {
      try {
        return cl.newInstance();
      } catch (Throwable err) {
        return rf.newConstructorForSerialization(cl, Object.class.getDeclaredConstructor()).newInstance();
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  public static Field setFieldAccessible(Field f) {
    try {
      f.setAccessible(true);
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      int modifiers = modifiersField.getInt(f);
      modifiersField.setInt(f, modifiers & -17);
      return f;
    } catch (Throwable ignored) {
    }
    return null;
  }
}