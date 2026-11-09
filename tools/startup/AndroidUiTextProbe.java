import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

/** adb shell CLASSPATH=probe.jar:shipping.apk app_process /system/bin AndroidUiTextProbe
 * Exercises the actual shipping DEX on Android ART/libcore, not JVM or a rewritten test renderer.
 */
public final class AndroidUiTextProbe {
    public static void main(String[] args) throws Exception {
        Class<?> type=Class.forName("dev.suicode.packages.UiTranslations");
        Object ui=type.getConstructor(List.class,String.class).newInstance(Collections.emptyList(),null);
        try {
            Object text=type.getMethod("text",String.class,java.util.Map.class).invoke(ui,"nav.files",Collections.emptyMap());
            if(!"Files".equals(text)) throw new AssertionError("Unexpected English fallback: "+text);
            System.out.println("SHIPPING_DEX_ENGLISH_STARTUP_PASS: "+text);
        } catch(InvocationTargetException failure) {
            System.err.println("SHIPPING_DEX_ENGLISH_STARTUP_FAILED");
            failure.getCause().printStackTrace();
            System.exit(10);
        }
    }
}
