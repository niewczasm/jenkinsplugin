# Build Phase Troubleshooting

Since compilation seems to be working (you're seeing Stapler generation), the failure is likely in another Maven phase.

## Get Specific Phase Error

Run this to see exactly where it fails:

```bash
mvn clean package -e 2>&1 | grep -E "Building|Failed to execute goal" | tail -20
```

This will show which Maven plugin/goal is actually failing.

## Common Post-Compilation Issues

### 1. Test Compilation Failure
If tests are failing to compile, skip them temporarily:
```bash
mvn clean package -DskipTests
```

### 2. FindBugs/SpotBugs Issues
Try disabling static analysis:
```bash
mvn clean package -Dfindbugs.skip=true -Dspotbugs.skip=true
```

### 3. Enforcer Plugin Issues (dependency conflicts)
Already fixed, but if it happens again:
```bash
mvn clean package -Denforcer.skip=true
```

### 4. Localizer/Messages Generation
Sometimes Jenkins message generation fails:
```bash
mvn clean package -Dmaven.test.skip=true -Dlocalizer.skip=true
```

### 5. Complete Bypass (for testing only)
To just get the HPI file:
```bash
mvn clean package -DskipTests -Dfindbugs.skip=true -Denforcer.skip=true -Dmaven.javadoc.skip=true
```

## Debug Build Process

Run the full build and save output:
```bash
./run-full-build.sh
```

Then check `build-output.log` for the exact error.

## Get Just the Error Section

```bash
mvn clean package 2>&1 | grep -A 30 "BUILD FAILURE"
```

## What to Send Me

Please run one of these and send the output:

**Option 1 (recommended):**
```bash
mvn clean package -e 2>&1 | grep -B 10 -A 10 "Failed to execute goal"
```

**Option 2:**
```bash
mvn clean package 2>&1 | tail -100
```

This will show the actual error that's causing the build to fail.
