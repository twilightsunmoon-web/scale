#!/usr/bin/env bash
# 把 index.html 打成安卓 APK。不需要 Android Studio / Gradle。
#
# 依赖：JDK 11+（含 keytool）、curl、python、unzip
# 首次运行会自动下载 Android build-tools 与 platform 到 .sdk/（约 120MB）
#
# 用法：  bash tools/build-apk.sh
# 产物：  dist/scale.apk

set -e
cd "$(dirname "$0")/.."
ROOT=$PWD
SDK=$ROOT/.sdk
BT=$SDK/build-tools/34
JAR=$SDK/platforms/android-34/android.jar
BUILD=$ROOT/.build
KS=$ROOT/tools/scale.keystore

# ---------- 1. 准备工具链 ----------
if [ ! -f "$BT/aapt2.exe" ] || [ ! -f "$JAR" ]; then
  echo "[1/5] 下载 Android 构建工具（只需一次，约 120MB）..."
  mkdir -p "$SDK/.tmp"
  curl -L -o "$SDK/.tmp/bt.zip"      https://dl.google.com/android/repository/build-tools_r34-windows.zip
  curl -L -o "$SDK/.tmp/platform.zip" https://dl.google.com/android/repository/platform-34-ext7_r03.zip
  python - <<'PY'
import zipfile, os, shutil
sdk = os.path.abspath('.sdk')
os.makedirs(os.path.join(sdk,'build-tools'), exist_ok=True)
os.makedirs(os.path.join(sdk,'platforms'), exist_ok=True)
with zipfile.ZipFile(os.path.join(sdk,'.tmp','bt.zip')) as z:
    z.extractall(os.path.join(sdk,'build-tools'))
with zipfile.ZipFile(os.path.join(sdk,'.tmp','platform.zip')) as z:
    z.extractall(os.path.join(sdk,'platforms'))
shutil.rmtree(os.path.join(sdk,'.tmp'), ignore_errors=True)
PY
  # build-tools 压缩包内层目录名是代号（如 android-14），统一改成 34
  if [ ! -d "$BT" ]; then
    for d in "$SDK"/build-tools/*/; do mv "$d" "$BT"; break; done
  fi
fi

# ---------- 2. 把网页塞进 assets ----------
echo "[2/5] 同步 index.html -> android/assets/"
mkdir -p android/assets "$BUILD"
cp index.html android/assets/index.html

# ---------- 3. 编译资源 ----------
echo "[3/5] aapt2 编译资源"
rm -rf "$BUILD/classes" "$BUILD/dex" "$BUILD/gen" "$BUILD"/*.apk
"$BT/aapt2.exe" compile --dir android/res -o "$BUILD/res.zip"
"$BT/aapt2.exe" link -o "$BUILD/base.apk" -I "$JAR" \
  --manifest android/AndroidManifest.xml -R "$BUILD/res.zip" -A android/assets \
  --java "$BUILD/gen" --min-sdk-version 23 --target-sdk-version 34 --auto-add-overlay

# ---------- 4. 编译 Java 并生成 dex ----------
echo "[4/5] javac + d8"
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$JAR" -d "$BUILD/classes" \
  "$BUILD/gen/com/local/scale/R.java" android/java/com/local/scale/MainActivity.java 2>&1 \
  | grep -v "警告\|warning\|注:" || true
mkdir -p "$BUILD/dex"
"$BT/d8.bat" --lib "$JAR" --release --min-api 23 --output "$BUILD/dex" \
  $(find "$BUILD/classes" -name "*.class")
python - <<'PY'
import zipfile, shutil
shutil.copyfile('.build/base.apk', '.build/unsigned.apk')
z = zipfile.ZipFile('.build/unsigned.apk', 'a', zipfile.ZIP_DEFLATED)
z.write('.build/dex/classes.dex', 'classes.dex')
z.close()
PY

# ---------- 5. 对齐并签名 ----------
echo "[5/5] zipalign + 签名"
"$BT/zipalign.exe" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"

if [ ! -f "$KS" ]; then
  echo "    没找到签名密钥，生成一个新的（tools/scale.keystore）..."
  KT=$(command -v keytool || echo "$JAVA_HOME/bin/keytool")
  "$KT" -genkeypair -keystore "$KS" -storepass scale2026 -keypass scale2026 \
    -alias scale -keyalg RSA -keysize 2048 -validity 12000 \
    -dname "CN=Scale Tool, O=Personal, C=CN"
fi

mkdir -p dist
"$BT/apksigner.bat" sign --ks "$KS" --ks-pass pass:scale2026 --key-pass pass:scale2026 \
  --ks-key-alias scale --v1-signing-enabled true --v2-signing-enabled true \
  --out dist/scale.apk "$BUILD/aligned.apk"
"$BT/apksigner.bat" verify dist/scale.apk

echo
echo "完成： dist/scale.apk"
ls -lh dist/scale.apk
