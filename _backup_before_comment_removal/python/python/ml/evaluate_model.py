"""
evaluate_model.py
=================
Full evaluation of the best saved model.
Semaine 5 - IA/ML
"""
import os, json, pickle, warnings
import pandas as pd
import numpy as np
from sklearn.metrics import (accuracy_score, f1_score, precision_score,
                             recall_score, confusion_matrix, classification_report)
warnings.filterwarnings("ignore")

BASE_DIR    = os.path.dirname(os.path.abspath(__file__))
DATASET_DIR = os.path.join(BASE_DIR, "..", "..", "dataset", "ml")
MODELS_DIR  = os.path.join(BASE_DIR, "models")
REPORTS_DIR = os.path.join(BASE_DIR, "reports")
os.makedirs(REPORTS_DIR, exist_ok=True)

LABEL_COL = "label"
ALL_POSSIBLE_FEATURES = [
    "nb_added_classes","nb_removed_classes",
    "nb_added_attributes","nb_removed_attributes",
    "nb_type_changes","nb_added_references",
    "nb_removed_references","nb_multiplicity_changes",
    "nb_containment_changes","nb_abstract_changes",
    "nb_supertype_changes","nsuri_changed","total_changes",
]

def load_model():
    mp = os.path.join(MODELS_DIR, "best_model.pkl")
    lp = os.path.join(MODELS_DIR, "label_encoder.pkl")
    if not os.path.exists(mp):
        raise FileNotFoundError(f"Run train_model.py first: {mp}")
    with open(mp, "rb") as f: model = pickle.load(f)
    with open(lp, "rb") as f: le    = pickle.load(f)
    print(f"[OK] Model loaded  : {mp}")
    return model, le

def load_report_features():
    rp = os.path.join(REPORTS_DIR, "train_report.json")
    if os.path.exists(rp):
        with open(rp) as f: data = json.load(f)
        if "feature_cols" in data:
            return data["feature_cols"]
    return None

def get_feature_cols(df, from_report=None):
    if from_report:
        cols = [c for c in from_report if c in df.columns]
        if cols:
            print(f"[INFO] Features from report ({len(cols)}): {cols}")
            return cols
    cols = [c for c in ALL_POSSIBLE_FEATURES if c in df.columns]
    if not cols:
        cols = [c for c in df.columns
                if c != LABEL_COL and pd.api.types.is_numeric_dtype(df[c])]
    print(f"[INFO] Features auto-detected ({len(cols)}): {cols}")
    return cols

def load_test(le):
    tc = os.path.join(DATASET_DIR, "test.csv")
    if os.path.exists(tc):
        df = pd.read_csv(tc)
        print(f"[OK] test.csv loaded : {len(df)} rows")
        return df
    print("[WARN] test.csv not found - generating synthetic test set")
    np.random.seed(99)
    feat = [c for c in ALL_POSSIBLE_FEATURES if c != "total_changes"]
    rows = []
    for label in le.classes_:
        idx = list(le.classes_).index(label)
        for _ in range(20):
            row = {c: 0 for c in feat}
            if idx < len(feat):
                row[feat[idx]] = np.random.randint(1, 5)
            row[LABEL_COL] = label
            rows.append(row)
    return pd.DataFrame(rows)

def main():
    print("=" * 60)
    print("  EVALUATE MODEL - Semaine 5")
    print("=" * 60)

    model, le         = load_model()
    report_feat       = load_report_features()
    df                = load_test(le)
    feat_cols         = get_feature_cols(df, report_feat)

    # keep only labels present in encoder
    known = set(le.classes_)
    df    = df[df[LABEL_COL].isin(known)].copy()
    print(f"[INFO] Rows after label filter: {len(df)}")

    X  = df[feat_cols].values
    yt = le.transform(df[LABEL_COL])
    yp = model.predict(X)

    acc  = accuracy_score(yt, yp)
    f1w  = f1_score(yt, yp, average="weighted", zero_division=0)
    prec = precision_score(yt, yp, average="weighted", zero_division=0)
    rec  = recall_score(yt, yp, average="weighted", zero_division=0)
    cm   = confusion_matrix(yt, yp)
    cr   = classification_report(
        yt, yp, target_names=le.classes_, output_dict=True, zero_division=0)

    print(f"\n  Accuracy      : {acc*100:.2f}%")
    print(f"  F1 (weighted) : {f1w*100:.2f}%")
    print(f"  Precision     : {prec*100:.2f}%")
    print(f"  Recall        : {rec*100:.2f}%")
    print("\n" + classification_report(
        yt, yp, target_names=le.classes_, zero_division=0))

    out = {
        "accuracy_pct":  round(acc  * 100, 2),
        "f1_weighted":   round(f1w,  4),
        "precision":     round(prec, 4),
        "recall":        round(rec,  4),
        "confusion_matrix":       cm.tolist(),
        "classification_report":  cr,
        "classes":        list(le.classes_),
        "feature_cols":   feat_cols,
    }
    rp = os.path.join(REPORTS_DIR, "evaluate_report.json")
    with open(rp, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2, ensure_ascii=False)
    print(f"[OK] Report : {rp}")

    print("\n" + "=" * 60)
    if acc >= 0.75:
        print(f"  OK  Accuracy = {acc*100:.2f}%")
    else:
        print(f"  WARN Accuracy={acc*100:.2f}% - run augment + optimize")
    print("=" * 60)

if __name__ == "__main__":
    main()