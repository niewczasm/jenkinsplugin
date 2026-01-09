# Compilation Error Troubleshooting

The error log doesn't show the specific compilation error. Here's how to diagnose:

## Step 1: Get Detailed Error Output

Run one of these commands to see the actual compilation errors:

```bash
# Option 1: Verbose Maven output
mvn clean compile -X 2>&1 | grep -B 5 -A 20 "error:"

# Option 2: Just the compiler errors
mvn clean compile 2>&1 | grep "error:"

# Option 3: Full output (scroll to find errors)
mvn clean compile
```

## Step 2: Common Issues to Check

### Issue 1: File naming mismatch
You mentioned earlier that the error said:
```
class UpdateHostStatusStep is public, should be declared in a file named UpdateHostStatusStep.java
```

**Check your actual file location:**
```bash
# The file MUST be named exactly as the class
ls -la src/main/java/io/jenkins/plugins/hostmonitor/UpdateHostStatus*.java

# Should show ONLY:
# UpdateHostStatusStep.java
```

If you have a file named `UpdateHostStatus.java`, rename it:
```bash
cd src/main/java/io/jenkins/plugins/hostmonitor/
mv UpdateHostStatus.java UpdateHostStatusStep.java
```

### Issue 2: Check all Java files exist with correct names

```bash
cd src/main/java/io/jenkins/plugins/hostmonitor/
ls -la *.java

# Should show:
# HostMonitorManager.java
# HostMonitorWidget.java
# MonitoredHost.java
# UpdateHostStatusStep.java
```

### Issue 3: Verify the class names match file names

```bash
# Check each file's class name
grep "public class" src/main/java/io/jenkins/plugins/hostmonitor/*.java

# Should output:
# HostMonitorManager.java:public class HostMonitorManager extends ManagementLink implements Saveable {
# HostMonitorWidget.java:public class HostMonitorWidget extends PageDecorator {
# MonitoredHost.java:public class MonitoredHost implements Serializable {
# UpdateHostStatusStep.java:public class UpdateHostStatusStep extends Step {
```

## Step 3: If the issue is file naming

If you accidentally have `UpdateHostStatus.java` instead of `UpdateHostStatusStep.java`:

```bash
cd src/main/java/io/jenkins/plugins/hostmonitor/
mv UpdateHostStatus.java UpdateHostStatusStep.java
mvn clean compile
```

## Step 4: Clean rebuild

Sometimes Maven cache causes issues:

```bash
mvn clean
rm -rf target/
mvn compile
```

## Step 5: Send me the actual error

Run this and send me the output:

```bash
mvn clean compile 2>&1 | grep -A 10 "COMPILATION ERROR" || mvn clean compile 2>&1 | grep "\.java:\[" | head -20
```

This will show the actual Java compilation errors.
