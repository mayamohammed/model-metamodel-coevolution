import sys 
from pathlib import Path 
sys .path .insert (0 ,str (Path (__file__ ).resolve ().parent .parent ))
import json ,pickle ,warnings 
import pandas as pd 
import numpy as np 
from sklearn .ensemble import RandomForestClassifier ,GradientBoostingClassifier 
from sklearn .preprocessing import LabelEncoder 
from sklearn .model_selection import GridSearchCV ,StratifiedKFold ,train_test_split 
from sklearn .metrics import accuracy_score ,f1_score ,classification_report 
from config import (
DATASET_TRAIN ,DATASET_VAL ,DATASET_ALL ,
MODELS_ML_DIR ,REPORTS_DIR 
)

try :
    import xgboost as xgb 
    HAS_XGB =True 
except ImportError :
    HAS_XGB =False 

warnings .filterwarnings ("ignore")

MODELS_ML_DIR .mkdir (parents =True ,exist_ok =True )
REPORTS_DIR .mkdir (parents =True ,exist_ok =True )

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
    return cols 

def load_data ():
    if DATASET_TRAIN .exists ()and DATASET_VAL .exists ():
        tr =pd .read_csv (DATASET_TRAIN )
        v =pd .read_csv (DATASET_VAL ,skiprows =1 )
        if DATASET_ALL .exists ():
            full =pd .read_csv (DATASET_ALL )
            _ ,te =train_test_split (full ,test_size =0.10 ,random_state =42 ,
            stratify =full [LABEL_COL ])
        else :
            _ ,te =train_test_split (v ,test_size =0.50 ,random_state =42 ,
            stratify =v [LABEL_COL ])
    else :
        df =pd .read_csv (DATASET_ALL )
        tr ,tmp =train_test_split (df ,test_size =0.30 ,random_state =42 ,
        stratify =df [LABEL_COL ])
        v ,te =train_test_split (tmp ,test_size =0.50 ,random_state =42 ,
        stratify =tmp [LABEL_COL ])
    print (f"Loaded: train={len(tr)} | val={len(v)} | test={len(te)}")
    return tr ,v ,te 

def build_candidates ():
    cands ={}
    cands ["RF_tuned"]=(
    RandomForestClassifier (class_weight ="balanced",random_state =42 ,n_jobs =-1 ),
    {"n_estimators":[200 ,300 ],
    "max_depth":[None ,20 ,30 ],
    "min_samples_leaf":[1 ,2 ]}
    )
    cands ["GBT"]=(
    GradientBoostingClassifier (random_state =42 ),
    {"n_estimators":[100 ,200 ],
    "max_depth":[3 ,5 ],
    "learning_rate":[0.05 ,0.1 ]}
    )
    if HAS_XGB :
        cands ["XGBoost"]=(
        xgb .XGBClassifier (
        eval_metric ="mlogloss",
        random_state =42 ,n_jobs =-1 ,verbosity =0 ),
        {"n_estimators":[200 ,300 ],
        "max_depth":[4 ,6 ],
        "learning_rate":[0.05 ,0.1 ]}
        )
    return cands 

