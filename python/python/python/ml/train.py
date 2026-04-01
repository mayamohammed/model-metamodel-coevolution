import sys 
from pathlib import Path 
sys .path .insert (0 ,str (Path (__file__ ).resolve ().parent .parent ))

import numpy as np 
import joblib 
import json 
import warnings 
from sklearn .ensemble import RandomForestClassifier 
from sklearn .preprocessing import LabelEncoder 
from sklearn .metrics import classification_report ,accuracy_score 
from sklearn .model_selection import train_test_split 
from xgboost import XGBClassifier 
from config import (
DATASET_TRAIN ,DATASET_VAL ,DATASET_ALL ,
MODELS_ML_DIR ,REPORTS_DIR ,read_csv 
)

warnings .filterwarnings ("ignore")

MODELS_ML_DIR .mkdir (parents =True ,exist_ok =True )
REPORTS_DIR .mkdir (parents =True ,exist_ok =True )

FEATURES =[
"nb_classes_v1","nb_classes_v2","delta_classes",
"nb_added_classes","nb_removed_classes",
"nb_attributes_v1","nb_attributes_v2","delta_attributes",
"nb_added_attributes","nb_removed_attributes","nb_type_changes",
"nb_references_v1","nb_references_v2","delta_references",
"nb_added_references","nb_removed_references",
"nb_multiplicity_changes","nb_containment_changes",
"nb_abstract_changes","nb_supertype_changes","nsuri_changed"
]

print ("="*55 )
print ("  ENTRAINEMENT ML")
print ("="*55 )

train_df =read_csv (DATASET_TRAIN )
val_df =read_csv (DATASET_VAL )

if DATASET_ALL .exists ():
    full =read_csv (DATASET_ALL )
    full [FEATURES ]=full [FEATURES ].fillna (0 )
    _ ,test_df =train_test_split (full ,test_size =0.10 ,random_state =42 ,
    stratify =full ["label"])
else :
    _ ,test_df =train_test_split (val_df ,test_size =0.50 ,random_state =42 ,
    stratify =val_df ["label"])

train_df [FEATURES ]=train_df [FEATURES ].fillna (0 )
val_df [FEATURES ]=val_df [FEATURES ].fillna (0 )
test_df [FEATURES ]=test_df [FEATURES ].fillna (0 )

print (f"  Train : {len(train_df)} paires")
print (f"  Val   : {len(val_df)}   paires")
print (f"  Test  : {len(test_df)}  paires")
print ("="*55 )

feat_cols =[c for c in FEATURES if c in train_df .columns ]

X_train =train_df [feat_cols ]
y_train =train_df ["label"]
X_val =val_df [feat_cols ]
y_val =val_df ["label"]

le =LabelEncoder ()
le .fit (y_train )
y_train_enc =le .transform (y_train )
y_val_enc =le .transform (y_val )

known =set (le .classes_ )
test_df =test_df [test_df ["label"].isin (known )].copy ()
X_test =test_df [feat_cols ]
y_test_enc =le .transform (test_df ["label"])

joblib .dump (le ,MODELS_ML_DIR /"label_encoder.pkl")
print (f"Classes ({len(le.classes_)}) : {list(le.classes_)}")

print ("\n"+"="*55 )
print ("  MODELE 1 - Random Forest")
print ("="*55 )

rf =RandomForestClassifier (
n_estimators =200 ,max_depth =None ,
min_samples_split =2 ,min_samples_leaf =1 ,
random_state =42 ,n_jobs =-1 
)
rf .fit (X_train ,y_train_enc )
rf_val_acc =accuracy_score (y_val_enc ,rf .predict (X_val ))
rf_test_acc =accuracy_score (y_test_enc ,rf .predict (X_test ))
print (f"  Val  accuracy : {rf_val_acc*100:.1f}%")
print (f"  Test accuracy : {rf_test_acc*100:.1f}%")
joblib .dump (rf ,MODELS_ML_DIR /"random_forest.pkl")

print ("\n"+"="*55 )
print ("  MODELE 2 - XGBoost")
print ("="*55 )

xgb_model =XGBClassifier (
n_estimators =200 ,max_depth =6 ,learning_rate =0.1 ,
subsample =0.8 ,colsample_bytree =0.8 ,
random_state =42 ,eval_metric ="mlogloss",verbosity =0 
)
xgb_model .fit (X_train ,y_train_enc ,
eval_set =[(X_val ,y_val_enc )],verbose =False )
xgb_val_acc =accuracy_score (y_val_enc ,xgb_model .predict (X_val ))
xgb_test_acc =accuracy_score (y_test_enc ,xgb_model .predict (X_test ))
print (f"  Val  accuracy : {xgb_val_acc*100:.1f}%")
print (f"  Test accuracy : {xgb_test_acc*100:.1f}%")
joblib .dump (xgb_model ,MODELS_ML_DIR /"xgboost.pkl")

best_name ="random_forest"if rf_test_acc >=xgb_test_acc else "xgboost"
best_model =rf if rf_test_acc >=xgb_test_acc else xgb_model 
best_acc =max (rf_test_acc ,xgb_test_acc )
joblib .dump (best_model ,MODELS_ML_DIR /"best_model.pkl")

print ("\n"+"="*55 )
print (f"  MEILLEUR MODELE : {best_name.upper()}")
print (f"  Test accuracy   : {best_acc*100:.1f}%")
print ("="*55 )

y_pred =best_model .predict (X_test )
labels_present =sorted (set (y_test_enc )|set (y_pred ))
report =classification_report (
y_test_enc ,y_pred ,
labels =labels_present ,
target_names =le .classes_ [labels_present ],
output_dict =True 
)
print (classification_report (
y_test_enc ,y_pred ,
labels =labels_present ,
target_names =le .classes_ [labels_present ]
))

fi =sorted (zip (feat_cols ,best_model .feature_importances_ ),
key =lambda x :x [1 ],reverse =True )
print ("  FEATURE IMPORTANCE (Top 10) :")
for i ,(feat ,imp )in enumerate (fi [:10 ]):
    print (f"  {i+1:2}. {feat:<35} {imp:.4f}")

result ={
"best_model":best_name ,
"random_forest_val":round (rf_val_acc ,4 ),
"random_forest_test":round (rf_test_acc ,4 ),
"xgboost_val":round (xgb_val_acc ,4 ),
"xgboost_test":round (xgb_test_acc ,4 ),
"best_test_accuracy":round (best_acc ,4 ),
"nb_classes":len (le .classes_ ),
"nb_train":len (X_train ),
"nb_val":len (X_val ),
"nb_test":len (X_test ),
"feature_importance":{f :round (float (v ),4 )for f ,v in fi },
"classification_report":report 
}

rp =REPORTS_DIR /"train_report.json"
with open (rp ,"w",encoding ="utf-8")as f :
    json .dump (result ,f ,indent =2 )

print ("\n"+"="*55 )
print (f"  Models  : {MODELS_ML_DIR}")
print (f"  Report  : {rp}")
print ("="*55 )
