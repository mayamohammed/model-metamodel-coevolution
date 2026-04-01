"""
augment_dataset.py  v2
======================
Augments minority classes - adds discriminative features.
Target: classes faibles >= 80 samples with better separation.
Semaine 5 - IA/ML
"""
import os ,json ,warnings 
import pandas as pd 
import numpy as np 
from sklearn .model_selection import train_test_split 
warnings .filterwarnings ("ignore")

BASE_DIR =os .path .dirname (os .path .abspath (__file__ ))
DATASET_DIR =os .path .join (BASE_DIR ,"..","..","dataset","ml")
REPORTS_DIR =os .path .join (BASE_DIR ,"reports")
os .makedirs (DATASET_DIR ,exist_ok =True )
os .makedirs (REPORTS_DIR ,exist_ok =True )

LABEL_COL ="label"
TARGET_MIN =80 
NOISE_STD =0.3 

ALL_POSSIBLE_FEATURES =[
"nb_added_classes","nb_removed_classes",
"nb_added_attributes","nb_removed_attributes",
"nb_type_changes","nb_added_references",
"nb_removed_references","nb_multiplicity_changes",
"nb_containment_changes","nb_abstract_changes",
"nb_supertype_changes","nsuri_changed","total_changes",
]

LABEL_PATTERNS ={
"ECLASS_REMOVED":{
"nb_removed_classes":(1 ,3 ),
"nb_added_classes":(0 ,0 ),
},
"ECLASS_SUPERTYPE_ADDED":{
"nb_supertype_changes":(1 ,3 ),
"nb_added_classes":(0 ,1 ),
},
"EREFERENCE_CONTAINMENT_CHANGED":{
"nb_containment_changes":(1 ,3 ),
"nb_added_references":(0 ,0 ),
"nb_removed_references":(0 ,0 ),
},
"EREFERENCE_REMOVED":{
"nb_removed_references":(1 ,3 ),
"nb_added_references":(0 ,0 ),
},
"EREFERENCE_MULTIPLICITY_CHANGED":{
"nb_multiplicity_changes":(1 ,3 ),
"nb_added_references":(0 ,0 ),
},
"MIXED":{
"nb_added_classes":(0 ,2 ),
"nb_removed_classes":(0 ,2 ),
"nb_added_attributes":(1 ,3 ),
"nb_removed_attributes":(1 ,3 ),
},
}

def get_feature_cols (df ):
    cols =[c for c in ALL_POSSIBLE_FEATURES if c in df .columns ]
    if not cols :
        cols =[c for c in df .columns 
        if c !=LABEL_COL 
        and pd .api .types .is_numeric_dtype (df [c ])]
    print (f"[INFO] Feature cols ({len(cols)}): {cols}")
    return cols 

def make_pattern_sample (feat_cols ,label ,rng ):
    row ={c :0 for c in feat_cols }
    pattern =LABEL_PATTERNS .get (label ,{})
    for col ,(lo ,hi )in pattern .items ():
        if col in row :
            row [col ]=int (rng .integers (lo ,hi +1 ))

    for col in feat_cols :
        if col not in pattern :
            row [col ]=max (0 ,int (rng .normal (0 ,0.5 )))
    return row 

def augment (df ,feat_cols ):
    rng =np .random .default_rng (42 )
    aug_rows =[]
    for label ,grp in df .groupby (LABEL_COL ):
        needed =TARGET_MIN -len (grp )
        if needed <=0 :
            cnt =len (grp )
            print (f"  {label:45s}: {cnt:3d} rows - OK")
            continue 
        print (f"  {label:45s}: {len(grp):3d} rows -> +{needed} synthetic")
        X =grp [feat_cols ].values .astype (float )

        for i in range (needed ):
            if label in LABEL_PATTERNS and i <needed //2 :

                row =make_pattern_sample (feat_cols ,label ,rng )
            else :

                base =X [rng .integers (0 ,len (X ))]
                synth =np .clip (
                np .round (base +rng .normal (0 ,NOISE_STD ,base .shape )),
                0 ,None 
                ).astype (int )
                row =dict (zip (feat_cols ,synth ))
            row [LABEL_COL ]=label 
            aug_rows .append (row )

    if aug_rows :
        df =pd .concat ([df ,pd .DataFrame (aug_rows )],ignore_index =True )
    return df .sample (frac =1 ,random_state =42 ).reset_index (drop =True )

def main ():
    print ("="*60 )
    print ("  AUGMENT DATASET v2 - Pattern + Noise - Semaine 5")
    print ("="*60 )

    fc =os .path .join (DATASET_DIR ,"features.csv")
    if not os .path .exists (fc ):
        print (f"[ERR] Not found: {fc}")
        return 
    orig =pd .read_csv (fc )
    print (f"[OK]  Loaded features.csv : {len(orig)} rows")

    feat_cols =get_feature_cols (orig )

    print (f"\nBefore ({len(orig)} rows):")
    print (orig [LABEL_COL ].value_counts ().to_string ())
    print ()

    aug =augment (orig ,feat_cols )

    print (f"\nAfter ({len(aug)} rows):")
    print (aug [LABEL_COL ].value_counts ().to_string ())

    aug .to_csv (fc ,index =False )

    tr ,tmp =train_test_split (
    aug ,test_size =0.30 ,random_state =42 ,stratify =aug [LABEL_COL ])
    v ,te =train_test_split (
    tmp ,test_size =0.50 ,random_state =42 ,stratify =tmp [LABEL_COL ])

    tr .to_csv (os .path .join (DATASET_DIR ,"train.csv"),index =False )
    v .to_csv (os .path .join (DATASET_DIR ,"val.csv"),index =False )
    te .to_csv (os .path .join (DATASET_DIR ,"test.csv"),index =False )

    added =len (aug )-len (orig )
    out ={
    "rows_before":len (orig ),"rows_after":len (aug ),
    "rows_added":added ,
    "train":len (tr ),"val":len (v ),"test":len (te ),
    "feature_cols":feat_cols ,
    "target_min":TARGET_MIN ,
    }
    rp =os .path .join (REPORTS_DIR ,"augment_report.json")
    with open (rp ,"w",encoding ="utf-8")as f :
        json .dump (out ,f ,indent =2 ,ensure_ascii =False )

    print (f"\n  {len(orig)} -> {len(aug)} (+{added} synthetic)")
    print (f"  train={len(tr)} | val={len(v)} | test={len(te)}")
    print (f"[OK] Report : {rp}")
    print ("="*60 )

if __name__ =="__main__":
    main ()
