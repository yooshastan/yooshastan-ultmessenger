#!/usr/bin/env python3
"""Guarded archive-row integration tests; no real archive actions or account data."""
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

SCRIPT = Path(__file__).resolve().parents[2] / "scripts/integrate-archive-visibility.py"
spec = importlib.util.spec_from_file_location("ult_archive_integration", SCRIPT)
integration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(integration)

class ArchiveVisibilityIntegrationTest(TestCase):
    def fixture(self, root: Path):
        activity = root / integration.ACTIVITY
        adapter = root / integration.ADAPTER
        activity.parent.mkdir(parents=True, exist_ok=True)
        adapter.parent.mkdir(parents=True, exist_ok=True)
        activity.write_text("".join((
            integration.ACTIVITY_IMPORT,
            integration.MENU_ANCHOR,
            integration.ACTIVITY_CLASS_ANCHOR,
            integration.ARCHIVE_GUARD,
            "    }\n",
            integration.PULL_GUARD,
        )))
        adapter.write_text("".join((
            integration.ADAPTER_IMPORT,
            integration.ADAPTER_CLASS,
            integration.ARRAY_ANCHOR,
            integration.MOVE_ANCHOR,
            integration.EMPTY_ANCHOR,
        )))
        return activity, adapter

    def test_filters_local_row_and_keeps_archive_reachable(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            activity, adapter = self.fixture(root)
            integration.integrate(root)
            a, b = activity.read_text(), adapter.read_text()
            self.assertIn("ULT: Hide Archive row", a)
            self.assertIn("ULT: Open Archive", a)
            self.assertIn('args.putInt("folderId", 1)', a)
            self.assertIn("!isUltArchiveRowHidden()", a)
            self.assertIn("visibleDialogs = new ArrayList<>(array.size())", b)
            self.assertIn("dialog.id != archiveId", b)
            self.assertIn("dialogs.indexOf(source)", b)
            self.assertNotIn("addDialogToFolder", a + b)
            self.assertNotIn("deleteDialog", a + b)
            self.assertTrue((root / integration.DESTINATION).is_file())
            with self.assertRaises(ValueError):
                integration.integrate(root)

    def test_drift_rejected_without_partial_write(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            activity, adapter = self.fixture(root)
            adapter.write_text(adapter.read_text().replace(integration.MOVE_ANCHOR, "upstream changed"))
            before_a, before_b = activity.read_text(), adapter.read_text()
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertEqual(activity.read_text(), before_a)
            self.assertEqual(adapter.read_text(), before_b)
            self.assertFalse((root / integration.DESTINATION).exists())

    def test_duplicate_anchor_rejected(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            activity, adapter = self.fixture(root)
            activity.write_text(activity.read_text() + integration.MENU_ANCHOR)
            with self.assertRaises(ValueError):
                integration.integrate(root)
            self.assertNotIn("ULT: Hide Archive row", activity.read_text())

if __name__ == "__main__":
    main()
