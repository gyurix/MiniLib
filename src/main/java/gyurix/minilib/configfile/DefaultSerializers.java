package gyurix.minilib.configfile;


import gyurix.minilib.utils.Primitives;
import gyurix.minilib.utils.Reflection;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static gyurix.minilib.configfile.ConfigData.serializeObject;
import static gyurix.minilib.configfile.ConfigSerialization.*;
import static gyurix.minilib.utils.Reflection.newInstance;

public class DefaultSerializers {
  public static final Type[] eta = new Type[0];
  public static int leftPad;

  public static void init() {
    serializers.put(String.class, new StringSerializer());
    serializers.put(Class.class, new ClassSerializer());
    serializers.put(UUID.class, new UUIDSerializer());
    serializers.put(ConfigData.class, new ConfigDataSerializer());

    NumberSerializer numberSerializer = new NumberSerializer();
    serializers.put(Array.class, new ArraySerializer());
    serializers.put(Boolean.class, new BooleanSerializer());
    serializers.put(Byte.class, numberSerializer);
    serializers.put(Character.class, new CharacterSerializer());
    serializers.put(Collection.class, new CollectionSerializer());
    serializers.put(Double.class, numberSerializer);
    serializers.put(Float.class, numberSerializer);
    serializers.put(Integer.class, numberSerializer);
    serializers.put(Long.class, numberSerializer);
    serializers.put(Map.class, new MapSerializer());
    serializers.put(Object.class, new ObjectSerializer());
    serializers.put(Pattern.class, new PatternSerializer());
    serializers.put(Short.class, numberSerializer);
    serializers.put(SimpleDateFormat.class, new SimpleDateFormatSerializer());

    aliases.put(Array.class, "[]");
    aliases.put(Boolean.class, "bool");
    aliases.put(Byte.class, "b");
    aliases.put(Character.class, "c");
    aliases.put(Collection.class, "{}");
    aliases.put(Double.class, "d");
    aliases.put(Float.class, "f");
    aliases.put(Integer.class, "i");
    aliases.put(LinkedHashMap.class, "<L>");
    aliases.put(LinkedHashSet.class, "{LS}");
    aliases.put(List.class, "{L}");
    aliases.put(Long.class, "l");
    aliases.put(Map.class, "<>");
    aliases.put(Object.class, "?");
    aliases.put(Set.class, "{S}");
    aliases.put(Short.class, "s");
    aliases.put(String.class, "str");
    aliases.put(TreeMap.class, "<T>");
    aliases.put(TreeSet.class, "{TS}");
    aliases.put(UUID.class, "uuid");
    interfaceBasedClasses.put(List.class, ArrayList.class);
    interfaceBasedClasses.put(Set.class, HashSet.class);
    interfaceBasedClasses.put(Map.class, HashMap.class);
  }

  public static class ArraySerializer implements Serializer {
    public Object fromData(ConfigData input, Class fixClass, Type... parameterTypes) {
      Class cl = Object.class;
      Type[] types = eta;
      if (parameterTypes.length >= 1) {
        if (parameterTypes[0] instanceof ParameterizedType) {
          ParameterizedType pt = (ParameterizedType) parameterTypes[0];
          cl = (Class) pt.getRawType();
          types = pt.getActualTypeArguments();
        } else {
          cl = (Class) parameterTypes[0];
        }
      }
      if (input.listData != null) {
        Object ar = Array.newInstance(cl, input.listData.size());
        int i = 0;
        for (ConfigData d : input.listData) {
          Array.set(ar, i++, d.deserialize(cl, types));
        }
        return ar;
      } else {
        String[] sd = input.stringData.split(";");
        Object ar = Array.newInstance(cl, sd.length);
        int i = 0;
        for (String d : sd) {
          Array.set(ar, i++, new ConfigData(d).deserialize(cl, types));
        }
        return ar;
      }
    }


    public ConfigData toData(Object input, Type... parameters) {
      Class cl = parameters.length >= 1 ? (Class) parameters[0] : Object.class;
      ConfigData d = new ConfigData();
      d.listData = new ArrayList<>();
      if (input instanceof Object[])
        for (Object o : Arrays.asList((Object[]) input)) {
          d.listData.add(serializeObject(o, o.getClass() != cl));
        }
      else {
        int len = Array.getLength(input);
        for (int i = 0; i < len; ++i) {
          Object o = Array.get(input, i);
          d.listData.add(serializeObject(o, o.getClass() != cl));
        }
      }
      return d;
    }
  }

  public static class BooleanSerializer implements Serializer {
    public Object fromData(ConfigData input, Class cl, Type... parameters) {
      String s = input.stringData.toLowerCase();
      return s.equals("+") || s.equals("true") || s.equals("yes");
    }

