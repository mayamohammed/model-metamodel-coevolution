"""
check_dataset.py
================
Validates the ML dataset integrity.
Semaine 5 - IA/ML
"""
import os ,json ,warnings 
import pandas as pd 
import numpy as np 
warnings .filterwarnings ("ignore")

BASE_DIR =os .path .dirname (os .path .abspath (__file__ ))
DATASET_DIR =os .path .join (BASE_DIR ,"..","..","dataset","ml")
REPORTS_DIR =os .path .join (BASE_DIR ,"reports")
os .makedirs (REPORTS_DIR ,exist_ok =True )

FEATURE_COLS =[
"nb_added_classes","nb_removed_classes",
"nb_added_attributes","nb_removed_attributes",
"nb_type_changes","nb_added_references",
"nb_removed_references","nb_multiplicity_changes",
"nb_containment_changes","nb_abstract_changes",
"nb_supertype_changes","nsuri_changed","total_changes",
]
LABEL_COL ="label"
MIN_ROWS =100 
MIN_CLASS =10 
EXPECTED =["ECLASS_ADDED","ECLASS_REMOVED","EATTRIBUTE_ADDED",
"EATTRIBUTE_REMOVED","EATTRIBUTE_TYPE_CHANGED",
"EREFERENCE_ADDED","EREFERENCE_REMOVED",
"EREFERENCE_MULTIPLICITY_CHANGED","EREFERENCE_CONTAINMENT_CHANGED",
"ECLASS_ABSTRACT_CHANGED","ECLASS_SUPERTYPE_ADDED","MIXED"]

def check (name ,df ):
    issues =[]
    req =FEATURE_COLS +[LABEL_COL ]
    miss =[c for c in req if c not in df .columns ]
    if miss :issues .append (f"Missing columns: {miss}")
    nulls =df .isnull ().sum ().sum ()
    if nulls >0 :issues .append (f"Null values: {nulls}")
    if len (df )<MIN_ROWS :issues .append (f"Only {len(df)} rows (min={MIN_ROWS})")
    if LABEL_COL in df .columns :
        dist =df [LABEL_COL ].value_counts ()
        for l in EXPECTED :
            if l not in dist .index :issues .append (f"Missing label: {l}")
            elif dist [l ]<MIN_CLASS :issues .append (f"{l}: {dist[l]} < {MIN_CLASS}")
    return issues 

def main ():
    print ("="*60 )
    print ("  CHECK DATASET - Validation - Semaine 5")
    print ("="*60 )
    results ={};all_ok =True 
    for name in ["features.csv","train.csv","val.csv","test.csv"]:
        path =os .path .join (DATASET_DIR ,name )
        if not os .path .exists (path ):
            print (f"[--] {name:15s}: NOT FOUND");continue 
        df =pd .read_csv (path )
        issues =check (name ,df )
        status ="OK"if not issues else "ISSUES"
        if issues :all_ok =False 
        results [name ]={"rows":len (df ),"issues":issues ,"status":status }
        print (f"\n[{status}] {name} ({len(df)} rows)")
        if issues :
            for i in issues :print (f"  WARNING: {i}")
        else :
            print (f"  All checks passed")
        if LABEL_COL in df .columns :
            print (f"  Distribution:\n{df[LABEL_COL].value_counts().to_string()}")
    with open (os .path .join (REPORTS_DIR ,"check_report.json"),"w",encoding ="utf-8")as f :
        json .dump (results ,f ,indent =2 ,ensure_ascii =False )
    print ("\n"+"="*60 )
    if all_ok :print ("  OK - Dataset ready for training")
    else :print ("  WARN - Issues found - run augment_dataset.py")
    print ("="*60 )

if __name__ =="__main__":main ()
