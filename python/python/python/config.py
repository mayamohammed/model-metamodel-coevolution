from pathlib import Path 
import pandas as pd 

BASE_DIR =Path (__file__ ).resolve ().parent 
PROJECT_ROOT =BASE_DIR .parent 

DATA_DIR =PROJECT_ROOT /"data"
DATASET_DIR =DATA_DIR /"ml_final"
MODELS_ML_DIR =DATA_DIR /"models-ml"
REPORTS_DIR =DATA_DIR /"reports"

DATASET_ALL =DATASET_DIR /"features_all.csv"
DATASET_TRAIN =DATASET_DIR /"train.csv"
DATASET_VAL =DATASET_DIR /"val.csv"

CLASSIFIER_PATH =MODELS_ML_DIR /"classifier_v1.pkl"
METADATA_PATH =MODELS_ML_DIR /"classifier_v1_metadata.json"

FLASK_HOST ="localhost"
FLASK_PORT =5000 

ACCURACY_THRESHOLD =0.85 
CONFIDENCE_THRESHOLD =0.80 
SIMILARITY_THRESHOLD =0.60 

LABELS =[
"ECLASS_ADDED",
"ECLASS_REMOVED",
"EATTRIBUTE_ADDED",
"EATTRIBUTE_REMOVED",
"EATTRIBUTE_TYPE_CHANGED",
"EREFERENCE_ADDED",
"EREFERENCE_REMOVED",
"EREFERENCE_MULTIPLICITY_CHANGED",
"ECLASS_ABSTRACT_CHANGED",
"ECLASS_SUPERTYPE_ADDED"
]

def read_csv (path ):
    with open (path ,encoding ="utf-8-sig")as f :
        first_line =f .readline ().strip ()
        second_line =f .readline ().strip ()

    if first_line =="sep=,":

        return pd .read_csv (path ,skiprows =1 ,encoding ="utf-8-sig")
    else :

        return pd .read_csv (path ,skiprows =0 ,encoding ="utf-8-sig")

def write_csv (path ,df ):
    path =Path (path )
    path .parent .mkdir (parents =True ,exist_ok =True )
    with open (path ,"wb")as f :
        f .write (b"\xef\xbb\xbf")
    with open (path ,"a",encoding ="utf-8",newline ="")as f :
        f .write ("sep=,\n")
        df .to_csv (f ,index =False )
