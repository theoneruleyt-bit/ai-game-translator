"""Failed builds must retain diagnostics and must never publish installable outputs."""
import json
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from package_ci_download import package


class DiagnosticsTests(unittest.TestCase):
    def test_failed_build_keeps_log_but_excludes_partial_bundle(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            log = root / "ci-logs/gradle.log"
            log.parent.mkdir(parents=True)
            log.write_text("compile failed")
            aab = root / "app/build/outputs/bundle/release/app-release.aab"
            aab.parent.mkdir(parents=True)
            aab.write_bytes(b"partial")
            output = root / "diagnostics.zip"
            package(root, output, "failure")
            with zipfile.ZipFile(output) as archive:
                self.assertEqual(archive.read("ci-logs/gradle.log"), b"compile failed")
                self.assertNotIn(aab.relative_to(root).as_posix(), archive.namelist())

    def test_success_requires_both_build_outputs(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            with self.assertRaises(FileNotFoundError):
                package(root, root / "output.zip", "success")

    def test_missing_archive_is_a_failure_with_diagnostic_report(self):
        self.check_bad_archive(False)

    def test_corrupt_archive_is_a_failure_with_diagnostic_report(self):
        self.check_bad_archive(True)

    def check_bad_archive(self, corrupt):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            aab = root / "missing.aab"
            if corrupt:
                aab.write_bytes(b"not a ZIP")
            report = root / "reports/native.json"
            result = subprocess.run([sys.executable, str(Path(__file__).with_name("check_native_pages.py")),
                                     str(aab), "--report", str(report)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 1)
            self.assertFalse(json.loads(report.read_text())["passed"])
            self.assertIn("alignment was not checked", result.stdout)
            self.assertNotIn("Traceback", result.stderr)
