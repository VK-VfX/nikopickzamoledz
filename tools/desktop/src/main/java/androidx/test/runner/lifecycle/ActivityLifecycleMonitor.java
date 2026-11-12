package androidx.test.runner.lifecycle;
import java.util.Collection;
public interface ActivityLifecycleMonitor {
  void addLifecycleCallback(ActivityLifecycleCallback c);
  void removeLifecycleCallback(ActivityLifecycleCallback c);
  Stage getLifecycleStageOf(android.app.Activity a);
  Collection<android.app.Activity> getActivitiesInStage(Stage s);
}
