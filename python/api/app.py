import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import json
import joblib
import numpy as np
from flask import Flask, request, jsonify
from config import MODELS_ML_DIR, REPORTS_DIR, FLASK_HOST, FLASK_PORT

print("[API] Chargement du modele...")
model   = joblib.load(MODELS_ML_DIR / "best_model.pkl")
encoder = joblib.load(MODELS_ML_DIR / "label_encoder.pkl")

with open(REPORTS_DIR / "train_report.json", "r") as f:
    report = json.load(f)

print(f"[API] Modele  : {report['best_model']}")
print(f"[API] Accuracy: {report['best_test_accuracy']*100:.1f}%")
print(f"[API] Classes : {len(encoder.classes_)}")

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

app = Flask(__name__)

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status"  : "OK",
        "model"   : report["best_model"],
        "accuracy": report["best_test_accuracy"],
        "classes" : len(encoder.classes_)
    }), 200

@app.route("/model/info", methods=["GET"])
def model_info():
    return jsonify({
        "best_model"         : report["best_model"],
        "random_forest_test" : report.get("random_forest_test"),
        "xgboost_test"       : report.get("xgboost_test"),
        "best_test_accuracy" : report["best_test_accuracy"],
        "nb_train"           : report["nb_train"],
        "nb_val"             : report["nb_val"],
        "nb_test"            : report["nb_test"],
        "nb_classes"         : report["nb_classes"],
        "classes"            : list(encoder.classes_),
        "features"           : FEATURES,
        "top_features"       : list(report["feature_importance"].items())[:5]
    }), 200

@app.route("/predict", methods=["POST"])
def predict():
    try:
        data = request.get_json(force=True)
        if data is None:
            return jsonify({"error": "Body JSON manquant"}), 400

        if "features" in data:
            values = data["features"]
            if len(values) != len(FEATURES):
                return jsonify({
                    "error"   : f"{len(FEATURES)} features attendues, {len(values)} recues",
                    "expected": FEATURES
                }), 400
        else:
            missing = [f for f in FEATURES if f not in data]
            if missing:
                return jsonify({"error": "Features manquantes", "missing": missing}), 400
            values = [float(data[f]) for f in FEATURES]

        X          = np.array(values).reshape(1, -1)
        pred_enc   = model.predict(X)[0]
        pred_proba = model.predict_proba(X)[0]
        pred_label = encoder.inverse_transform([pred_enc])[0]
        confidence = float(pred_proba[pred_enc])

        top3_idx = np.argsort(pred_proba)[::-1][:3]
        top3 = [
            {
                "label"     : encoder.inverse_transform([i])[0],
                "confidence": round(float(pred_proba[i]), 4)
            }
            for i in top3_idx
        ]

        return jsonify({
            "prediction"       : pred_label,
            "confidence"       : round(confidence, 4),
            "top3"             : top3,
            "features_received": dict(zip(FEATURES, values))
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/predict/batch", methods=["POST"])
def predict_batch():
    try:
        data = request.get_json(force=True)
        if "pairs" not in data:
            return jsonify({"error": "Champ pairs manquant"}), 400

        results = []
        for i, pair in enumerate(data["pairs"]):
            values     = pair["features"] if "features" in pair \
                         else [float(pair.get(f, 0)) for f in FEATURES]
            X          = np.array(values).reshape(1, -1)
            pred_enc   = model.predict(X)[0]
            pred_proba = model.predict_proba(X)[0]
            pred_label = encoder.inverse_transform([pred_enc])[0]
            confidence = float(pred_proba[pred_enc])
            results.append({
                "index"     : i,
                "prediction": pred_label,
                "confidence": round(confidence, 4)
            })

        return jsonify({"total": len(results), "results": results}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    print(f"\n[API] Demarrage sur http://{FLASK_HOST}:{FLASK_PORT}")
    print(f"[API]   GET  /health")
    print(f"[API]   GET  /model/info")
    print(f"[API]   POST /predict")
    print(f"[API]   POST /predict/batch")
    app.run(host=FLASK_HOST, port=FLASK_PORT, debug=False)

