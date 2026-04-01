"""
generate_gui_report.py
Appelé par la GUI Java pour générer le rapport.
"""
import requests ,json ,csv ,os ,sys ,argparse 
from datetime import datetime 

parser =argparse .ArgumentParser ()
parser .add_argument ("--input",default =None )
parser .add_argument ("--output",default =None )
args =parser .parse_args ()

ROOT =os .path .dirname (os .path .dirname (os .path .abspath (__file__ )))
API ="http://localhost:5000"

CSV_IN =args .input if args .input else os .path .join (ROOT ,"dataset","ml","test.csv")
OUT_DIR =args .output if args .output else os .path .join (ROOT ,"python","ml","reports")

os .makedirs (OUT_DIR ,exist_ok =True )

FEATURES =[
"nb_added_classes","nb_removed_classes",
"nb_added_attributes","nb_removed_attributes",
"nb_type_changes","nb_added_references",
"nb_removed_references","nb_multiplicity_changes",
"nb_containment_changes","nb_abstract_changes",
"nb_supertype_changes","nsuri_changed"
]

print ("="*58 )
print ("  GUI REPORT GENERATOR")
print ("="*58 )

rows ,labels =[],[]
with open (CSV_IN ,encoding ="utf-8")as f :
    reader =csv .DictReader (f )
    for row in reader :
        item ={k :int (float (row .get (k ,0 )))
        for k in FEATURES }
        rows .append (item )
        labels .append (row .get ("label","?"))

print (f"[INFO] CSV           : {CSV_IN}")
print (f"[INFO] Lignes        : {len(rows)}")

r =requests .post (f"{API}/predict/batch",
json ={"items":rows },
timeout =60 ).json ()
results =r .get ("results",[])
print (f"[INFO] Prédictions   : {len(results)}")

dist_pred ={}
for res in results :
    lbl =res .get ("prediction","?")
    dist_pred [lbl ]=dist_pred .get (lbl ,0 )+1 

dist_real ={}
for l in labels :
    dist_real [l ]=dist_real .get (l ,0 )+1 

correct =sum (
1 for i ,res in enumerate (results )
if i <len (labels )and res .get ("prediction")==labels [i ])
acc =correct /len (results )*100 if results else 0 

print (f"\n{'─'*58}")
print (f"  Accuracy : {correct}/{len(results)} = {acc:.1f}%")
print (f"{'─'*58}")
print (f"  {'Label':42s} {'Prédit':>6}  {'Réel':>5}")
print (f"{'─'*58}")

all_labels =sorted (set (list (dist_pred )+list (dist_real )))
for lbl in all_labels :
    p =dist_pred .get (lbl ,0 )
    r2 =dist_real .get (lbl ,0 )
    ok ="✅"if p ==r2 else "⚠️ "
    print (f"  {ok} {lbl:40s} {p:6d}  {r2:5d}")

print (f"{'─'*58}")

ts =datetime .now ().strftime ("%Y%m%d_%H%M%S")
outfile =os .path .join (OUT_DIR ,f"report_gui_{ts}.json")

report ={
"date":datetime .now ().strftime ("%Y-%m-%d %H:%M:%S"),
"source":CSV_IN ,
"total_rows":len (rows ),
"total_predicted":len (results ),
"accuracy_pct":round (acc ,2 ),
"correct":correct ,
"distribution_predicted":dist_pred ,
"distribution_real":dist_real 
}

with open (outfile ,"w",encoding ="utf-8")as f :
    json .dump (report ,f ,indent =2 ,ensure_ascii =False )

print (f"\n[OK] Rapport sauvegarde :")
print (f"     {outfile}")
print (f"\n{'='*58}")
print (f"  RAPPORT GENERE ✅")
print (f"{'='*58}")
