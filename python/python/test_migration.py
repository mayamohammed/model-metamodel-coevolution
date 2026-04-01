"""
test_migration.py
=================
Tests the full migration pipeline:
  1. Call Flask API /predict
  2. Select ATL file via TransformationGenerator logic
  3. Apply transformation (simulate)
  4. Validate output
  5. Export report

Semaine 7 - Migration ATL
"""
import os ,json ,requests ,shutil 
from datetime import datetime 

BASE_DIR =os .path .dirname (os .path .abspath (__file__ ))
ROOT =os .path .dirname (BASE_DIR )
ATL_DIR =os .path .join (ROOT ,"data","transformations")
TEST_DIR =os .path .join (ROOT ,"data","test_migration")
OUTPUT_DIR =os .path .join (ROOT ,"data","migrated")
REPORT_DIR =os .path .join (ROOT ,"python","ml","reports")
API_URL ="http://localhost:5000"

os .makedirs (OUTPUT_DIR ,exist_ok =True )
os .makedirs (REPORT_DIR ,exist_ok =True )

LABEL_TO_ATL ={
"ECLASS_ADDED":"add_class_migration.atl",
"ECLASS_REMOVED":"remove_class_migration.atl",
"EATTRIBUTE_TYPE_CHANGED":"rename_attribute_migration.atl",
"EATTRIBUTE_REMOVED":"rename_attribute_migration.atl",
"EATTRIBUTE_ADDED":"add_class_migration.atl",
"EREFERENCE_ADDED":"add_class_migration.atl",
"EREFERENCE_REMOVED":"remove_class_migration.atl",
"EREFERENCE_MULTIPLICITY_CHANGED":"rename_attribute_migration.atl",
"EREFERENCE_CONTAINMENT_CHANGED":"rename_attribute_migration.atl",
"ECLASS_ABSTRACT_CHANGED":"rename_attribute_migration.atl",
"ECLASS_SUPERTYPE_ADDED":"add_class_migration.atl",
"MIXED":"mixed_changes_migration.atl",
}

SCENARIOS =[
{"file":"model_eclass_added.ecore",
"features":{"nb_added_classes":1 },
"expected":"ECLASS_ADDED"},
{"file":"model_eclass_removed.ecore",
"features":{"nb_removed_classes":1 },
"expected":"ECLASS_REMOVED"},
{"file":"model_multiplicity.ecore",
"features":{"nb_multiplicity_changes":2 },
"expected":"EREFERENCE_MULTIPLICITY_CHANGED"},
{"file":"model_abstract.ecore",
"features":{"nb_abstract_changes":1 },
"expected":"ECLASS_ABSTRACT_CHANGED"},
{"file":"model_mixed.ecore",
"features":{"nb_added_classes":1 ,"nb_removed_classes":1 ,
"nb_added_attributes":1 },
"expected":"MIXED"},
]

def predict (features ):
    full ={k :0 for k in [
    "nb_added_classes","nb_removed_classes","nb_added_attributes",
    "nb_removed_attributes","nb_type_changes","nb_added_references",
    "nb_removed_references","nb_multiplicity_changes",
    "nb_containment_changes","nb_abstract_changes",
    "nb_supertype_changes","nsuri_changed"]}
    full .update (features )
    r =requests .post (f"{API_URL}/predict",json =full ,timeout =10 )
    return r .json ()

def apply_migration (src_path ,label ,atl_file ):
    with open (src_path ,"r",encoding ="utf-8")as f :
        content =f .read ()
    comment =f"\n  <!-- ATL Migration: {label} | ATL: {atl_file} -->\n"
    if label =="ECLASS_ADDED":
        content =content .replace ("</ecore:EPackage>",
        comment +
        '  <eClassifiers xsi:type="ecore:EClass" '
        'name="MigratedClass_ATL" abstract="false"/>\n'
        "</ecore:EPackage>")
    elif label =="EREFERENCE_MULTIPLICITY_CHANGED":
        import re 
        content =re .sub (r'upperBound="-?\d+"','upperBound="-1"',content )
        content +=comment 
    elif label =="ECLASS_ABSTRACT_CHANGED":
        content =content .replace ('abstract="false"','abstract="true"')
        content +=comment 
    elif label =="MIXED":
        content =content .replace ("</ecore:EPackage>",
        comment +
        '  <eClassifiers xsi:type="ecore:EClass" '
        'name="NewMixedClass_ATL" abstract="false"/>\n'
        "</ecore:EPackage>")
    else :
        content +=comment 
    fname =os .path .basename (src_path ).replace (".ecore","_migrated.ecore")
    out =os .path .join (OUTPUT_DIR ,fname )
    with open (out ,"w",encoding ="utf-8")as f :
        f .write (content )
    return out 

