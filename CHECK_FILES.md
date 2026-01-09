# File Verification Checklist

Run these commands in your repository to verify file naming:

```bash
# 1. Check what Java files you actually have
ls -la src/main/java/io/jenkins/plugins/hostmonitor/

# You should see EXACTLY these files:
# - HostMonitorManager.java
# - HostMonitorWidget.java  
# - MonitoredHost.java
# - UpdateHostStatusStep.java  (NOT UpdateHostStatus.java!)

# 2. If you see UpdateHostStatus.java, rename it:
cd src/main/java/io/jenkins/plugins/hostmonitor/
mv UpdateHostStatus.java UpdateHostStatusStep.java

# 3. Verify class names match file names:
grep "^public class" *.java

# Should output:
# HostMonitorManager.java:public class HostMonitorManager extends ManagementLink implements Saveable {
# HostMonitorWidget.java:public class HostMonitorWidget extends PageDecorator {
# MonitoredHost.java:public class MonitoredHost implements Serializable {
# UpdateHostStatusStep.java:public class UpdateHostStatusStep extends Step {

# 4. Then rebuild:
cd ../../../../../../../../..  # Back to project root
mvn clean package
```

## Most Likely Issue

Based on your original error:
```
class UpdateHostStatusStep is public, should be declared in a file named UpdateHostStatusStep.java
```

Your file is probably named `UpdateHostStatus.java` but needs to be `UpdateHostStatusStep.java`

**Fix:**
```bash
cd src/main/java/io/jenkins/plugins/hostmonitor/
mv UpdateHostStatus.java UpdateHostStatusStep.java
cd -
mvn clean package
```
