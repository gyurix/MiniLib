package gyurix.minilib.utils;

import gyurix.minilib.configfile.ConfigData;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static gyurix.minilib.configfile.DefaultSerializers.eta;

public class VariableAPI {
  public static final ArrayList<Object> emptyList = new ArrayList<>();
  public static final HashMap<String, VariableHandler> handlers = new HashMap();
  private static final HashSet<String> errorVars = new HashSet<>();
  private static final HashSet<String> missingHandlers = new HashSet<>();

  public static ArrayList<Object> fill(String msg, int from, Object[] oArgs) {
    int l = msg.length();
    int sid = from;
    ArrayList<Object> out = new ArrayList<>();
    for (int i = from; i < l; ++i) {
      char c = msg.charAt(i);
      if (c == '<') {
        if (sid < i) {
          out.add(msg.substring(sid, i));
        }
        ArrayList<Object> d = fill(msg, i + 1, oArgs);
        i = (Integer) d.get(0);
        sid = i + 1;
        d.remove(0);
        out.add(fillVar(d, oArgs));
        continue;
      }
      if (c != '>') continue;
      if (sid < i) {
        out.add(msg.substring(sid, i));
      }
      out.add(0, i);
      return out;
    }
    if (sid < msg.length()) {
      out.add(msg.substring(sid, msg.length()));
    }
    out.add(0, msg.length() - 1);
    return out;
  }

  public static Object fillVar(List<Object> inside, Object[] oArgs) {
    StringBuilder sb = new StringBuilder();
    int l = inside.size();
    for (int c = 0; c < l; ++c) {
      String os = String.valueOf(inside.get(c));
      int id = os.indexOf(58);
      if (id != -1) {
        sb.append(os.substring(0, id));
        ArrayList<Object> list = new ArrayList<>(inside.subList(c + 1, l));
        if (id != os.length() - 1) {
          list.add(0, os.substring(id + 1));
        }
        return handle(sb.toString(), list, oArgs);
      }
      sb.append(os);
    }
    return handle(sb.toString(), emptyList, oArgs);
  }

  public static void fillVariables(GlobalLangFile.PluginLang lang, Object o, String subLang) {
    for (Field f : Reflection.getAllFields(o.getClass())) {
      String key = subLang + ".defaults." + f.getName();
      if (lang.contains(key)) {
        Type t = f.getGenericType();
        Type[] types = t instanceof ParameterizedType ? ((ParameterizedType) t).getActualTypeArguments() : eta;
        ConfigData cd = new ConfigData(fillVariables(lang.get(key)));
        try {
          f.set(o, cd.deserialize(f.getType(), types));
        } catch (Throwable e) {
          System.err.println("Failed to set default value of field " + f.getName() + " to \"" + cd + "\"");
        }
      }
    }
  }

  public static String fillVariables(String msg, Object... oArgs) {
    ArrayList<Object> out = fill(msg.replace("\\<", "\u0000").replace("\\>", "\u0001"), 0, oArgs);
    out.remove(0);
    String s = StringUtils.join(out, "").replace('\u0000', '<').replace('\u0001', '>');
    return s;
  }

  private static Object handle(String var, ArrayList<Object> inside, Object[] oArgs) {
    VariableHandler vh = handlers.get(var);
    if (vh == null) {
      if (missingHandlers.add(var))
        System.out.println("Missing handler for variable " + var + "!");
      return "<" + var + ">";
    }
    try {
      return vh.getValue(inside, oArgs);
    } catch (Throwable e) {
      if (errorVars.add(var)) {
        System.out.println("Error on calculating variable " + var + "!");
        e.printStackTrace();
      }
      return '<' + var + '>';
    }
  }

  public interface VariableHandler {
    Object getValue(ArrayList<Object> args, Object[] eArgs);
  }

}

