"""
app.py
======
Flask REST API for metamodel change prediction.
Endpoints:
  POST /predict       -> predict change type
  POST /predict/batch -> predict multiple pairs
  GET  /health        -> health check
  GET  /model/info    -> model metadata

Semaine 6 - API + CLI
"""
from flask import Flask ,request ,jsonify 
from predict import predict_single ,predict_batch 
from model_loader import get_model_info 
import time 
import os 

app =Flask (__name__ )

@app .route ("/health",methods =["GET"])
def health ():
    return jsonify ({
    "status":"ok",
    "service":"metamodel-coevolution-api",
    "version":"1.0.0",
    "timestamp":time .time ()
    }),200 

@app .route ("/model/info",methods =["GET"])
def model_info ():
    info =get_model_info ()
    return jsonify (info ),200 

@app .route ("/predict",methods =["POST"])
def predict ():
    data =request .get_json (silent =True )
    if not data :
        return jsonify ({"error":"JSON body required"}),400 

    result =predict_single (data )

    if "error"in result :
        return jsonify (result ),422 

    return jsonify (result ),200 

@app .route ("/predict/batch",methods =["POST"])
def predict_batch_endpoint ():
    data =request .get_json (silent =True )
    if not data or "items"not in data :
        return jsonify ({"error":"JSON body with 'items' list required"}),400 

    items =data ["items"]
    if not isinstance (items ,list )or len (items )==0 :
        return jsonify ({"error":"'items' must be a non-empty list"}),400 

    if len (items )>500 :
        return jsonify ({"error":"Maximum 500 items per batch"}),400 

    results =predict_batch (items )
    return jsonify ({
    "total":len (items ),
    "results":results 
    }),200 

@app .route ("/labels",methods =["GET"])
def get_labels ():
    info =get_model_info ()
    return jsonify ({
    "labels":info .get ("classes",[]),
    "count":len (info .get ("classes",[]))
    }),200 

if __name__ =="__main__":
    port =int (os .environ .get ("PORT",5000 ))
    debug =os .environ .get ("DEBUG","true").lower ()=="true"
    print (f"[INFO] Starting Flask API on port {port} (debug={debug})")
    app .run (host ="0.0.0.0",port =port ,debug =debug )
