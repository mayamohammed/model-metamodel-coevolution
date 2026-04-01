"""
schema.py
=========
Input validation for the prediction API.
Validates feature values and returns clean feature dict.

Semaine 6 - API + CLI
"""

ALL_FEATURES =[
"nb_added_classes","nb_removed_classes",
"nb_added_attributes","nb_removed_attributes",
"nb_type_changes","nb_added_references",
"nb_removed_references","nb_multiplicity_changes",
"nb_containment_changes","nb_abstract_changes",
"nb_supertype_changes","nsuri_changed","total_changes",
]

REQUIRED_MIN =[
"nb_added_classes","nb_removed_classes",
"nb_added_attributes","nb_removed_attributes",
]

def validate_input (data :dict )->tuple :
    """
    Validates and cleans input data.
    Returns (cleaned_dict, error_message).
    error_message is None if valid.
    """
    if not isinstance (data ,dict ):
        return None ,"Input must be a JSON object"

    cleaned ={}
    errors =[]

    for feature in ALL_FEATURES :
        val =data .get (feature ,0 )

        if not isinstance (val ,(int ,float )):
            try :
                val =float (val )
            except (TypeError ,ValueError ):
                errors .append (f"'{feature}' must be numeric, got: {val}")
                continue 

        if val <0 :
            errors .append (f"'{feature}' must be >= 0, got: {val}")
            continue 
        cleaned [feature ]=int (val )

    if errors :
        return None ,"; ".join (errors )

    total =sum (cleaned .get (f ,0 )for f in ALL_FEATURES if f !="total_changes")
    if total ==0 and cleaned .get ("total_changes",0 )==0 :
        return None ,"At least one feature must be non-zero"

    if "total_changes"not in data or data .get ("total_changes",0 )==0 :
        cleaned ["total_changes"]=total 

    return cleaned ,None 

def build_feature_vector (cleaned :dict ,feature_cols :list )->list :
    """Build feature vector in the correct column order."""
    return [cleaned .get (col ,0 )for col in feature_cols ]

def validate_batch (items :list )->tuple :
    """Validate a batch of inputs. Returns (valid_items, errors)."""
    valid =[]
    errors =[]
    for i ,item in enumerate (items ):
        cleaned ,err =validate_input (item )
        if err :
            errors .append ({"index":i ,"error":err })
        else :
            valid .append ((i ,cleaned ))
    return valid ,errors
