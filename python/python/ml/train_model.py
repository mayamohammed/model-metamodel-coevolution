"""
train_model.py
==============
Trains a RandomForest classifier on the metamodel change dataset.
Adapts automatically to available columns.
Semaine 5 - IA/ML
"""
import os ,json ,pickle ,warnings 
import pandas as pd 
import numpy as np 
from sklearn .ensemble import RandomForestClassifier 
from sklearn .preprocessing import LabelEncoder 
from sklearn .model_selection import train_test_split 
from sklearn .metrics import classification_report ,accuracy_score 
warnings .filterwarnings ("ignore")

BASE_DIR =os .path .dirname (os .path .abspath (__file__ ))
DATASET_DIR =os .path .join (BASE_DIR ,"..","..","dataset","ml")
MODELS_DIR =os .path .join (BASE_DIR ,"models")
REPORTS_DIR =os .path .join (BASE_DIR ,"reports")
os .makedirs (MODELS_DIR ,exist_ok =True )
os .makedirs (REPORTS_DIR ,exist_ok =True )

LABEL_COL ="label"
ALL_POSSIBLE_FEATURES =[
"nb_added_classes","nb_removed_classes",
"nb_added_attributes","nb_removed_attributes",
"nb_type_changes","nb_added_references",
"nb_removed_references","nb_multiplicity_changes",
"nb_containment_changes","nb_abstract_changes",
"nb_supertype_changes","nsuri_changed","total_changes",
]

def get_feature_cols (df ):
    cols =[c for c in ALL_POSSIBLE_FEATURES if c in df .columns ]
    if not cols :
        cols =[c for c in df .columns 
        if c !=LABEL_COL and pd .api .types .is_numeric_dtype (df [c ])]
    print (f"[INFO] Features ({len(cols)}): {cols}")
    return cols 

def generate_synthetic ():
    np .random .seed (42 )
    feat =[c for c in ALL_POSSIBLE_FEATURES if c !="total_changes"]
    labels =[
    "ECLASS_ADDED","ECLASS_REMOVED","EATTRIBUTE_ADDED",
    "EATTRIBUTE_REMOVED","EATTRIBUTE_TYPE_CHANGED",
    "EREFERENCE_ADDED","EREFERENCE_REMOVED",
    "EREFERENCE_MULTIPLICITY_CHANGED","EREFERENCE_CONTAINMENT_CHANGED",
    "ECLASS_ABSTRACT_CHANGED","ECLASS_SUPERTYPE_ADDED","MIXED"
    ]
    rows =[]
    for i ,label in enumerate (labels ):
        for _ in range (80 ):
            row ={c :0 for c in feat }
            if i <len (feat ):
                row [feat [i ]]=np .random .randint (1 ,5 )
            row [LABEL_COL ]=label 
            rows .append (row )
    df =pd .DataFrame (rows ).sample (frac =1 ,random_state =42 ).reset_index (drop =True )
    tr ,tmp =train_test_split (df ,test_size =0.30 ,random_state =42 ,stratify =df [LABEL_COL ])
    v ,te =train_test_split (tmp ,test_size =0.50 ,random_state =42 ,stratify =tmp [LABEL_COL ])
    return tr ,v ,te 

def load_data ():
    print ("[INFO] Loading dataset ...")
    tc =os .path .join (DATASET_DIR ,"train.csv")
    fc =os .path .join (DATASET_DIR ,"features.csv")
    if os .path .exists (tc ):
        tr =pd .read_csv (tc )
        v =pd .read_csv (os .path .join (DATASET_DIR ,"val.csv"))
        te =pd .read_csv (os .path .join (DATASET_DIR ,"test.csv"))
    elif os .path .exists (fc ):
        df =pd .read_csv (fc )
        tr ,tmp =train_test_split (df ,test_size =0.30 ,random_state =42 ,stratify =df [LABEL_COL ])
        v ,te =train_test_split (tmp ,test_size =0.50 ,random_state =42 ,stratify =tmp [LABEL_COL ])
    else :
        print ("[WARN] No CSV found - using synthetic data")
        tr ,v ,te =generate_synthetic ()
    print (f"  train={len(tr)} | val={len(v)} | test={len(te)}")
    return tr ,v ,te 

def train (tr ,v ,feat_cols ):
    le =LabelEncoder ()
    le .fit (tr [LABEL_COL ])
    X_tr =tr [feat_cols ].values 
    y_tr =le .transform (tr [LABEL_COL ])
    X_v =v [feat_cols ].values 
    y_v =le .transform (v [LABEL_COL ])

    model =RandomForestClassifier (
    n_estimators =200 ,max_features ="sqrt",
    class_weight ="balanced",random_state =42 ,n_jobs =-1 )
    model .fit (X_tr ,y_tr )

    y_pred =model .predict (X_v )
    val_acc =accuracy_score (y_v ,y_pred )
    report =classification_report (
    y_v ,y_pred ,target_names =le .classes_ ,output_dict =True ,zero_division =0 )

    print (f"\n  Validation Accuracy : {val_acc*100:.2f}%")
    print (classification_report (y_v ,y_pred ,target_names =le .classes_ ,zero_division =0 ))
    return model ,le ,val_acc ,report 

def save_model (model ,le ,val_acc ,report ,feat_cols ):
    for name in ["random_forest.pkl","best_model.pkl"]:
        with open (os .path .join (MODELS_DIR ,name ),"wb")as f :
            pickle .dump (model ,f )
    with open (os .path .join (MODELS_DIR ,"label_encoder.pkl"),"wb")as f :
        pickle .dump (le ,f )

    importances ={}
    if hasattr (model ,"feature_importances_"):
        importances =dict (zip (feat_cols ,
        [round (float (x ),6 )for x in model .feature_importances_ ]))

    out ={
    "model":"RandomForestClassifier",
    "n_estimators":200 ,
    "val_accuracy":round (val_acc ,4 ),
    "val_accuracy_pct":round (val_acc *100 ,2 ),
    "classes":list (le .classes_ ),
    "feature_cols":feat_cols ,
    "classification_report":report ,
    "feature_importances":importances ,
    }
    rp =os .path .join (REPORTS_DIR ,"train_report.json")
    with open (rp ,"w",encoding ="utf-8")as f :
        json .dump (out ,f ,indent =2 ,ensure_ascii =False )

    print (f"\n[OK] Models : {MODELS_DIR}")
    print (f"[OK] Report : {rp}")

def main ():
    print ("="*60 )
    print ("  TRAIN MODEL - RandomForest - Semaine 5")
    print ("="*60 )
    tr ,v ,te =load_data ()
    feat_cols =get_feature_cols (tr )
    model ,le ,val_acc ,report =train (tr ,v ,feat_cols )
    save_model (model ,le ,val_acc ,report ,feat_cols )
    print ("\n"+"="*60 )
    if val_acc >=0.75 :
        print (f"  OK  Validation Accuracy = {val_acc*100:.2f}%")
    else :
        print (f"  WARN Accuracy={val_acc*100:.2f}% - run augment then optimize")
    print ("="*60 )

if __name__ =="__main__":
    main ()
