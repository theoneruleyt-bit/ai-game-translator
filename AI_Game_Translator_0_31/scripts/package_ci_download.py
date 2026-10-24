"""Collect CI diagnostics, with installable output only when checks pass."""
import os
from pathlib import Path
import zipfile


def package(root, output, status):
    files = set()
    for pattern in ("app/build/reports/**/*", "app/build/test-results/**/*"):
        files.update(p for p in root.glob(pattern) if p.is_file())
    if status == "success":
        for relative in ("app/build/outputs/apk/debug/app-debug.apk",
                         "app/build/outputs/bundle/release/app-release.aab"):
            path = root / relative
            if not path.is_file():
                raise FileNotFoundError(relative)
            files.add(path)
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("CHECK_STATUS.txt", f"Checks: {status}\nAPK and unsigned AAB included only when all preceding checks pass.\n")
        for path in sorted(files):
            archive.write(path, path.relative_to(root).as_posix())
    return len(files)


if __name__ == "__main__":
    status = os.environ.get("CHECK_STATUS", "unknown")
    count = package(Path.cwd(), Path("ci-download.zip"), status)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as stream:
            stream.write(f"\n## Android checks\n\nResult: **{status}**. Files collected: {count}.\n\nDownload is delivered through a draft release on the default branch. Pull requests run checks only.\n")
    print(f"Download prepared: checks={status}, files={count}")
