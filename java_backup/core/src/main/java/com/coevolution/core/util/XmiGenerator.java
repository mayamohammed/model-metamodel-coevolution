package com.coevolution.core.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class XmiGenerator {

    private static final Random RNG = new Random(42);

    private static final String[] NAMES = {
        "Youssef","Fatima","Mohammed","Khadija",
        "Amine","Nadia","Khalid","Samira",
        "Hassan","Zineb","Omar","Hasnaa",
        "Rachid","Loubna","Tariq","Meriem",
        "Soufiane","Imane","Hamza","Houda"
    };

    private static final String[] COMPANIES = {
        "MarocTech","AtlasCorp","RifGroup",
        "SaharaCo","OmegaSarl","DeltaMaroc",
        "AlphaGroup","BetaSolutions","GammaInc",
        "SigmaServices"
    };

    private static final String[] DEPARTMENTS = {
        "Informatique","Finance","Marketing",
        "Operations","RessourcesHumaines",
        "Commercial","Logistique","Juridique"
    };

    private static final String[] CITIES = {
        "Casablanca","Rabat","Fes","Marrakech",
        "Agadir","Tanger","Meknes","Oujda",
        "Kenitra","Tetouan","Safi","BeniMellal"
    };

    private static final String[] HOSPITALS = {
        "HopitalCasablanca","CliniqueFes",
        "HopitalRabat","CliniqueMarrakech",
        "HopitalAgadir","CliniqueTanger",
        "HopitalMeknes","CliniqueSafi",
        "HopitalOujda","CliniqueBeniMellal"
    };

    private static final String[] DIAGNOSES = {
        "Grippe","Diabete","Hypertension",
        "Asthme","Fracture","Infection",
        "Allergie","Anemie","Migraine","Covid"
    };

    private static final String[] SPECIALTIES = {
        "Cardiologie","Neurologie","Pediatrie",
        "Orthopedie","Dermatologie","Oncologie",
        "Gynecologie","Chirurgie","Radiologie"
    };

    private static final String[] COURSES = {
        "Mathematiques","Physique","Chimie",
        "Biologie","Informatique","Droit",
        "Economie","Histoire","Philosophie","Langue"
    };

    private static final String[] PRODUCTS = {
        "Laptop","Telephone","Tablette","Camera",
        "Casque","Moniteur","Clavier","Souris",
        "Imprimante","Disque_Dur"
    };

    private static final String[] STATUSES = {
        "ACTIF","INACTIF","EN_ATTENTE","FERME"
    };

    public static void main(String[] args) {
        String base = "data/domains";

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║   XMI Generator — Maroc          ║");
        System.out.println("║   250 fichiers x 5 domaines      ║");
        System.out.println("╚═══���══════════════════════════════╝");
        System.out.println();

        long start = System.currentTimeMillis();

        generateDomain("company",    base, 50);
        generateDomain("hospital",   base, 50);
        generateDomain("university", base, 50);
        generateDomain("ecommerce",  base, 50);
        generateDomain("bank",       base, 50);

        long end = System.currentTimeMillis();

        System.out.println();
        System.out.println("──────────────────────────────────");
        verify(base);
        System.out.println("──────────────────────────────────");
        System.out.println("Temps : " + (end - start) + " ms");
        System.out.println("──────────────────────────────────");
    }

    private static void generateDomain(String domain,
                                        String base,
                                        int count) {
        String dir = base + "/" + domain + "/models/v1";
        ensureDir(dir);

        for (int i = 1; i <= count; i++) {
            String path = String.format(
                "%s/%s_%03d.xmi", dir, domain, i
            );
            String xml = buildXml(domain, i);
            write(path, xml);
        }

        System.out.printf(
            "  %-12s → %d fichiers generés%n",
            domain, count
        );
    }

    private static String buildXml(String domain, int i) {
        switch (domain) {
            case "company":    return buildCompany(i);
            case "hospital":   return buildHospital(i);
            case "university": return buildUniversity(i);
            case "ecommerce":  return buildEcommerce(i);
            case "bank":       return buildBank(i);
            default: throw new IllegalArgumentException(
                "Unknown domain: " + domain
            );
        }
    }

    private static String buildCompany(int i) {
        return xmlHeader()
            + "<company:Company\n"
            + attr("xmi:version", "2.0")
            + attr("xmlns:xmi",
                   "http://www.omg.org/XMI")
            + attr("xmlns:company",
                   "http://www.coevolution.com/company/v1")
            + attr("name",
                   pick(COMPANIES) + "_" + i)
            + attr("location", pick(CITIES))
            + ">\n"
            + "  <departments"
            + inlineAttr("name", pick(DEPARTMENTS))
            + ">\n"
            + "    <employees"
            + inlineAttr("name",   pick(NAMES))
            + inlineAttr("salary",
                         (3000 + RNG.nextInt(8000)) + ".0")
            + inlineAttr("age",
                         String.valueOf(22 + RNG.nextInt(38)))
            + inlineAttr("phone",
                         "+2126" + rndPhone())
            + "/>\n"
            + "  </departments>\n"
            + "</company:Company>\n";
    }

    private static String buildHospital(int i) {
        return xmlHeader()
            + "<hospital:Hospital\n"
            + attr("xmi:version", "2.0")
            + attr("xmlns:xmi",
                   "http://www.omg.org/XMI")
            + attr("xmlns:hospital",
                   "http://www.coevolution.com/hospital/v1")
            + attr("name",
                   pick(HOSPITALS) + "_" + i)
            + ">\n"
            + "  <wards"
            + inlineAttr("name", "Service_" + i)
            + ">\n"
            + "    <patients"
            + inlineAttr("name",      pick(NAMES))
            + inlineAttr("diagnosis", pick(DIAGNOSES))
            + inlineAttr("room",
                         String.valueOf(100 + RNG.nextInt(400)))
            + inlineAttr("phone",
                         "+2126" + rndPhone())
            + "/>\n"
            + "    <doctors"
            + inlineAttr("name",
                         "Dr_" + pick(NAMES))
            + inlineAttr("specialty", pick(SPECIALTIES))
            + "/>\n"
            + "  </wards>\n"
            + "</hospital:Hospital>\n";
    }

    private static String buildUniversity(int i) {
        return xmlHeader()
            + "<university:University\n"
            + attr("xmi:version", "2.0")
            + attr("xmlns:xmi",
                   "http://www.omg.org/XMI")
            + attr("xmlns:university",
                   "http://www.coevolution.com/university/v1")
            + attr("name",
                   "Universite_" + pick(CITIES) + "_" + i)
            + ">\n"
            + "  <faculties"
            + inlineAttr("name", "Faculte_" + i)
            + ">\n"
            + "    <students"
            + inlineAttr("name",    pick(NAMES))
            + inlineAttr("grade",
                         (10 + RNG.nextInt(10)) + ".0")
            + inlineAttr("year",
                         String.valueOf(1 + RNG.nextInt(4)))
            + inlineAttr("credits",
                         String.valueOf(10 + RNG.nextInt(50)))
            + "/>\n"
            + "    <courses"
            + inlineAttr("title",   pick(COURSES))
            + inlineAttr("credits",
                         String.valueOf(2 + RNG.nextInt(4)))
            + "/>\n"
            + "  </faculties>\n"
            + "</university:University>\n";
    }

    private static String buildEcommerce(int i) {
        return xmlHeader()
            + "<ecommerce:Shop\n"
            + attr("xmi:version", "2.0")
            + attr("xmlns:xmi",
                   "http://www.omg.org/XMI")
            + attr("xmlns:ecommerce",
                   "http://www.coevolution.com/ecommerce/v1")
            + attr("name",
                   "Boutique_" + pick(CITIES) + "_" + i)
            + ">\n"
            + "  <products"
            + inlineAttr("name",
                         pick(PRODUCTS) + "_" + i)
            + inlineAttr("price",
                         (50 + RNG.nextInt(5000)) + ".99")
            + inlineAttr("weight",
                         (1 + RNG.nextInt(20)) + ".0")
            + inlineAttr("stock",
                         String.valueOf(RNG.nextInt(500)))
            + "/>\n"
            + "</ecommerce:Shop>\n";
    }

    private static String buildBank(int i) {
        return xmlHeader()
            + "<bank:Bank\n"
            + attr("xmi:version", "2.0")
            + attr("xmlns:xmi",
                   "http://www.omg.org/XMI")
            + attr("xmlns:bank",
                   "http://www.coevolution.com/bank/v1")
            + attr("name", "BanqueMaroc_" + i)
            + ">\n"
            + "  <accounts"
            + inlineAttr("owner",
                         pick(NAMES) + "_" + i)
            + inlineAttr("balance",
                         (1000 + RNG.nextInt(100000)) + ".0")
            + inlineAttr("rate",
                         (1 + RNG.nextInt(5)) + ".0")
            + inlineAttr("status", pick(STATUSES))
            + "/>\n"
            + "</bank:Bank>\n";
    }

    private static void verify(String base) {
        System.out.println("Verification :");
        String[] domains = {
            "company","hospital",
            "university","ecommerce","bank"
        };
        int total = 0;
        for (String d : domains) {
            File dir = new File(
                base + "/" + d + "/models/v1"
            );
            File[] files = dir.listFiles(
                (f, n) -> n.endsWith(".xmi")
            );
            int count = files != null ? files.length : 0;
            total += count;
            System.out.printf(
                "  %-12s : %d/50 %s%n",
                d, count,
                count == 50 ? "OK" : "KO"
            );
        }
        System.out.println();
        System.out.println(
            "  TOTAL : " + total + "/250 "
            + (total == 250 ? "OK" : "KO")
        );
    }

    private static String xmlHeader() {
        return "<?xml version=\"1.0\""
             + " encoding=\"UTF-8\"?>\n";
    }

    private static String attr(String key, String value) {
        return "    " + key + "=\"" + value + "\"\n";
    }

    private static String inlineAttr(String key,
                                      String value) {
        return " " + key + "=\"" + value + "\"";
    }

    private static String rndPhone() {
        return String.format(
            "%08d", RNG.nextInt(100000000)
        );
    }

    private static String pick(String[] arr) {
        return arr[RNG.nextInt(arr.length)];
    }

    private static void ensureDir(String path) {
        new File(path).mkdirs();
    }

    private static void write(String path,
                               String content) {
        try (FileWriter fw = new FileWriter(path)) {
            fw.write(content);
        } catch (IOException e) {
            System.err.println(
                "ERROR : " + path
                + " -> " + e.getMessage()
            );
        }
    }
}