import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import json, warnings
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from config import (
    DATASET_ALL, DATASET_TRAIN, DATASET_VAL,
    REPORTS_DIR, LABELS
)

warnings.filterwarnings("ignore")

DATASET_ALL.parent.mkdir(parents=True, exist_ok=True)
REPORTS_DIR.mkdir(parents=True, exist_ok=True)

LABEL_COL  = "label"
TARGET_MIN = 80
NOISE_STD  = 0.3

ALL_POSSIBLE_FEATURES = [
    "nb_added_classes","nb_removed_classes",
    "nb_added_attributes","nb_removed_attributes",
    "nb_type_changes","nb_added_references",
    "nb_removed_references","nb_multiplicity_changes",
    "nb_containment_changes","nb_abstract_changes",
    "nb_supertype_changes","nsuri_changed","total_changes",
]

LABEL_PATTERNS = {
    "ECLASS_REMOVED": {
        "nb_removed_classes": (1, 3),
        "nb_added_classes":   (0, 0),
    },
    "ECLASS_SUPERTYPE_ADDED": {
        "nb_supertype_changes": (1, 3),
        "nb_added_classes":     (0, 1),
    },
    "EREFERENCE_REMOVED": {
        "nb_removed_references": (1, 3),
        "nb_added_references":   (0, 0),
    },
    "EREFERENCE_MULTIPLICITY_CHANGED": {
        "nb_multiplicity_changes": (1, 3),
        "nb_added_references":     (0, 0),
    },
}

def get_feature_cols(df):
    cols = [c for c in ALL_POSSIBLE_FEATURES if c in df.columns]
    if not cols:
        cols = [c for c in df.columns
                if c != LABEL_COL and pd.api.types.is_numeric_dtype(df[c])]
    print(f"Feature cols ({len(cols)}): {cols}")
    return cols

def make_pattern_sample(feat_cols, label, rng):
    row     = {c: 0 for c in feat_cols}
    pattern = LABEL_PATTERNS.get(label, {})
    for col, (lo, hi) in pattern.items():
        if col in row:
            row[col] = int(rng.integers(lo, hi + 1))
    for col in feat_cols:
        if col not in pattern:
            row[col] = max(0, int(rng.normal(0, 0.5)))
    return row

def augment(df, feat_cols):
    rng      = np.random.default_rng(42)
    aug_rows = []
    for label, grp in df.groupby(LABEL_COL):
        needed = TARGET_MIN - len(grp)
        if needed <= 0:
            print(f"  {label:45s}: {len(grp):3d} rows - OK")
            continue
        print(f"  {label:45s}: {len(grp):3d} rows -> +{needed} synthetic")
        X = grp[feat_cols].values.astype(float)
        for i in range(needed):
            if label in LABEL_PATTERNS and i < needed // 2:
                row = make_pattern_sample(feat_cols, label, rng)
            else:
                base  = X[rng.integers(0, len(X))]
                synth = np.clip(
                    np.round(base + rng.normal(0, NOISE_STD, base.shape)),
                    0, None
                ).astype(int)
                row = dict(zip(feat_cols, synth))
            row[LABEL_COL] = label
            aug_rows.append(row)
    if aug_rows:
        df = pd.concat([df, pd.DataFrame(aug_rows)], ignore_index=True)
    return df.sample(frac=1, random_state=42).reset_index(drop=True)

def write_csv(path, df):
    with open(path, "wb") as f:
        f.write(b"\xef\xbb\xbf")
    with open(path, "a", encoding="utf-8", newline="") as f:
        f.write("sep=,\n")
        df.to_csv(f, index=False)

def main():
    print("=" * 60)
    print("  AUGMENT DATASET - Pattern + Noise")
    print("=" * 60)

    if not DATASET_ALL.exists():
        print(f"Not found: {DATASET_ALL}")
        return

    orig      = pd.read_csv(DATASET_ALL)
    feat_cols = get_feature_cols(orig)

    print(f"Before ({len(orig)} rows):")
    print(orig[LABEL_COL].value_counts().to_string())

    aug = augment(orig, feat_cols)

    print(f"\nAfter ({len(aug)} rows):")
    print(aug[LABEL_COL].value_counts().to_string())

    write_csv(DATASET_ALL, aug)

    tr, tmp = train_test_split(aug, test_size=0.30, random_state=42,
                                stratify=aug[LABEL_COL])
    v, te   = train_test_split(tmp, test_size=0.50, random_state=42,
                                stratify=tmp[LABEL_COL])

    write_csv(DATASET_TRAIN, tr)
    write_csv(DATASET_VAL,   v)

    added = len(aug) - len(orig)
    out = {
        "rows_before":  len(orig),
        "rows_after":   len(aug),
        "rows_added":   added,
        "train":        len(tr),
        "val":          len(v),
        "test":         len(te),
        "feature_cols": feat_cols,
        "target_min":   TARGET_MIN,
    }
    rp = REPORTS_DIR / "augment_report.json"
    with open(rp, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2, ensure_ascii=False)

    print(f"\n{len(orig)} -> {len(aug)} (+{added} synthetic)")
    print(f"train={len(tr)} | val={len(v)} | test={len(te)}")
    print(f"Report : {rp}")
    print("=" * 60)

if __name__ == "__main__":
    main()


