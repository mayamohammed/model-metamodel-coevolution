import requests ,csv ,os 

ROOT =os .path .dirname (os .path .dirname (os .path .abspath (__file__ )))
CSV =os .path .join (ROOT ,"dataset","ml","test.csv")
API ="http://localhost:5000"

FEATURES =[
"nb_added_classes","nb_removed_classes",
"nb_added_attributes","nb_removed_attributes",
"nb_type_changes","nb_added_references",
"nb_removed_references","nb_multiplicity_changes",
"nb_containment_changes","nb_abstract_changes",
"nb_supertype_changes","nsuri_changed"
]

with open (CSV ,encoding ="utf-8")as f :
    rows =list (csv .DictReader (f ))

print (f"Total : {len(rows)} lignes")
print ("Cherche lignes avec valeurs manquantes...")
print ("="*55 )

problemes =[]
for i ,row in enumerate (rows ):
    item ={}
    missing =[]
    for k in FEATURES :
        v =row .get (k ,"")
        if v .strip ()=="":
            missing .append (k )
            item [k ]=0 
        else :
            try :item [k ]=int (float (v ))
            except :item [k ]=0 
    if missing :
        label =row .get ("label","?")
        print (f"  Ligne {i+2}: manquant = {missing}")
        print (f"  Label reel = {label}")
        problemes .append ((i +2 ,missing ,label ,item ))

print ("="*55 )
if not problemes :
    print ("[OK] Aucune colonne manquante !")
else :
    print (f"[WARN] {len(problemes)} lignes avec donnees manquantes")
    print ("\nTest prediction pour ces lignes :")
    for ligne ,miss ,label ,item in problemes :
        r =requests .post (f"{API}/predict",json =item ).json ()
        pred =r .get ("prediction","?")
        conf =r .get ("confidence_pct",0 )
        ok ="✅"if pred ==label else "❌"
        print (f"  {ok} Ligne {ligne}: reel={label:30s} "
        f"predit={pred} ({conf:.1f}%)")
