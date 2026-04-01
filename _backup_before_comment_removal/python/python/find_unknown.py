import requests, csv, os, json

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV  = os.path.join(ROOT, "dataset", "ml", "test.csv")
API  = "http://localhost:5000"

FEATURES = [
    "nb_added_classes","nb_removed_classes",
    "nb_added_attributes","nb_removed_attributes",
    "nb_type_changes","nb_added_references",
    "nb_removed_references","nb_multiplicity_changes",
    "nb_containment_changes","nb_abstract_changes",
    "nb_supertype_changes","nsuri_changed"
]

with open(CSV, encoding="utf-8") as f:
    rows = list(csv.DictReader(f))

print(f"Total : {len(rows)} lignes")
print("="*60)

unknown = []
for i, row in enumerate(rows):
    item  = {k: int(float(row.get(k,0))) for k in FEATURES}
    label = row.get("label","?")

    r    = requests.post(f"{API}/predict", json=item).json()
    pred = r.get("prediction","?")
    conf = r.get("confidence_pct", 0)

    if pred == "?" or pred == "" or pred is None:
        print(f"\n  Ligne {i+2} :")
        print(f"  Label reel  = {label}")
        print(f"  Features    = {item}")
        print(f"  Reponse API = {json.dumps(r, indent=2)}")
        unknown.append(i+2)

print("="*60)
if not unknown:
    print(f"[OK] Aucune prediction inconnue !")
    print(f"[INFO] Les 2 '?' viennent du parsing JSON")
    print(f"       du rapport batch — pas des donnees")
else:
    print(f"[WARN] {len(unknown)} predictions inconnues")
    print(f"       Lignes : {unknown}")