def validate (out_path ,label ):
    with open (out_path ,"r",encoding ="utf-8")as f :
        content =f .read ()
    errors =[];warnings =[]
    if "ecore:EPackage"not in content :
        errors .append ("Missing EPackage")
    if "ATL Migration:"not in content :
        warnings .append ("ATL comment not found")
    if label =="ECLASS_ADDED"and "MigratedClass_ATL"not in content :
        warnings .append ("MigratedClass_ATL not found")
    if label =="MIXED"and "NewMixedClass_ATL"not in content :
        warnings .append ("NewMixedClass_ATL not found")
    return errors ,warnings 

def main ():
    print ("="*65 )
    print ("  TEST MIGRATION PIPELINE - Semaine 7")
    print ("="*65 )

    try :
        r =requests .get (f"{API_URL}/health",timeout =5 )
        print (f"[OK] API health: {r.json()['status']}")
    except Exception as e :
        print (f"[ERR] API not reachable: {e}")
        return 

    results =[]
    passed =0 

    for i ,sc in enumerate (SCENARIOS ):
        print (f"\n>>> Scenario {i+1}: {sc['expected']}")
        src =os .path .join (TEST_DIR ,sc ["file"])

        pred =predict (sc ["features"])
        label =pred .get ("prediction","UNKNOWN")
        confidence =pred .get ("confidence_pct",0 )
        print (f"  Predicted  : {label} ({confidence:.2f}%)")
        print (f"  Expected   : {sc['expected']}")

        atl_file =LABEL_TO_ATL .get (label ,"mixed_changes_migration.atl")
        atl_path =os .path .join (ATL_DIR ,atl_file )
        atl_exists =os .path .exists (atl_path )
        print (f"  ATL file   : {atl_file} ({'OK' if atl_exists else 'MISSING'})")

        out_path =apply_migration (src ,label ,atl_file )
        out_size =os .path .getsize (out_path )
        print (f"  Output     : {os.path.basename(out_path)} ({out_size} bytes)")

        errors ,warnings =validate (out_path ,label )
        valid =len (errors )==0 
        print (f"  Valid      : {'YES' if valid else 'NO'}")
        for w in warnings :print (f"    WARN: {w}")
        for e in errors :print (f"    ERR : {e}")

        status ="PASS"if valid else "FAIL"
        print (f"  Status     : {status}")
        if valid :passed +=1 

        results .append ({
        "scenario":i +1 ,"file":sc ["file"],
        "expected":sc ["expected"],"predicted":label ,
        "confidence_pct":confidence ,"atl_file":atl_file ,
        "atl_exists":atl_exists ,"output":os .path .basename (out_path ),
        "output_size":out_size ,"valid":valid ,
        "errors":errors ,"warnings":warnings ,"status":status 
        })

    report ={
    "semaine":7 ,"date":datetime .now ().strftime ("%Y-%m-%d %H:%M"),
    "total":len (SCENARIOS ),"passed":passed ,
    "failed":len (SCENARIOS )-passed ,
    "success_rate_pct":round (passed /len (SCENARIOS )*100 ,2 ),
    "scenarios":results 
    }
    rp =os .path .join (REPORT_DIR ,"migration_test_report_s7.json")
    with open (rp ,"w",encoding ="utf-8")as f :
        json .dump (report ,f ,indent =2 ,ensure_ascii =False )

    print ("\n"+"="*65 )
    print (f"  RESULTS : {passed}/{len(SCENARIOS)} PASS")
    print (f"  Rate    : {report['success_rate_pct']}%")
    print (f"  Report  : {rp}")
    if passed ==len (SCENARIOS ):
        print ("  STATUS  : ALL TESTS PASSED  🏆")
    else :
        print (f"  STATUS  : {len(SCENARIOS)-passed} FAILED")
    print ("="*65 )

if __name__ =="__main__":
    main ()
