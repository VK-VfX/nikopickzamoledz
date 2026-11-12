package androidx.test.internal.runner.lifecycle;
import androidx.test.runner.lifecycle.*;
import java.util.*;
public class ActivityLifecycleMonitorImpl implements ActivityLifecycleMonitor {
  private final List<ActivityLifecycleCallback> cbs = new ArrayList<>();
  private final Map<android.app.Activity, Stage> stages = new HashMap<>();
  public ActivityLifecycleMonitorImpl() {}
  public void addLifecycleCallback(ActivityLifecycleCallback c) { cbs.add(c); }
  public void removeLifecycleCallback(ActivityLifecycleCallback c) { cbs.remove(c); }
  public Stage getLifecycleStageOf(android.app.Activity a) { Stage s = stages.get(a); if (s == null) throw new IllegalArgumentException("Unknown activity"); return s; }
  public Collection<android.app.Activity> getActivitiesInStage(Stage s) { List<android.app.Activity> r = new ArrayList<>(); for (Map.Entry<android.app.Activity, Stage> e : stages.entrySet()) if (e.getValue() == s) r.add(e.getKey()); return r; }
  public void signalLifecycleChange(Stage stage, android.app.Activity a) { stages.put(a, stage); for (ActivityLifecycleCallback c : new ArrayList<>(cbs)) c.onActivityLifecycleChanged(a, stage); }
}
