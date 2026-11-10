#!/bin/bash
set -e

# Resolve Java & Jar binary (uses JAVA_HOME or system PATH first)
JAVA_CMD="javac"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVA_CMD="$JAVA_HOME/bin/javac"
elif [ -x "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/javac" ]; then
    JAVA_CMD="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/javac"
fi

JAR_CMD="jar"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/jar" ]; then
    JAR_CMD="$JAVA_HOME/bin/jar"
elif [ -x "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/jar" ]; then
    JAR_CMD="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/jar"
fi

M2_REPO="${HOME}/.m2/repository"

echo "1. Compiling Java classes..."
"$JAVA_CMD" -cp "${M2_REPO}/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar:${M2_REPO}/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar" -d target/classes src/main/java/com/orgchat/*.java

echo "2. Ensuring keystore.p12 is present in target classes..."
cp keystore.p12 target/classes/keystore.p12 2>/dev/null || true

echo "3. Packaging standalone executable SecureOrg-client.jar..."
"$JAR_CMD" cfe SecureOrg-client.jar com.orgchat.ChatClient \
  -C target/classes com/orgchat/ChatClient.class \
  -C target/classes com/orgchat/ChatFrame.class \
  -C target/classes "com/orgchat/ChatFrame\$1.class" \
  -C target/classes "com/orgchat/ChatFrame\$2.class" \
  -C target/classes com/orgchat/LoginFrame.class \
  -C target/classes "com/orgchat/LoginFrame\$1.class" \
  -C target/classes "com/orgchat/LoginFrame\$2.class" \
  -C target/classes keystore.p12

echo "SUCCESS: Created SecureOrg-client.jar in project root!"
