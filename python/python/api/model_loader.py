"""
model_loader.py
===============
Loads and caches the ML model + label encoder.
Reads feature list from train_report.json.

Semaine 6 - API + CLI
"""
import os 
import json 
import pickle 

BASE_DIR =os .path .dirname (os .path .abspath (__file__ ))
ML_DIR =os .path .join (BASE_DIR ,"..","ml")
MODELS_DIR =os .path .join (ML_DIR ,"models")
REPORTS_DIR =os .path .join (ML_DIR ,"reports")

_model =None 
_encoder =None 
_info =None 

ALL_POSSIBLE_FEATURES =[
"nb_added_classes","nb_removed_classes",
"nb_added_attributes","nb_removed_attributes",
"nb_type_changes","nb_added_references",
"nb_removed_references","nb_multiplicity_changes",
"nb_containment_changes","nb_abstract_changes",
"nb_supertype_changes","nsuri_changed","total_changes",
]

def _load_feature_cols ():
    """Load feature columns from train_report.json."""
    rp =os .path .join (REPORTS_DIR ,"train_report.json")
    if os .path .exists (rp ):
        with open (rp ,encoding ="utf-8")as f :
            data =json .load (f )
        if "feature_cols"in data :
            return data ["feature_cols"]
    return [c for c in ALL_POSSIBLE_FEATURES if c !="total_changes"]

def load_model ():
    """Load model + encoder (singleton pattern)."""
    global _model ,_encoder 
    if _model is not None :
        return _model ,_encoder 

    mp =os .path .join (MODELS_DIR ,"best_model.pkl")
    lp =os .path .join (MODELS_DIR ,"label_encoder.pkl")

    if not os .path .exists (mp ):
        raise FileNotFoundError (f"Model not found: {mp}")
    if not os .path .exists (lp ):
        raise FileNotFoundError (f"Encoder not found: {lp}")

    with open (mp ,"rb")as f :_model =pickle .load (f )
    with open (lp ,"rb")as f :_encoder =pickle .load (f )

    print (f"[OK] Model loaded   : {mp}")
    print (f"[OK] Encoder loaded : {lp}")
    return _model ,_encoder 

def get_model_info ():
    """Return model metadata."""
    global _info 
    if _info is not None :
        return _info 

    model ,encoder =load_model ()
    feat_cols =_load_feature_cols ()

    report ={}
    for name in ["optimize_report.json","train_report.json"]:
        rp =os .path .join (REPORTS_DIR ,name )
        if os .path .exists (rp ):
            with open (rp ,encoding ="utf-8")as f :
                report =json .load (f )
            break 

    _info ={
    "model_type":type (model ).__name__ ,
    "classes":list (encoder .classes_ ),
    "n_classes":len (encoder .classes_ ),
    "feature_cols":feat_cols ,
    "n_features":len (feat_cols ),
    "accuracy_pct":report .get ("test_accuracy_pct",
    report .get ("val_accuracy_pct","N/A")),
    "winner":report .get ("winner",type (model ).__name__ ),
    "version":"1.0.0",
    }
    return _info 

def get_feature_cols ():
    return _load_feature_cols ()
