package androidx.test.internal.runner.lifecycle;
import androidx.test.runner.lifecycle.*;
import java.util.*;
public class ApplicationLifecycleMonitorImpl implements ApplicationLifecycleMonitor {
  private final List<ApplicationLifecycleCallback> cbs = new ArrayList<>();
  public ApplicationLifecycleMonitorImpl() {}
  public void addLifecycleCallback(ApplicationLifecycleCallback c) { cbs.add(c); }
  public void removeLifecycleCallback(ApplicationLifecycleCallback c) { cbs.remove(c); }
  public void signalLifecycleChange(android.app.Application app, ApplicationStage stage) { for (ApplicationLifecycleCallback c : new ArrayList<>(cbs)) c.onApplicationLifecycleChanged(app, stage); }
}
