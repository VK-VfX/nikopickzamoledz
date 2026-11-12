package androidx.test.platform.app;
import android.app.Instrumentation;
import android.os.Bundle;
public final class InstrumentationRegistry {
  private static Instrumentation instance; private static Bundle args = new Bundle();
  public static void registerInstance(Instrumentation i, Bundle b) { instance = i; args = b == null ? new Bundle() : b; }
  public static Instrumentation getInstrumentation() { if (instance == null) throw new IllegalStateException("No instrumentation registered"); return instance; }
  public static Bundle getArguments() { return args; }
}
