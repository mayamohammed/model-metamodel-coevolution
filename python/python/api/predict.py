"""
predict.py
==========
Core prediction logic.
Called by app.py endpoints.

Semaine 6 - API + CLI
"""
import numpy as np 
from model_loader import load_model ,get_feature_cols ,get_model_info 
from schema import validate_input ,validate_batch ,build_feature_vector 

def predict_single (data :dict )->dict :
    """
    Predict change type for a single metamodel pair.
    Returns prediction result dict.
    """

    cleaned ,error =validate_input (data )
    if error :
        return {"error":error }

    try :
        model ,encoder =load_model ()
        feat_cols =get_feature_cols ()
        X =[build_feature_vector (cleaned ,feat_cols )]
        X_arr =np .array (X ,dtype =float )

        pred_idx =model .predict (X_arr )[0 ]
        pred_label =encoder .inverse_transform ([pred_idx ])[0 ]

        probabilities ={}
        if hasattr (model ,"predict_proba"):
            proba =model .predict_proba (X_arr )[0 ]
            probabilities ={
            label :round (float (p ),4 )
            for label ,p in zip (encoder .classes_ ,proba )
            }

        top3 =sorted (probabilities .items (),
        key =lambda x :x [1 ],reverse =True )[:3 ]

        confidence =probabilities .get (pred_label ,1.0 )

        return {
        "prediction":pred_label ,
        "confidence":round (confidence ,4 ),
        "confidence_pct":round (confidence *100 ,2 ),
        "top3":[{"label":l ,"probability":p }for l ,p in top3 ],
        "probabilities":probabilities ,
        "features_used":feat_cols ,
        "input_summary":{
        k :v for k ,v in cleaned .items ()if v >0 
        },
        }

    except FileNotFoundError as e :
        return {"error":f"Model not loaded: {str(e)}"}
    except Exception as e :
        return {"error":f"Prediction error: {str(e)}"}

def predict_batch (items :list )->list :
    """
    Predict change types for a batch of metamodel pairs.
    Returns list of prediction results.
    """
    valid_items ,validation_errors =validate_batch (items )

    results =[None ]*len (items )

    for ve in validation_errors :
        results [ve ["index"]]={
        "index":ve ["index"],
        "error":ve ["error"]
        }

    if not valid_items :
        return results 

    try :
        model ,encoder =load_model ()
        feat_cols =get_feature_cols ()

        indices =[i for i ,_ in valid_items ]
        cleaned =[c for _ ,c in valid_items ]
        X_arr =np .array (
        [build_feature_vector (c ,feat_cols )for c in cleaned ],
        dtype =float 
        )

        pred_idx =model .predict (X_arr )
        pred_labels =encoder .inverse_transform (pred_idx )

        probas =None 
        if hasattr (model ,"predict_proba"):
            probas =model .predict_proba (X_arr )

        for j ,(orig_idx ,clean )in enumerate (valid_items ):
            label =pred_labels [j ]
            confidence =1.0 
            proba_dict ={}

            if probas is not None :
                proba_dict ={
                lbl :round (float (p ),4 )
                for lbl ,p in zip (encoder .classes_ ,probas [j ])
                }
                confidence =proba_dict .get (label ,1.0 )

            results [orig_idx ]={
            "index":orig_idx ,
            "prediction":label ,
            "confidence":round (confidence ,4 ),
            "confidence_pct":round (confidence *100 ,2 ),
            "input_summary":{k :v for k ,v in clean .items ()if v >0 },
            }

    except Exception as e :
        for orig_idx ,_ in valid_items :
            results [orig_idx ]={
            "index":orig_idx ,
            "error":f"Prediction error: {str(e)}"
            }

    return results
