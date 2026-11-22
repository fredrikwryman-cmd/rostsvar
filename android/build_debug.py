"""Bygger en lokalt signerad test-APK med Android SDK och JDK pa Windows.

Resultatet heter Tap-and-Talk-android-test.apk och ligger i mappen build.
Paket-ID och den lokala testnyckeln ar oforandrade, sa en ny APK
uppgraderar en tidigare installation i stallet for att krocka med den.
"""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent
sdk = Path(os.environ["ANDROID_HOME"])
tools = sdk / "build-tools" / "36.0.0"
android = sdk / "platforms/android-35/android.jar"
source = root / "app/src/main"
namespace = "se.rostsvar.mobile"

def run(*args):
    subprocess.run([str(arg) for arg in args], check=True)

with tempfile.TemporaryDirectory() as tmp:
    out = Path(tmp)
    classes = out / "classes"
    classes.mkdir()
    manifest = ET.parse(source / "AndroidManifest.xml")
    manifest.getroot().set("package", namespace)
    ns = "{http://schemas.android.com/apk/res/android}"
    ET.register_namespace("android", ns[1:-1])
    manifest.getroot().set(ns + "versionCode", "2")
    manifest.getroot().set(ns + "versionName", "0.2")
    manifest.write(out / "AndroidManifest.xml", encoding="utf-8")
    run(tools / "aapt2.exe", "compile", "--dir", source / "res", "-o", out / "res.zip")
    run(tools / "aapt2.exe", "link", "-I", android, "--manifest", out / "AndroidManifest.xml",
        "--min-sdk-version", "30", "--target-sdk-version", "35", "-o", out / "raw.apk", out / "res.zip")
    run("javac", "-encoding", "UTF-8", "--release", "8", "-classpath", android, "-d", classes,
        *sorted((source / "java").rglob("*.java")))
    with zipfile.ZipFile(out / "classes.jar", "w") as jar:
        for item in classes.rglob("*.class"):
            jar.write(item, item.relative_to(classes))
    run(tools / "d8.bat", "--min-api", "30", "--lib", android, "--output", out, out / "classes.jar")
    with zipfile.ZipFile(out / "raw.apk", "a") as apk:
        apk.write(out / "classes.dex", "classes.dex")
    run(tools / "zipalign.exe", "-f", "4", out / "raw.apk", out / "aligned.apk")
    build = root / "build"
    build.mkdir(exist_ok=True)
    key = build / "local-debug.p12"
    if not key.exists():
        run("keytool", "-genkeypair", "-alias", "debug", "-keystore", key, "-storepass", "android",
            "-keypass", "android", "-keyalg", "RSA", "-keysize", "2048", "-validity", "3650",
            "-dname", "CN=Rostsvar Local Debug")
    dest = build / "Tap-and-Talk-android-test.apk"
    run(tools / "apksigner.bat", "sign", "--ks", key, "--ks-key-alias", "debug", "--ks-pass",
        "pass:android", "--out", dest, out / "aligned.apk")
    run(tools / "apksigner.bat", "verify", "--verbose", dest)
    print(dest)
