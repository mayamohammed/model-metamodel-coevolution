# -*- coding: utf-8 -*-
import os
import sys
import json
import joblib
import numpy as np
from flask import Flask, request, jsonify

# ── Chemins ───────────────────────────────────────────────────────
BASE   = os.path.dirname(os.path.abspath(__file__))
MDL    = os.path.join(BASE, "..", "ml", "models")
RPT    = os.path.join(BASE, "..", "ml", "reports")

# ── Chargement modele + encoder ───────────────────────────────────
print("[API] Chargement du modele...")
model   = joblib.load(os.path.join(MDL, "best_model.pkl"))
encoder = joblib.load(os.path.join(MDL, "label_encoder.pkl"))

# Charger le rapport
report_path = os.path.join(RPT, "train_report.json")
with open(report_path, "r") as f:
    report = json.load(f)

model_name = report["best_model"]; print(f"[API] Modele charge : {model_name}")
acc = report["best_test_accuracy"]; print(f"[API] Accuracy      : {acc*100:.1f}%")
nb_cls = len(encoder.classes_); print(f"[API] Classes       : {nb_cls}")

# ── Features attendues (21) ───────────────────────────────────────
FEATURES = [
    "nb_classes_v1", "nb_classes_v2", "delta_classes",
    "nb_added_classes", "nb_removed_classes",
    "nb_attributes_v1", "nb_attributes_v2", "delta_attributes",
    "nb_added_attributes", "nb_removed_attributes", "nb_type_changes",
    "nb_references_v1", "nb_references_v2", "delta_references",
    "nb_added_references", "nb_removed_references",
    "nb_multiplicity_changes", "nb_containment_changes",
    "nb_abstract_changes", "nb_supertype_changes", "nsuri_changed"
]

# ── Flask app ─────────────────────────────────────────────────────
app = Flask(__name__)

# ─────────────────────────────────────────────────────────────────
# GET /health
# ─────────────────────────────────────────────────────────────────
@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status"  : "OK",
        "model"   : report["best_model"],
        "accuracy": report["best_test_accuracy"],
        "classes" : len(encoder.classes_)
    }), 200

# ─────────────────────────────────────────────────────────────────
# GET /model/info
# ─────────────────────────────────────────────────────────────────
@app.route("/model/info", methods=["GET"])
def model_info():
    return jsonify({
        "best_model"          : report["best_model"],
        "random_forest_test"  : report["random_forest_test"],
        "xgboost_test"        : report["xgboost_test"],
        "best_test_accuracy"  : report["best_test_accuracy"],
        "nb_train"            : report["nb_train"],
        "nb_val"              : report["nb_val"],
        "nb_test"             : report["nb_test"],
        "nb_classes"          : report["nb_classes"],
        "classes"             : list(encoder.classes_),
        "features"            : FEATURES,
        "top_features"        : list(report["feature_importance"].items())[:5]
    }), 200

# ─────────────────────────────────────────────────────────────────
# POST /predict
# Body JSON :
#   { "features": [f1, f2, ..., f21] }
#   ou
#   { "nb_classes_v1": 5, "nb_classes_v2": 6, ... }
# ─────────────────────────────────────────────────────────────────
@app.route("/predict", methods=["POST"])
def predict():
    try:
        data = request.get_json(force=True)
        if data is None:
            return jsonify({"error": "Body JSON manquant"}), 400

        # ── Extraire le vecteur de features ───────────────────────
        if "features" in data:
            # Format tableau direct
            values = data["features"]
            if len(values) != len(FEATURES):
                return jsonify({
                    "error": f"21 features attendues, {len(values)} recues",
                    "expected": FEATURES
                }), 400
        else:
            # Format dictionnaire nommé
            missing = [f for f in FEATURES if f not in data]
            if missing:
                return jsonify({
                    "error"  : "Features manquantes",
                    "missing": missing
                }), 400
            values = [float(data[f]) for f in FEATURES]

        # ── Prediction ────────────────────────────────────────────
        X = np.array(values).reshape(1, -1)
        pred_enc   = model.predict(X)[0]
        pred_proba = model.predict_proba(X)[0]

        # Label prédit
        pred_label = encoder.inverse_transform([pred_enc])[0]
        confidence = float(pred_proba[pred_enc])

        # Top 3 predictions
        top3_idx = np.argsort(pred_proba)[::-1][:3]
        top3 = [
            {
                "label"      : encoder.inverse_transform([i])[0],
                "confidence" : round(float(pred_proba[i]), 4)
            }
            for i in top3_idx
        ]

        return jsonify({
            "prediction" : pred_label,
            "confidence" : round(confidence, 4),
            "top3"       : top3,
            "features_received": dict(zip(FEATURES, values))
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# ─────────────────────────────────────────────────────────────────
# POST /predict/batch
# Body JSON : { "pairs": [ {features...}, {features...} ] }
# ─────────────────────────────────────────────────────────────────
@app.route("/predict/batch", methods=["POST"])
def predict_batch():
    try:
        data = request.get_json(force=True)
        if "pairs" not in data:
            return jsonify({"error": "Champ pairs manquant"}), 400

        results = []
        for i, pair in enumerate(data["pairs"]):
            if "features" in pair:
                values = pair["features"]
            else:
                values = [float(pair.get(f, 0)) for f in FEATURES]

            X = np.array(values).reshape(1, -1)
            pred_enc   = model.predict(X)[0]
            pred_proba = model.predict_proba(X)[0]
            pred_label = encoder.inverse_transform([pred_enc])[0]
            confidence = float(pred_proba[pred_enc])

            results.append({
                "index"      : i,
                "prediction" : pred_label,
                "confidence" : round(confidence, 4)
            })

        return jsonify({
            "total"  : len(results),
            "results": results
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# ─────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    print(f"\n[API] Demarrage sur http://localhost:{port}")
    print(f"[API] Endpoints disponibles :")
    print(f"[API]   GET  /health")
    print(f"[API]   GET  /model/info")
    print(f"[API]   POST /predict")
    print(f"[API]   POST /predict/batch")
    print(f"[API] Pret ! 🚀\n")
    app.run(host="0.0.0.0", port=port, debug=False)
