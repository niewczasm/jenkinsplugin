# Build Troubleshooting

If you're getting "release version 17 not supported" errors, try these solutions:

## Solution 1: Use the build script
```bash
./build.sh
```

## Solution 2: Set JAVA_HOME explicitly
```bash
# Find your Java 21 installation
which java

# Set JAVA_HOME (adjust path as needed)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Build
mvn clean package
```

## Solution 3: Use Maven with explicit Java version
```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn clean package
```

## Solution 4: Check Maven's Java version
Sometimes Maven uses a different Java than your shell:

```bash
mvn -version
# Look at the "Java version" line

# If it's not Java 21, update Maven's settings
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
mvn -version
```

## Solution 5: If you only have JDK 11 or 17

If you cannot install JDK 21, switch back to Java 11:

1. Edit `pom.xml` and change all occurrences of `21` to `11`:
   ```xml
   <java.level>11</java.level>
   <maven.compiler.source>11</maven.compiler.source>
   <maven.compiler.target>11</maven.compiler.target>
   <maven.compiler.release>11</maven.compiler.release>
   ```

2. Make sure Jenkins version is compatible (2.462.1+ for Java 11)

3. Build:
   ```bash
   mvn clean package
   ```

## Verify Your Setup

```bash
# Check Java version
java -version

# Check Maven's Java
mvn -version

# Both should show the same Java version
```
