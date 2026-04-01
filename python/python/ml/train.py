import pandas as pd 
import numpy as np 
import joblib 
import json 
import os 
from sklearn .ensemble import RandomForestClassifier 
from sklearn .preprocessing import LabelEncoder 
from sklearn .metrics import classification_report ,accuracy_score ,confusion_matrix 
from xgboost import XGBClassifier 
import warnings 
warnings .filterwarnings ("ignore")

BASE =os .path .dirname (os .path .abspath (__file__ ))
ML_DIR =os .path .join (BASE ,"..","..","dataset","ml")
MDL =os .path .join (BASE ,"models")
RPT =os .path .join (BASE ,"reports")
os .makedirs (MDL ,exist_ok =True )
os .makedirs (RPT ,exist_ok =True )

print ("="*55 )
print ("  SEMAINE 4a â€” ENTRAINEMENT ML")
print ("="*55 )

train_df =pd .read_csv (os .path .join (ML_DIR ,"train.csv"))
val_df =pd .read_csv (os .path .join (ML_DIR ,"val.csv"))
test_df =pd .read_csv (os .path .join (ML_DIR ,"test.csv"))

print (f"  Train : {len(train_df)} paires")
print (f"  Val   : {len(val_df)}   paires")
print (f"  Test  : {len(test_df)}  paires")
print ("="*55 )

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

X_train =train_df [FEATURES ]
y_train =train_df ["label"]
X_val =val_df [FEATURES ]
y_val =val_df ["label"]
X_test =test_df [FEATURES ]
y_test =test_df ["label"]

le =LabelEncoder ()
le .fit (y_train )
y_train_enc =le .transform (y_train )
y_val_enc =le .transform (y_val )
y_test_enc =le .transform (y_test )

joblib .dump (le ,os .path .join (MDL ,"label_encoder.pkl"))
print (f"\n  Classes ({len(le.classes_)}) : {list(le.classes_)}")

print ("\n"+"="*55 )
print ("  MODELE 1 â€” Random Forest")
print ("="*55 )

rf =RandomForestClassifier (
n_estimators =200 ,
max_depth =None ,
min_samples_split =2 ,
min_samples_leaf =1 ,
random_state =42 ,
n_jobs =-1 
)
rf .fit (X_train ,y_train_enc )

rf_val_acc =accuracy_score (y_val_enc ,rf .predict (X_val ))
rf_test_acc =accuracy_score (y_test_enc ,rf .predict (X_test ))

print (f"  Val  accuracy : {rf_val_acc:.4f}  ({rf_val_acc*100:.1f}%)")
print (f"  Test accuracy : {rf_test_acc:.4f}  ({rf_test_acc*100:.1f}%)")

joblib .dump (rf ,os .path .join (MDL ,"random_forest.pkl"))
print ("  Modele sauvegarde : models/random_forest.pkl âœ…")

print ("\n"+"="*55 )
print ("  MODELE 2 â€” XGBoost")
print ("="*55 )

xgb =XGBClassifier (
n_estimators =200 ,
max_depth =6 ,
learning_rate =0.1 ,
subsample =0.8 ,
colsample_bytree =0.8 ,
random_state =42 ,
eval_metric ="mlogloss",
verbosity =0 
)
xgb .fit (
X_train ,y_train_enc ,
eval_set =[(X_val ,y_val_enc )],
verbose =False 
)

xgb_val_acc =accuracy_score (y_val_enc ,xgb .predict (X_val ))
xgb_test_acc =accuracy_score (y_test_enc ,xgb .predict (X_test ))

print (f"  Val  accuracy : {xgb_val_acc:.4f}  ({xgb_val_acc*100:.1f}%)")
print (f"  Test accuracy : {xgb_test_acc:.4f}  ({xgb_test_acc*100:.1f}%)")

joblib .dump (xgb ,os .path .join (MDL ,"xgboost.pkl"))
print ("  Modele sauvegarde : models/xgboost.pkl âœ…")

best_name ="random_forest"if rf_test_acc >=xgb_test_acc else "xgboost"
best_model =rf if rf_test_acc >=xgb_test_acc else xgb 
best_acc =max (rf_test_acc ,xgb_test_acc )

joblib .dump (best_model ,os .path .join (MDL ,"best_model.pkl"))

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
report_str =classification_report (
y_test_enc ,y_pred ,
labels =labels_present ,
target_names =le .classes_ [labels_present ]
)
print ("\n  RAPPORT CLASSIFICATION (Test) :")
print (report_str )

importances =best_model .feature_importances_ 
fi =sorted (zip (FEATURES ,importances ),key =lambda x :x [1 ],reverse =True )

print ("  FEATURE IMPORTANCE (Top 10) :")
for i ,(feat ,imp )in enumerate (fi [:10 ]):
    bar ="â–ˆ"*int (imp *100 )
    print (f"  {i+1:2}. {feat:<35} {imp:.4f}  {bar}")

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
"feature_importance":{f :round (float (i ),4 )for f ,i in fi },
"classification_report":report 
}

with open (os .path .join (RPT ,"train_report.json"),"w")as f :
    json .dump (result ,f ,indent =2 )

print ("\n"+"="*55 )
print ("  FICHIERS GENERES")
print ("="*55 )
print ("  models/random_forest.pkl  âœ…")
print ("  models/xgboost.pkl        âœ…")
print ("  models/best_model.pkl     âœ…")
print ("  models/label_encoder.pkl  âœ…")
print ("  reports/train_report.json âœ…")
print ("="*55 )
print ("\n  4a TERMINE â€” Pret pour 4b API Flask ! ðŸš€")
