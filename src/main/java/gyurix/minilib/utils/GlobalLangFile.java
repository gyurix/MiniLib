package gyurix.minilib.utils;

import gyurix.minilib.configfile.ConfigData;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class GlobalLangFile {
  public static final HashMap<String, HashMap<String, String>> map = new HashMap<>();

  public static boolean contains(String lang, String adr) {
    if (lang != null && !lang.isEmpty()) {
      HashMap<String, String> m = map.get(lang);
      return m != null && m.get(adr) != null;
    }
    return false;
  }

  public static String get(String lang, String adr) {
    HashMap<String, String> m;
    String msg;
    if (lang != null && !lang.isEmpty()) {
      m = map.get(lang);
      if (m != null) {
        msg = m.get(adr);
        if (msg != null) {
          return msg;
        }
        System.err.println("The requested key (" + adr + ") is missing from language " + lang + ".");
      }
      System.err.println("The requested language (" + lang + ") is not available.");
    }
    return lang + "." + adr;
  }

  private static void load(String[] data) {
    String adr = ".en";
    StringBuilder cs = new StringBuilder();
    int lvl = 0;
    int line = 0;
    for (String s : data) {
      int blockLvl = 0;
      ++line;
      while (s.charAt(blockLvl) == ' ') {
        ++blockLvl;
      }
      String[] d = ((s = s.substring(blockLvl)) + " ").split(" *: +", 2);
      if (d.length == 1) {
        s = ConfigData.unescape(s);
        if (cs.length() != 0) {
          cs.append('\n');
        }
        cs.append(s);
        continue;
      }
      put(adr.substring(1), cs.toString());
      cs.setLength(0);
      if (blockLvl == lvl + 2) {
        adr = adr + "." + d[0];
        lvl += 2;
      } else if (blockLvl == lvl) {
        adr = adr.substring(0, adr.lastIndexOf('.') + 1) + d[0];
      } else if (blockLvl < lvl && blockLvl % 2 == 0) {
        while (blockLvl != lvl) {
          lvl -= 2;
          adr = adr.substring(0, adr.lastIndexOf('.'));
        }
        adr = adr.substring(0, adr.lastIndexOf('.') + 1) + d[0];
      } else {
        throw new RuntimeException("Block leveling error in line " + line + "!");
      }
      if (d[1].isEmpty()) continue;
      cs.append(d[1].substring(0, d[1].length() - 1));
    }
    put(adr.substring(1), cs.toString());
  }

  public static PluginLang loadLF(String pn, InputStream stream, String fn) {
    try {
      byte[] bytes = new byte[stream.available()];
      stream.read(bytes);
      load(new String(bytes, "UTF-8").split("\r?\n"));
      load(new String(Files.readAllBytes(new File(fn).toPath()), "UTF-8").split("\r?\n"));
      return new PluginLang(pn);
    } catch (Throwable e) {
      e.printStackTrace();
      return null;
    }
  }

  public static PluginLang loadLF(String pn, String fn) {
    try {
      load(new String(Files.readAllBytes(new File(fn).toPath()), "UTF-8").split("\r?\n"));
      return new PluginLang(pn);
    } catch (Throwable e) {
      e.printStackTrace();
      return null;
    }
  }

  private static void put(String adr, String value) {
    if (!adr.contains(".")) {
      if (!map.containsKey(adr)) {
        map.put(adr, new HashMap());
      }
    } else {
      HashMap<String, String> m = map.get(adr.substring(0, adr.indexOf('.')));
      m.put(adr.substring(adr.indexOf('.') + 1), value);
    }
  }

  public static void unloadLF(PluginLang lng) {
    for (HashMap<String, String> m : map.values()) {
      Iterator<Entry<String, String>> i = m.entrySet().iterator();
      while (i.hasNext()) {
        Entry<String, String> e = i.next();
        if (!e.getKey().matches(".*\\." + lng.pluginName + ".*")) continue;
        i.remove();
      }
    }
  }

  public static class PluginLang {
    public final String pluginName;
    private String language = "en";

    private PluginLang(String plugin) {
      pluginName = plugin;
    }

    public boolean contains(String adr) {
      return GlobalLangFile.contains(language, pluginName + "." + adr);
    }

    public String get(String adr, Object... obj) {
      String msg = GlobalLangFile.get(language, pluginName + '.' + adr);
      Object key = null;
      for (Object o : obj) {
        if (key == null) {
          key = o;
          continue;
        }
        msg = msg.replace("<" + key + '>', String.valueOf(o));
        key = null;
      }
      return msg;
    }

    public String getLanguage() {
      return language;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public void msg(String adr, Object... obj) {
      System.out.println(get(adr, obj));
    }
  }

}

