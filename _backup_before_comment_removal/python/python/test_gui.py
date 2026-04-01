"""
test_gui.py
===========
Teste tous les endpoints utilisés par la GUI.
Semaine 8 - Test GUI
"""
import requests, json, os

API  = "http://localhost:5000"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def sep(title):
    print(f"\n{'='*55}")
    print(f"  {title}")
    print('='*55)

def check(condition, msg):
    status = "PASS ✅" if condition else "FAIL ❌"
    print(f"  [{status}] {msg}")
    return condition

passed = 0
total  = 0

# ── TEST 1 : Health ───────────────────────────────────────────
sep("TEST 1 : Health Check")
r = requests.get(f"{API}/health").json()
total += 1
if check(r.get("status") == "ok", f"status=ok  version={r.get('version')}"):
    passed += 1

# ── TEST 2 : Model Info ───────────────────────────────────────
sep("TEST 2 : Model Info (Dataset Tab)")
r = requests.get(f"{API}/model/info").json()
total += 3
if check(r.get("model_type") == "XGBClassifier",
         f"model_type={r.get('model_type')}"):           passed += 1
if check(float(r.get("accuracy_pct", 0)) > 90,
         f"accuracy={r.get('accuracy_pct')}%"):          passed += 1
if check(r.get("n_classes") == 12,
         f"n_classes={r.get('n_classes')}"):             passed += 1

# ── TEST 3 : Prediction Tab ───────────────────────────────────
sep("TEST 3 : Prediction Tab (5 scenarios)")
SCENARIOS = [
    ({"nb_added_classes":1},           "ECLASS_ADDED"),
    ({"nb_removed_classes":1},         "ECLASS_REMOVED"),
    ({"nb_multiplicity_changes":2},    "EREFERENCE_MULTIPLICITY_CHANGED"),
    ({"nb_abstract_changes":1},        "ECLASS_ABSTRACT_CHANGED"),
    ({"nb_type_changes":3},            "EATTRIBUTE_TYPE_CHANGED"),
]
BASE = {k:0 for k in [
    "nb_added_classes","nb_removed_classes","nb_added_attributes",
    "nb_removed_attributes","nb_type_changes","nb_added_references",
    "nb_removed_references","nb_multiplicity_changes",
    "nb_containment_changes","nb_abstract_changes",
    "nb_supertype_changes","nsuri_changed"]}

for feat, expected in SCENARIOS:
    payload = {**BASE, **feat}
    r = requests.post(f"{API}/predict", json=payload).json()
    pred = r.get("prediction","?")
    conf = r.get("confidence_pct", 0)
    total += 1
    if check(pred == expected,
             f"{expected:42s} -> {pred} ({conf:.1f}%)"):
        passed += 1

# ── TEST 4 : Migration Tab ────────────────────────────────────
sep("TEST 4 : Migration Tab (ATL files)")
ATL_DIR = os.path.join(ROOT, "data", "transformations")
ATL_FILES = [
    "add_class_migration.atl",
    "remove_class_migration.atl",
    "rename_attribute_migration.atl",
    "mixed_changes_migration.atl",
]
for f in ATL_FILES:
    path = os.path.join(ATL_DIR, f)
    total += 1
    if check(os.path.exists(path), f"ATL exists: {f}"):
        passed += 1

# ── TEST 5 : Ecore models ─────────────────────────────────────
sep("TEST 5 : Migration models (.ecore)")
MODEL_DIR = os.path.join(ROOT, "data", "test_migration")
MODELS = [
    "model_eclass_added.ecore",
    "model_eclass_removed.ecore",
    "model_multiplicity.ecore",
    "model_abstract.ecore",
    "model_mixed.ecore",
]
for f in MODELS:
    path = os.path.join(MODEL_DIR, f)
    total += 1
    if check(os.path.exists(path), f"Model exists: {f}"):
        passed += 1

# ── TEST 6 : Labels ───────────────────────────────────────────
sep("TEST 6 : Labels endpoint")
r = requests.get(f"{API}/labels").json()
total += 1
if check(r.get("count") == 12,
         f"labels count={r.get('count')}"):
    passed += 1

# ── TEST 7 : Batch ────────────────────────────────────────────
sep("TEST 7 : Batch Prediction (Report Tab)")
items = [{**BASE, "nb_added_classes":1},
         {**BASE, "nb_removed_classes":1},
         {**BASE, "nb_multiplicity_changes":2}]
r = requests.post(f"{API}/predict/batch", json={"items":items}).json()
total += 1
if check(r.get("total") == 3,
         f"batch total={r.get('total')}"):
    passed += 1
for i, res in enumerate(r.get("results",[])):
    print(f"    Item {i}: {res.get('prediction'):40s} "
          f"({res.get('confidence_pct'):.1f}%)")

# ── RESULTAT FINAL ────────────────────────────────────────────
print(f"\n{'='*55}")
print(f"  RESULTS : {passed}/{total} PASS")
print(f"  Rate    : {passed/total*100:.1f}%")
if passed == total:
    print("  STATUS  : ALL GUI TESTS PASSED  🏆")
else:
    print(f"  STATUS  : {total-passed} FAILED ❌")
print('='*55)