    public ConfigData toData(Object in, Type... parameters) {
      return new ConfigData((boolean) in ? "+" : "-");
    }
  }

  public static class CharacterSerializer implements Serializer {
    public Object fromData(ConfigData input, Class cl, Type... parameters) {
      return input.stringData.charAt(0);
    }

    public ConfigData toData(Object in, Type... parameters) {
      return new ConfigData(String.valueOf(in));
    }
  }

  private static class ClassSerializer implements Serializer {
    ClassSerializer() {
    }

    public Object fromData(ConfigData input, Class cl, Type... parameters) {
      try {
        return Class.forName(input.stringData);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      return null;
    }

    public ConfigData toData(Object input, Type... parameters) {
      return new ConfigData(((Class) input).getName());
    }
  }

  public static class CollectionSerializer implements Serializer {
    public Object fromData(ConfigData input, Class fixClass, Type... parameterTypes) {
      try {
        Collection col = (Collection) fixClass.newInstance();
        Class cl;
        Type[] types;
        ParameterizedType pt;
        cl = Object.class;
        types = eta;
        if (parameterTypes.length >= 1) {
          if (parameterTypes[0] instanceof ParameterizedType) {
            pt = (ParameterizedType) parameterTypes[0];
            cl = (Class) pt.getRawType();
            types = pt.getActualTypeArguments();
          } else {
            cl = (Class) parameterTypes[0];
          }
        }
        if (input.listData != null) {
          for (ConfigData d : input.listData) {
            col.add(d.deserialize(cl, types));
          }
        } else if (input.stringData != null && !input.stringData.isEmpty()) {
          for (String s : input.stringData.split("[;,] *"))
            col.add(new ConfigData(s).deserialize(cl, types));
        }
        return col;
      } catch (Throwable e) {
        e.printStackTrace();
      }
      return null;
    }


    public ConfigData toData(Object input, Type... parameters) {
      Type[] types = eta;
      Class cl = Object.class;
      if (parameters.length >= 1) {
        if (parameters[0] instanceof ParameterizedType) {
          ParameterizedType key = (ParameterizedType) parameters[0];
          types = key.getActualTypeArguments();
          cl = (Class) key.getRawType();
        } else {
          cl = (Class) parameters[0];
        }
      }
      if (((Collection) input).isEmpty())
        return new ConfigData("");
      ConfigData d = new ConfigData();
      d.listData = new ArrayList<>();
      for (Object o : (Collection) input)
        d.listData.add(serializeObject(o, o.getClass() != cl, types));
      return d;
    }
  }

  public static class ConfigDataSerializer implements Serializer {
    public Object fromData(ConfigData data, Class cl, Type... type) {
      return data;
    }

    public ConfigData toData(Object data, Type... type) {
      return (ConfigData) data;
    }
  }

  public static class MapSerializer implements Serializer {
    public Object fromData(ConfigData input, Class fixClass, Type... parameterTypes) {
      try {
        Map map;
        if (fixClass == EnumMap.class)
          map = new EnumMap((Class) parameterTypes[0]);
        else
          map = (Map) fixClass.newInstance();
        Class keyClass;
        Type[] keyTypes;
        Class valueClass;
        Type[] valueTypes;
        ParameterizedType pt;
        if (input.mapData != null) {
          keyClass = Object.class;
          keyTypes = eta;
          if (parameterTypes.length >= 1) {
            if (parameterTypes[0] instanceof ParameterizedType) {
              pt = (ParameterizedType) parameterTypes[0];
              keyClass = (Class) pt.getRawType();
              keyTypes = pt.getActualTypeArguments();
            } else {
              keyClass = (Class) parameterTypes[0];
            }
          }
          boolean dynamicValueCl = ValueClassSelector.class.isAssignableFrom(keyClass);
          valueClass = Object.class;
          valueTypes = eta;
          if (!dynamicValueCl && parameterTypes.length >= 2) {
            if (parameterTypes[1] instanceof ParameterizedType) {
              pt = (ParameterizedType) parameterTypes[1];
              valueClass = (Class) pt.getRawType();
              valueTypes = pt.getActualTypeArguments();
            } else {
              valueClass = (Class) parameterTypes[1];
            }
          }
          if (dynamicValueCl) {
            for (Entry<ConfigData, ConfigData> e : input.mapData.entrySet()) {
              try {
                ValueClassSelector key = (ValueClassSelector) e.getKey().deserialize(keyClass, keyTypes);
                map.put(key, e.getValue().deserialize(key.getValueClass(), key.getValueTypes()));
              } catch (Throwable err) {
                System.err.println("§cMap element deserialization error:\n§eKey = §f" + e.getKey() + "§e; Value = §f" + e.getValue());
                err.printStackTrace();
              }
            }
          } else {
            for (Entry<ConfigData, ConfigData> e : input.mapData.entrySet()) {
              try {
                map.put(e.getKey().deserialize(keyClass, keyTypes), e.getValue().deserialize(valueClass, valueTypes));
              } catch (Throwable err) {
                System.err.println("§cMap element deserialization error:\n§eKey = §f" + e.getKey() + "§e; Value = §f" + e.getValue());
                err.printStackTrace();
              }
            }
          }
        }
        return map;
      } catch (Throwable e) {
        e.printStackTrace();
        //e.printStackTrace();
      }
      return null;
    }


