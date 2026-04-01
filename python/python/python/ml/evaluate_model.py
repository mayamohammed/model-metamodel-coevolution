import sys 
from pathlib import Path 
sys .path .insert (0 ,str (Path (__file__ ).resolve ().parent .parent ))
import json ,pickle ,warnings 
import pandas as pd 
import numpy as np 
from sklearn .metrics import (accuracy_score ,f1_score ,precision_score ,
recall_score ,confusion_matrix ,classification_report )
from config import (
DATASET_VAL ,DATASET_ALL ,
MODELS_ML_DIR ,REPORTS_DIR ,LABELS 
)

warnings .filterwarnings ("ignore")

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

def load_model ():
    mp =MODELS_ML_DIR /"best_model.pkl"
    lp =MODELS_ML_DIR /"label_encoder.pkl"
    if not mp .exists ():
        raise FileNotFoundError (f"Run train_model.py first: {mp}")
    with open (mp ,"rb")as f :model =pickle .load (f )
    with open (lp ,"rb")as f :le =pickle .load (f )
    print (f"Model loaded : {mp}")
    return model ,le 

def load_report_features ():
    rp =REPORTS_DIR /"train_report.json"
    if rp .exists ():
        with open (rp )as f :data =json .load (f )
        if "feature_cols"in data :
            return data ["feature_cols"]
    return None 

def get_feature_cols (df ,from_report =None ):
    if from_report :
        cols =[c for c in from_report if c in df .columns ]
        if cols :
            return cols 
    cols =[c for c in ALL_POSSIBLE_FEATURES if c in df .columns ]
    if not cols :
        cols =[c for c in df .columns 
        if c !=LABEL_COL and pd .api .types .is_numeric_dtype (df [c ])]
    return cols 

def load_test (le ):
    if DATASET_VAL .exists ():
        df =pd .read_csv (DATASET_VAL )
        print (f"val.csv loaded : {len(df)} rows")
        return df 
    if DATASET_ALL .exists ():
        from sklearn .model_selection import train_test_split 
        df =pd .read_csv (DATASET_ALL )
        _ ,te =train_test_split (df ,test_size =0.10 ,random_state =42 ,
        stratify =df [LABEL_COL ])
        print (f"test split from features_all.csv : {len(te)} rows")
        return te 
    print ("No CSV found - generating synthetic test set")
    np .random .seed (99 )
    feat =[c for c in ALL_POSSIBLE_FEATURES if c !="total_changes"]
    rows =[]
    for label in le .classes_ :
        idx =list (le .classes_ ).index (label )
        for _ in range (20 ):
            row ={c :0 for c in feat }
            if idx <len (feat ):
                row [feat [idx ]]=np .random .randint (1 ,5 )
            row [LABEL_COL ]=label 
            rows .append (row )
    return pd .DataFrame (rows )

def main ():
    print ("="*60 )
    print ("  EVALUATE MODEL")
    print ("="*60 )

    model ,le =load_model ()
    report_feat =load_report_features ()
    df =load_test (le )
    feat_cols =get_feature_cols (df ,report_feat )

    known =set (le .classes_ )
    df =df [df [LABEL_COL ].isin (known )].copy ()
    print (f"Rows after label filter: {len(df)}")

    X =df [feat_cols ].values 
    yt =le .transform (df [LABEL_COL ])
    yp =model .predict (X )

    acc =accuracy_score (yt ,yp )
    f1w =f1_score (yt ,yp ,average ="weighted",zero_division =0 )
    prec =precision_score (yt ,yp ,average ="weighted",zero_division =0 )
    rec =recall_score (yt ,yp ,average ="weighted",zero_division =0 )
    cm =confusion_matrix (yt ,yp )
    cr =classification_report (
    yt ,yp ,target_names =le .classes_ ,output_dict =True ,zero_division =0 )

    print (f"Accuracy      : {acc*100:.2f}%")
    print (f"F1 (weighted) : {f1w*100:.2f}%")
    print (f"Precision     : {prec*100:.2f}%")
    print (f"Recall        : {rec*100:.2f}%")
    print (classification_report (yt ,yp ,target_names =le .classes_ ,zero_division =0 ))

    out ={
    "accuracy_pct":round (acc *100 ,2 ),
    "f1_weighted":round (f1w ,4 ),
    "precision":round (prec ,4 ),
    "recall":round (rec ,4 ),
    "confusion_matrix":cm .tolist (),
    "classification_report":cr ,
    "classes":list (le .classes_ ),
    "feature_cols":feat_cols ,
    }
    rp =REPORTS_DIR /"evaluate_report.json"
    with open (rp ,"w",encoding ="utf-8")as f :
        json .dump (out ,f ,indent =2 ,ensure_ascii =False )
    print (f"Report : {rp}")

    print ("="*60 )
    if acc >=0.75 :
        print (f"  OK  Accuracy = {acc*100:.2f}%")
    else :
        print (f"  WARN Accuracy={acc*100:.2f}% - run augment + optimize")
    print ("="*60 )

if __name__ =="__main__":
    main ()
