import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import json, warnings
import pandas as pd
from config import (
    DATASET_ALL, DATASET_TRAIN, DATASET_VAL,
    REPORTS_DIR, LABELS, read_csv
)

warnings.filterwarnings("ignore")

REPORTS_DIR.mkdir(parents=True, exist_ok=True)

FEATURE_COLS = [
    "nb_classes_v1","nb_classes_v2","delta_classes",
    "nb_added_classes","nb_removed_classes",
    "nb_attributes_v1","nb_attributes_v2","delta_attributes",
    "nb_added_attributes","nb_removed_attributes","nb_type_changes",
    "nb_references_v1","nb_references_v2","delta_references",
    "nb_added_references","nb_removed_references",
    "nb_multiplicity_changes","nb_containment_changes",
    "nb_abstract_changes","nb_supertype_changes","nsuri_changed",
]
LABEL_COL = "label"
MIN_ROWS  = 100
MIN_CLASS = 5

def check(name, df):
    issues = []
    miss = [c for c in FEATURE_COLS + [LABEL_COL] if c not in df.columns]
    if miss:
        issues.append(f"Missing columns: {miss}")

    cols_ok = [c for c in FEATURE_COLS + [LABEL_COL] if c in df.columns]
    nulls   = df[cols_ok].isnull().sum().sum()
    if nulls > 0:
        issues.append(f"Null values in features: {nulls}")

    if len(df) < MIN_ROWS:
        issues.append(f"Only {len(df)} rows (min={MIN_ROWS})")

    if LABEL_COL in df.columns:
        dist = df[LABEL_COL].value_counts()
        for label in LABELS:
            if label not in dist.index:
                issues.append(f"Missing label: {label}")
            elif dist[label] < MIN_CLASS:
                issues.append(f"{label}: {dist[label]} < {MIN_CLASS}")
    return issues

def main():
    print("=" * 60)
    print("  CHECK DATASET - Validation")
    print("=" * 60)

    files = {
        "features_all.csv": DATASET_ALL,
        "train.csv":        DATASET_TRAIN,
        "val.csv":          DATASET_VAL,
    }

    results = {}
    all_ok  = True

    for name, path in files.items():
        if not path.exists():
            print(f"[--] {name:20s}: NOT FOUND")
            continue

        df     = read_csv(path)
        df[FEATURE_COLS] = df[FEATURE_COLS].fillna(0)
        issues = check(name, df)
        status = "OK" if not issues else "ISSUES"
        if issues:
            all_ok = False

        results[name] = {"rows": len(df), "issues": issues, "status": status}
        print(f"\n[{status}] {name} ({len(df)} rows)")

        if issues:
            for i in issues:
                print(f"  WARNING: {i}")
        else:
            print("  All checks passed")

        if LABEL_COL in df.columns:
            print(f"  Distribution:\n{df[LABEL_COL].value_counts().to_string()}")

    rp = REPORTS_DIR / "check_report.json"
    with open(rp, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\nReport : {rp}")

    print("=" * 60)
    if all_ok:
        print("  OK - Dataset ready for training")
    else:
        print("  WARN - Issues found - run augment_dataset.py")
    print("=" * 60)

if __name__ == "__main__":
    main()