    public ConfigData toData(Object input, Type... parameters) {
      try {
        if (((Map) input).isEmpty())
          return new ConfigData();
        Class keyClass = Object.class;
        Class valueClass = Object.class;
        Type[] keyTypes = eta;
        Type[] valueTypes = eta;
        if (parameters.length >= 1) {
          if (parameters[0] instanceof ParameterizedType) {
            ParameterizedType key = (ParameterizedType) parameters[0];
            keyTypes = key.getActualTypeArguments();
            keyClass = (Class) key.getRawType();
          } else {
            keyClass = (Class) parameters[0];
          }
        }
        boolean valueClassSelector = keyClass.isAssignableFrom(ValueClassSelector.class);
        if (!valueClassSelector && parameters.length >= 2) {
          if (parameters[1] instanceof ParameterizedType) {
            ParameterizedType value = (ParameterizedType) parameters[1];
            valueTypes = value.getActualTypeArguments();
            valueClass = (Class) value.getRawType();
          } else {
            valueClass = (Class) parameters[1];
          }
        }

        ConfigData d = new ConfigData();
        d.mapData = new LinkedHashMap();
        for (Entry<?, ?> e : ((Map<?, ?>) input).entrySet()) {
          Object key = e.getKey();
          Object value = e.getValue();
          if (key != null && value != null)
            d.mapData.put(serializeObject(key, key.getClass() != keyClass, keyTypes),
                    serializeObject(value, !valueClassSelector && value.getClass() != valueClass, valueTypes));
        }
        return d;
      } catch (Throwable e) {
        e.printStackTrace();
        return null;
      }
    }
  }

  public static class NumberSerializer implements Serializer {
    public static final HashMap<Class, Method> methods = new HashMap();

    static {
      try {
        methods.put(Short.class, Short.class.getMethod("decode", String.class));
        methods.put(Integer.class, Integer.class.getMethod("decode", String.class));
        methods.put(Long.class, Long.class.getMethod("decode", String.class));
        methods.put(Float.class, Float.class.getMethod("valueOf", String.class));
        methods.put(Double.class, Double.class.getMethod("valueOf", String.class));
        methods.put(Byte.class, Byte.class.getMethod("valueOf", String.class));
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }

    public Object fromData(ConfigData input, Class fixClass, Type... parameters) {
      Method m = methods.get(Primitives.wrap(fixClass));
      try {
        String s = StringUtils.stripStart(input.stringData.replace(" ", ""), "0");
        if (s.endsWith("."))
          s = s + "0";
        return m.invoke(null, s.isEmpty() ? "0" : s);
      } catch (Throwable e) {
        System.out.println("INVALID NUMBER: " + fixClass.getName() + " - \"" + input + "\"");
      }
      return null;
    }

    public ConfigData toData(Object input, Type... parameters) {
      String s = input.toString();
      int id = (s + ".").indexOf(".");
      return new ConfigData(StringUtils.leftPad(s, Math.max(leftPad + s.length() - id, 0), '0'));

    }
  }