def run_search (tr ,v ,feat_cols ):
    le =LabelEncoder ()
    le .fit (pd .concat ([tr ,v ])[LABEL_COL ])

    X_tr =tr [feat_cols ].values ;y_tr =le .transform (tr [LABEL_COL ])
    X_v =v [feat_cols ].values ;y_v =le .transform (v [LABEL_COL ])
    X_tv =np .vstack ([X_tr ,X_v ])
    y_tv =np .concatenate ([y_tr ,y_v ])

    cv =StratifiedKFold (n_splits =5 ,shuffle =True ,random_state =42 )
    results ={}
    best_acc ,best_name ,best_model =-1 ,None ,None 

    for name ,(clf ,grid )in build_candidates ().items ():
        print (f"\n[{name}] GridSearch ...")
        gs =GridSearchCV (clf ,grid ,cv =cv ,scoring ="accuracy",
        n_jobs =-1 ,refit =True )
        gs .fit (X_tr ,y_tr )
        val_acc =accuracy_score (y_v ,gs .predict (X_v ))
        f1w =f1_score (y_v ,gs .predict (X_v ),average ="weighted",zero_division =0 )
        print (f"  Best params : {gs.best_params_}")
        print (f"  CV  Accuracy: {gs.best_score_*100:.2f}%")
        print (f"  Val Accuracy: {val_acc*100:.2f}%  F1={f1w*100:.2f}%")
        results [name ]={
        "best_params":gs .best_params_ ,
        "cv_accuracy":round (gs .best_score_ ,4 ),
        "val_accuracy":round (val_acc ,4 ),
        "f1_weighted":round (f1w ,4 ),
        }
        if val_acc >best_acc :
            best_acc ,best_name ,best_model =val_acc ,name ,gs .best_estimator_ 

    print (f"\nWINNER : {best_name}  (Val={best_acc*100:.2f}%)")
    best_model .fit (X_tv ,y_tv )
    return best_model ,le ,best_name ,best_acc ,results 

def evaluate_test (model ,le ,te ,feat_cols ):
    known =set (le .classes_ )
    te2 =te [te [LABEL_COL ].isin (known )].copy ()
    X_te =te2 [feat_cols ].values 
    y_te =le .transform (te2 [LABEL_COL ])
    y_pr =model .predict (X_te )
    acc =accuracy_score (y_te ,y_pr )
    f1w =f1_score (y_te ,y_pr ,average ="weighted",zero_division =0 )
    cr_d =classification_report (
    y_te ,y_pr ,target_names =le .classes_ ,output_dict =True ,zero_division =0 )
    print (f"Test Accuracy : {acc*100:.2f}%")
    print (f"Test F1 (w)   : {f1w*100:.2f}%")
    print (classification_report (y_te ,y_pr ,target_names =le .classes_ ,zero_division =0 ))
    return acc ,f1w ,cr_d 

def main ():
    print ("="*60 )
    print ("  OPTIMIZE MODEL - GridSearch")
    print ("="*60 )

    tr ,v ,te =load_data ()
    feat_cols =get_feature_cols (tr )
    print (f"Features ({len(feat_cols)}): {feat_cols}")

    model ,le ,winner ,val_acc ,results =run_search (tr ,v ,feat_cols )
    test_acc ,test_f1 ,cr_d =evaluate_test (model ,le ,te ,feat_cols )

    for name in ["best_model.pkl","random_forest.pkl"]:
        with open (MODELS_ML_DIR /name ,"wb")as f :
            pickle .dump (model ,f )
    with open (MODELS_ML_DIR /"label_encoder.pkl","wb")as f :
        pickle .dump (le ,f )
    print (f"Models saved : {MODELS_ML_DIR}")

    out ={
    "winner":winner ,
    "val_accuracy":round (val_acc ,4 ),
    "test_accuracy":round (test_acc ,4 ),
    "test_accuracy_pct":round (test_acc *100 ,2 ),
    "test_f1_weighted":round (test_f1 ,4 ),
    "feature_cols":feat_cols ,
    "candidates":results ,
    "classification_report":cr_d ,
    }
    rp =REPORTS_DIR /"optimize_report.json"
    with open (rp ,"w",encoding ="utf-8")as f :
        json .dump (out ,f ,indent =2 ,ensure_ascii =False )
    print (f"Report : {rp}")

    print ("="*60 )
    if test_acc >=0.85 :
        print (f"  TARGET MET  Test Accuracy = {test_acc*100:.2f}%  >= 85%")
    elif test_acc >=0.75 :
        print (f"  GOOD  Test Accuracy = {test_acc*100:.2f}%")
    else :
        print (f"  WARN  Test Accuracy = {test_acc*100:.2f}%  < 75%")
    print ("="*60 )

if __name__ =="__main__":
    main ()
