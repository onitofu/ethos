#!/usr/bin/env bash
set -euo pipefail

# Plugin template setup script
# Usage: ./setup.sh
# Interactively creates a new plugin from this template

read -rp "Plugin name (e.g. mob-spawn-control): " PLUGIN_NAME
read -rp "Java package suffix (e.g. mob_spawn_control): " PLUGIN_PACKAGE
read -rp "Main class name (e.g. MobSpawnToggle): " PLUGIN_CLASS
read -rp "Command name (e.g. mobspawn): " COMMAND_NAME
read -rp "Command description: " COMMAND_DESC
read -rp "Permission node (e.g. mobspawn.admin): " PERMISSION
read -rp "Permission description: " PERMISSION_DESC

echo ""
echo "=== Summary ==="
echo "Plugin name:    $PLUGIN_NAME"
echo "Package:        ru.nyansus.mc.$PLUGIN_PACKAGE"
echo "Main class:     $PLUGIN_CLASS"
echo "Command:        /$COMMAND_NAME"
echo "Permission:     $PERMISSION"
echo ""
read -rp "Proceed? [Y/n] " CONFIRM
if [[ "${CONFIRM,,}" == "n" ]]; then
    echo "Aborted."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Rename directories
MAIN_SRC="src/main/java/ru/nyansus/mc"
TEST_SRC="src/test/java/ru/nyansus/mc"

mv "$MAIN_SRC/TEMPLATE_PLUGIN_PACKAGE" "$MAIN_SRC/$PLUGIN_PACKAGE"
mv "$TEST_SRC/TEMPLATE_PLUGIN_PACKAGE" "$TEST_SRC/$PLUGIN_PACKAGE"

# Rename files with class name
mv "$MAIN_SRC/$PLUGIN_PACKAGE/TEMPLATE_PLUGIN_CLASS.java" \
   "$MAIN_SRC/$PLUGIN_PACKAGE/$PLUGIN_CLASS.java"
mv "$TEST_SRC/$PLUGIN_PACKAGE/TEMPLATE_PLUGIN_CLASSTest.java" \
   "$TEST_SRC/$PLUGIN_PACKAGE/${PLUGIN_CLASS}Test.java"

# Replace placeholders in all text files
find . -type f \( -name "*.java" -o -name "*.yml" -o -name "*.gradle" -o -name "*.xml" -o -name "*.md" \) \
    -not -path "./.git/*" -not -path "./.gradle/*" -not -path "./build/*" | while read -r file; do
    sed -i \
        -e "s/TEMPLATE_PLUGIN_NAME/$PLUGIN_NAME/g" \
        -e "s/TEMPLATE_PLUGIN_PACKAGE/$PLUGIN_PACKAGE/g" \
        -e "s/TEMPLATE_PLUGIN_CLASS/$PLUGIN_CLASS/g" \
        -e "s/TEMPLATE_COMMAND_NAME/$COMMAND_NAME/g" \
        -e "s|TEMPLATE_COMMAND_DESCRIPTION|$COMMAND_DESC|g" \
        -e "s/TEMPLATE_PERMISSION_DESCRIPTION/$PERMISSION_DESC/g" \
        -e "s/TEMPLATE_PERMISSION/$PERMISSION/g" \
        "$file"
done

# Remove this script after setup
rm -- "$0"

echo ""
echo "Done! Plugin '$PLUGIN_NAME' is ready."
echo "Run './gradlew build' to verify."
