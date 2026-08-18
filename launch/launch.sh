#!/usr/bin/env bash
cd "$(dirname "$0")"
OS="$(uname -s)"
if [ "$OS" = "Linux" ]; then
    NATIVES="$(pwd)/natives/linux"
    GAMEDIR="$HOME/.minecraft"
else
    NATIVES="$(pwd)/natives/windows"
    GAMEDIR="$APPDATA/.minecraft"
fi
JAVA="$(pwd)/jre/bin/java"
if [ ! -f "$JAVA" ]; then
    echo "[Yuri] jre/bin/java not found. Make sure the jre/ folder is included."
    exit 1
fi
mkdir -p "$GAMEDIR"
echo "[Yuri] Using bundled JRE"
echo "[Yuri] Game dir: $GAMEDIR"
JAVA_LIB="$(pwd)/jre/lib/amd64"
if [ ! -d "$JAVA_LIB" ]; then
    JAVA_LIB="$(pwd)/jre/jre/lib/amd64"
fi
"$JAVA" -Dorg.lwjgl.librarypath="$NATIVES" -Djava.library.path="$NATIVES:$JAVA_LIB" -jar "Yuri.jar" --version Yuri --accessToken 0 --assetsDir assets --assetIndex 1.8 --gameDir "$GAMEDIR" --userProperties {}
