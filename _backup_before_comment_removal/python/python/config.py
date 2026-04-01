import os

BASE_DIR        = os.path.dirname(os.path.abspath(__file__))
DATA_DIR        = os.path.join(BASE_DIR, "../data")
DATASET_DIR     = os.path.join(DATA_DIR, "dataset/processed")
MODELS_ML_DIR   = os.path.join(DATA_DIR, "models-ml")
REPORTS_DIR     = os.path.join(DATA_DIR, "reports")

DATASET_FINAL   = os.path.join(DATASET_DIR, "dataset_final.csv")
CLASSIFIER_PATH = os.path.join(MODELS_ML_DIR, "classifier_v1.pkl")
METADATA_PATH   = os.path.join(MODELS_ML_DIR, "classifier_v1_metadata.json")

FLASK_HOST     = "localhost"
FLASK_PORT     = 5000

ACCURACY_THRESHOLD   = 0.85
CONFIDENCE_THRESHOLD = 0.80
SIMILARITY_THRESHOLD = 0.60

LABELS = [
    "NONE",
    "RENAME_FEATURE",
    "ADD_DEFAULT_VALUE",
    "DELETE_OR_IGNORE_FEATURE",
    "CHANGE_TYPE_CAST",
    "CARDINALITY_FILL_MIN"
]