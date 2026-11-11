"""Run an isolated Java source check without Gradle or Minecraft dependencies."""
from pathlib import Path
import os
import subprocess
import tempfile


def run_java_source(class_name: str, source: str) -> None:
    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin" / "java") if java_home else "java"
    with tempfile.TemporaryDirectory(prefix="chao-isolated-") as directory:
        path = Path(directory) / (class_name + ".java")
        path.write_text(source, encoding="utf-8")
        try:
            result = subprocess.run([java, str(path)], check=False)
        except FileNotFoundError as error:
            raise SystemExit("Java 21 is required. Set JAVA_HOME or add java to PATH.") from error
        if result.returncode:
            raise SystemExit(result.returncode)
