package gyurix.minilib.utils;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Random;

/**
 * Created by GyuriX on 2016. 12. 21..
 */
public class SU {
  public static final Charset utf8 = Charset.forName("UTF-8");
  public static Random rand = new Random();

  public static void checkNull(Object... objects) {
    String msg = null;
    for (Object o : objects) {
      if (msg == null) {
        msg = String.valueOf(o);
        continue;
      }
      if (o == null) {
        throw new NullPointerException(msg + " can not be null!");
      }
      msg = null;
    }
  }

  /**
   * Escape multi line text to a single line one
   *
   * @param text multi line escapeable text input
   * @return The escaped text
   */
  public static String escapeText(String text) {
    return text.replace("\\", "\\\\")
            .replace("_", "\\_")
            .replace("|", "\\|")
            .replace(" ", "_")
            .replace("\n", "|");
  }

  /**
   * Fills variables in a String
   *
   * @param s    - The String
   * @param vars - The variables and their values, which should be filled
   * @return The variable filled String
   */
  public static String fillVariables(String s, Object... vars) {
    String last = null;
    for (Object v : vars) {
      if (last == null)
        last = (String) v;
      else {
        s = s.replace('<' + last + '>', String.valueOf(v));
        last = null;
      }
    }
    return s;
  }

  /**
   * Generates a random number between min (inclusive) and max (exclusive)
   *
   * @param min - Minimal value of the random number
   * @param max - Maximal value of the random number
   * @return A random double between min and max
   */
  public static double rand(double min, double max) {
    return rand.nextDouble() * Math.abs(max - min) + min;
  }

  /**
   * Generate a configurable random color
   *
   * @param minSaturation - Minimal saturation (0-1)
   * @param maxSaturation - Maximal saturation (0-1)
   * @param minLuminance  - Minimal luminance (0-1)
   * @param maxLuminance  - Maximal luminance (0-1)
   * @return The generated random color
   */
  public static Color randColor(double minSaturation, double maxSaturation, double minLuminance, double maxLuminance) {
    float hue = rand.nextFloat();
    float saturation = (float) rand(minSaturation, maxSaturation);
    float luminance = (float) rand(minLuminance, maxLuminance);
    return java.awt.Color.getHSBColor(hue, saturation, luminance);
  }

  /**
   * Convert String to the given class
   *
   * @param str - Convertable String
   * @param cl  - Target class
   * @return The converted Object
   */
  public static Object convert(String str, Class cl) {
    if (str == null || str.equals("null"))
      return null;
    cl = Primitives.wrap(cl);
    try {
      return Reflection.getConstructor(cl, String.class).newInstance(str);
    } catch (Throwable ignored) {
    }
    try {
      return Reflection.getMethod(cl, "valueOf", String.class).invoke(null, str);
    } catch (Throwable ignored) {
    }
    try {
      Method m = Reflection.getMethod(cl, "fromString", String.class);
      return m.invoke(null, str);
    } catch (Throwable ignored) {

    }
    System.err.println("Failed to convert \"" + str + "\" to class " + cl.getName());
    return null;
  }

  public static void saveFiles(String... files) {
    try {
      for (String fn : files) {
        checkNull("File name", fn);
        File f = new File(fn);
        if (f.exists())
          return;
        transloadStream(SU.class.getResourceAsStream("/" + fn), new FileOutputStream(f));
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public static byte[] streamToBytes(InputStream is) throws IOException {
    byte[] buf = new byte[2048];
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    for (int i = is.read(buf, 0, 2048); i > 0; i = is.read(buf, 0, 2048))
      bos.write(buf, 0, i);
    is.close();
    return bos.toByteArray();
  }

  public static void transloadStream(InputStream is, OutputStream os) throws IOException {
    checkNull("InputStream", is, "OutputSteam", os);
    byte[] buf = new byte[4096];
    for (int done = is.read(buf); done > 0; done = is.read(buf))
      os.write(buf, 0, done);
    is.close();
  }

  /**
   * Unescape multi line to single line escaped text
   *
   * @param text multi line escaped text input
   * @return The unescaped text
   */

  public static String unescapeText(String text) {
    return (' ' + text).replaceAll("([^\\\\])_", "$1 ")
            .replaceAll("([^\\\\])\\|", "$1\n")
            .replaceAll("([^\\\\])\\\\([_\\|])", "$1$2")
            .replace("\\\\", "\\").substring(1);
  }
}