  public static class ObjectSerializer implements Serializer {
    public Object fromData(ConfigData input, Class fixClass, Type... parameters) {
      ConfigOptions co = (ConfigOptions) fixClass.getAnnotation(ConfigOptions.class);
      try {
        if (fixClass.isEnum() || fixClass.getSuperclass() != null && fixClass.getSuperclass().isEnum()) {
          if (input.stringData == null || input.stringData.equals(""))
            return null;
          for (Object en : fixClass.getEnumConstants()) {
            if (en.toString().equals(input.stringData))
              return en;
          }
          return null;
        }
        if (ArrayUtils.contains(fixClass.getInterfaces(), StringSerializable.class) || fixClass == BigDecimal.class || fixClass == BigInteger.class) {
          if (input.stringData == null || input.stringData.equals(""))
            return null;
          return fixClass.getConstructor(String.class).newInstance(input.stringData);
        }
      } catch (Throwable e) {
        System.err.println("Error on deserializing \"" + input.stringData + "\" to a " + fixClass.getName() + " object.");
        e.printStackTrace();
        return null;
      }


      Object obj = newInstance(fixClass);
      if (input.mapData == null)
        return obj;
      for (Field f : Reflection.getAllFields(fixClass)) {
        f.setAccessible(true);
        try {
          if (f.getType() == Class.class || f.getType().getName().startsWith("java.lang.reflect."))
            continue;
          String fn = f.getName();
          ConfigData d = input.mapData.get(new ConfigData(fn));
          Class cl = f.getType();
          if (d != null) {
            Type[] types = f.getGenericType() instanceof ParameterizedType ? ((ParameterizedType) f.getGenericType()).getActualTypeArguments() : cl.isArray() ? new Type[]{cl.getComponentType()} : eta;
            Object out = d.deserialize(ConfigSerialization.getNotInterfaceClass(cl), types);
            if (out != null)
              f.set(obj, out);
          }
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
      try {
        if (ArrayUtils.contains(fixClass.getInterfaces(), PostLoadable.class))
          ((PostLoadable) obj).postLoad();
      } catch (Throwable e) {
        System.err.println("Error on post loading " + fixClass.getName() + " object.");
        e.printStackTrace();
      }
      return obj;
    }

    public ConfigData toData(Object obj, Type... parameters) {
      Class c = Primitives.wrap(obj.getClass());
      if (c.isEnum() || c.getSuperclass() != null && c.getSuperclass().isEnum() || ArrayUtils.contains(c.getInterfaces(), StringSerializable.class) || c == BigDecimal.class || c == BigInteger.class) {
        return new ConfigData(obj.toString());
      }
      ConfigOptions dfOptions = (ConfigOptions) c.getAnnotation(ConfigOptions.class);
      String dfValue = dfOptions == null ? "null" : dfOptions.defaultValue();
      boolean dfSerialize = dfOptions == null || dfOptions.serialize();
      String comment = dfOptions == null ? "" : dfOptions.comment();
      ConfigData out = new ConfigData();
      if (!comment.isEmpty())
        out.comment = comment;
      out.mapData = new LinkedHashMap();
      for (Field f : Reflection.getAllFields(c)) {
        try {
          String dffValue = dfValue;
          comment = "";
          ConfigOptions options = f.getAnnotation(ConfigOptions.class);
          if (options != null) {
            if (!options.serialize())
              continue;
            dffValue = options.defaultValue();
            comment = options.comment();
          }
          if (!dfSerialize)
            continue;
          Object o = f.get(obj);
          if (o != null && !o.toString().matches(dffValue) && !((o instanceof Iterable) && !((Iterable) o).iterator().hasNext())) {
            String cn = ConfigSerialization.calculateClassName(Primitives.wrap(f.getType()), o.getClass());
            Class check = f.getType().isArray() ? f.getType().getComponentType() : f.getType();
            if (check == Class.class || check.getName().startsWith("java.lang.reflect."))
              continue;
            String fn = f.getName();
            Type t = f.getGenericType();
            ConfigData value = serializeObject(o, !cn.isEmpty(),
                    t instanceof ParameterizedType ?
                            ((ParameterizedType) t).getActualTypeArguments() :
                            ((Class) t).isArray() ?
                                    new Type[]{((Class) t).getComponentType()} :
                                    eta);
            out.mapData.put(new ConfigData(fn, comment), value);
          }
        } catch (Throwable ignored) {
        }
      }
      return out;
    }
  }

  public static class PatternSerializer implements Serializer {
    public Object fromData(ConfigData data, Class paramClass, Type... paramVarArgs) {
      return Pattern.compile(data.stringData);
    }

    public ConfigData toData(Object pt, Type... paramVarArgs) {
      return new ConfigData(((Pattern) pt).pattern());
    }
  }

  public static class SimpleDateFormatSerializer implements Serializer {
    public static final Field patternF = Reflection.getField(SimpleDateFormat.class, "pattern");

    public Object fromData(ConfigData input, Class cl, Type... parameters) {
      return new SimpleDateFormat(input.stringData);
    }

    public ConfigData toData(Object input, Type... parameters) {
      try {
        return new ConfigData((String) patternF.get(input));
      } catch (Throwable e) {
        e.printStackTrace();
      }
      return new ConfigData();
    }
  }

  public static class StringSerializer implements Serializer {
    public Object fromData(ConfigData input, Class cl, Type... parameters) {
      return input.stringData;
    }

    public ConfigData toData(Object input, Type... parameters) {
      return new ConfigData((String) input);
    }
  }

  public static class UUIDSerializer implements Serializer {
    public Object fromData(ConfigData input, Class cl, Type... parameters) {
      return UUID.fromString(input.stringData);
    }

    public ConfigData toData(Object input, Type... parameters) {
      return new ConfigData(input.toString());
    }
  }

}