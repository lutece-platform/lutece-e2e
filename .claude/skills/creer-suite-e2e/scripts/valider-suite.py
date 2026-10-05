#!/usr/bin/env python3
"""Valide un fichier .e2e-suite.yml sans rien executer.

Reproduit les controles de LecteurDeSuite.java, pour qu'un site puisse verifier sa suite sans
disposer du depot lutece-e2e ni d'un navigateur. Le lecteur Java reste l'autorite : ce script dit
ce qu'il refusera, pas l'inverse. Un test du depot de tests compare les deux vocabulaires et
echoue s'ils divergent.

Usage :
    python3 valider-suite.py .e2e-suite.yml
    python3 valider-suite.py .e2e-suite.yml --strict   # les avertissements deviennent bloquants

Sortie : 0 si le fichier est executable, 1 sinon.
"""

import sys
import unicodedata
from pathlib import Path

# --- Vocabulaire. Doit rester aligne sur Vocabulaire.java (un test du depot le verifie). -------

TYPES_DE_QUESTION = [
    "texte", "texte-long", "nombre", "date", "radio", "bouton-radio", "case-a-cocher",
    "liste-deroulante", "liste-ordonnee", "fichier", "image", "camera", "commentaire",
    "conditions-utilisation", "numerotation", "telephone", "geolocalisation", "creneau",
    "session", "attribut-mylutece",
]

TYPES_DE_TACHE = [
    "statut-publication", "date-modification", "commentaire", "confirmation",
    "notification-usager", "alerte-usager", "piece-jointe", "demande-complement",
    "correction-saisie", "edition-reponse", "edition-reponse-fo", "edition-reponse-auto",
    "duplication-reponse", "valorisation-auto", "affectation-entite", "affectation-entite-auto",
    "desaffectation-entite", "notification-entite", "assignation-utilisateur", "archivage",
    "choix", "changement-etat-auto", "choix-etat-suivant", "export-pdf",
]

NATURES_DE_SAISIE = ["texte", "nombre", "date"]
FORMATS_D_EXPORT = ["csv", "pdf"]

# Limites connues : le vocabulaire les accepte, les briques sous-jacentes ne les honorent pas
# encore. Les signaler a l'ecriture evite de les decouvrir au bout d'un quart d'heure de conteneur.
LIMITES = {
    "iterations": "les iterations de groupe en front office ne sont pas honorees par la brique",
    "brouillon": "la sauvegarde en brouillon suppose vraisemblablement un usager authentifie",
}


def normaliser(mot):
    """Forme comparable d'un mot : sans casse, sans accent, separateurs ramenes au tiret."""
    sans_accent = "".join(
        c for c in unicodedata.normalize("NFD", str(mot).strip().lower())
        if unicodedata.category(c) != "Mn"
    )
    return sans_accent.replace("_", "-").replace(" ", "-")


class Rapport:
    """Accumule erreurs et avertissements, et les restitue dans l'ordre du fichier."""

    def __init__(self):
        self.erreurs = []
        self.avertissements = []

    def erreur(self, ou, probleme, attendus=None):
        msg = f"{ou} : {probleme}"
        if attendus:
            msg += "\n    valeurs acceptees : " + ", ".join(sorted(attendus))
        self.erreurs.append(msg)

    def avertir(self, ou, propos):
        self.avertissements.append(f"{ou} : {propos}")


def charger(chemin, rapport):
    """Lit le YAML, en traduisant les erreurs de syntaxe en conseil utile."""
    try:
        import yaml
    except ImportError:
        print("ERREUR : le module python3-yaml est requis (pip install pyyaml)", file=sys.stderr)
        sys.exit(2)

    try:
        with open(chemin, encoding="utf-8") as flux:
            return yaml.safe_load(flux)
    except yaml.YAMLError as faute:
        rapport.erreur("fichier", f"YAML invalide : {faute}")
        rapport.avertir(
            "fichier",
            "un libelle contenant « ? » ou « , » ne passe pas dans le style { cle: valeur } : "
            "l'ecrire en style bloc, ou le guillemeter",
        )
        return None


def valider(contenu, rapport):
    """Applique les controles de LecteurDeSuite sur une description deja chargee."""
    if not isinstance(contenu, dict):
        rapport.erreur("racine", "le fichier devrait decrire un objet")
        return

    if not isinstance(contenu.get("suite"), dict) or not contenu["suite"].get("nom"):
        rapport.erreur("suite", "« nom » est obligatoire")

    organisation = contenu.get("organisation")
    workflow = contenu.get("workflow")
    formulaire = contenu.get("formulaire")
    parcours = contenu.get("parcours") or []

    if not any([organisation, workflow, formulaire, parcours]):
        rapport.erreur(
            "racine",
            "la suite ne decrit ni organisation, ni workflow, ni formulaire, ni parcours : "
            "elle n'a rien a executer",
        )
        return

    etats = valider_workflow(workflow, rapport)
    questions, choix_par_question, etapes = valider_formulaire(formulaire, rapport)
    valider_parcours(parcours, rapport, etats, questions, choix_par_question, etapes,
                     workflow, formulaire)


def valider_workflow(workflow, rapport):
    if workflow is None:
        return []
    etats = [str(e) for e in (workflow.get("etats") or [])]
    if not workflow.get("nom"):
        rapport.erreur("workflow", "« nom » est obligatoire")
    if not etats:
        rapport.erreur(
            "workflow",
            "au moins un etat est necessaire ; le premier devient l'etat initial, celui que "
            "prend une reponse a sa soumission",
        )

    for rang, action in enumerate(workflow.get("actions") or [], 1):
        ou = f"workflow > action {rang}"
        if not isinstance(action, dict):
            rapport.erreur(ou, "chaque action devrait etre un objet")
            continue
        nom = action.get("nom")
        ou = f"workflow > action « {nom} »" if nom else ou
        for champ in ("nom", "de", "vers"):
            if not action.get(champ):
                rapport.erreur(ou, f"« {champ} » est obligatoire")
        for champ in ("de", "vers"):
            valeur = action.get(champ)
            if valeur and valeur not in etats:
                rapport.erreur(f"{ou}, champ « {champ} »", f"etat inconnu : « {valeur} »", etats)

        for rang_tache, tache in enumerate(action.get("taches") or [], 1):
            ou_tache = f"{ou} > tache {rang_tache}"
            if not isinstance(tache, dict):
                rapport.erreur(ou_tache, "chaque tache devrait etre un objet")
                continue
            type_tache = tache.get("type")
            if not type_tache:
                rapport.erreur(ou_tache, "« type » est obligatoire")
            elif normaliser(type_tache) not in TYPES_DE_TACHE:
                rapport.erreur(ou_tache, f"type de tache inconnu : « {type_tache} »", TYPES_DE_TACHE)
            if tache.get("config") is not None and not isinstance(tache["config"], dict):
                rapport.erreur(ou_tache, "« config » devrait etre un objet")
    return etats


def valider_formulaire(formulaire, rapport):
    if formulaire is None:
        return {}, {}, []
    if not formulaire.get("titre"):
        rapport.erreur("formulaire", "« titre » est obligatoire")

    etapes = formulaire.get("etapes") or []
    if not etapes:
        rapport.erreur("formulaire", "au moins une etape est necessaire")

    questions = {}
    choix_par_question = {}
    titres_etapes = []

    for rang, etape in enumerate(etapes, 1):
        if not isinstance(etape, dict):
            rapport.erreur(f"formulaire > etape {rang}", "chaque etape devrait etre un objet")
            continue
        titre = etape.get("titre")
        if not titre:
            rapport.erreur(f"formulaire > etape {rang}", "« titre » est obligatoire")
            continue
        titres_etapes.append(titre)
        ou_etape = f"formulaire > etape « {titre} »"

        groupes = [g.get("titre") for g in (etape.get("groupes") or []) if isinstance(g, dict)]
        for groupe in etape.get("groupes") or []:
            if isinstance(groupe, dict) and not groupe.get("titre"):
                rapport.erreur(f"{ou_etape} > groupe", "« titre » est obligatoire")

        for question in etape.get("questions") or []:
            if not isinstance(question, dict):
                rapport.erreur(f"{ou_etape} > question", "chaque question devrait etre un objet")
                continue
            titre_q = question.get("titre")
            ou_q = f"{ou_etape} > question « {titre_q} »" if titre_q else f"{ou_etape} > question"
            if not titre_q:
                rapport.erreur(ou_q, "« titre » est obligatoire")
                continue
            if titre_q in questions:
                rapport.avertir(
                    ou_q,
                    "deux questions portent ce libelle : les conditions et les reponses de "
                    "parcours s'y relient par le libelle, et ne sauront pas laquelle designer",
                )
            questions[titre_q] = titre

            type_q = question.get("type")
            if not type_q:
                rapport.erreur(ou_q, "« type » est obligatoire")
            elif normaliser(type_q) not in TYPES_DE_QUESTION:
                rapport.erreur(ou_q, f"type de question inconnu : « {type_q} »", TYPES_DE_QUESTION)

            choix = [str(c) for c in (question.get("choix") or [])]
            choix_par_question[titre_q] = choix

            groupe = question.get("groupe")
            if groupe and groupe not in groupes:
                rapport.erreur(ou_q, f"groupe inconnu sur cette etape : « {groupe} »", groupes)

            validation = question.get("validation")
            if validation is not None:
                if not isinstance(validation, dict) or not validation.get("regle"):
                    rapport.erreur(f"{ou_q} > validation", "« regle » est obligatoire")

    # Les conditions se verifient une fois toutes les questions connues : une question peut en
    # commander une autre posee sur une etape ulterieure.
    for etape in etapes:
        if not isinstance(etape, dict):
            continue
        for question in etape.get("questions") or []:
            if not isinstance(question, dict):
                continue
            controler_condition(question.get("affichee-si"),
                                f"formulaire > etape « {etape.get('titre')} » > question "
                                f"« {question.get('titre')} » > affichee-si",
                                rapport, questions, choix_par_question)

    controler_enchainement(formulaire, rapport, titres_etapes, questions, choix_par_question)

    for titre_q in formulaire.get("questions-rouvertes") or []:
        if str(titre_q) not in questions:
            rapport.erreur("formulaire > questions-rouvertes",
                           f"aucune question ne s'intitule « {titre_q} »", list(questions))

    return questions, choix_par_question, titres_etapes


def controler_condition(condition, ou, rapport, questions, choix_par_question):
    if condition is None:
        return
    if not isinstance(condition, dict):
        rapport.erreur(ou, "la condition devrait etre un objet")
        return
    pilote, valeur = condition.get("question"), condition.get("vaut")
    if not pilote or valeur is None:
        rapport.erreur(ou, "« question » et « vaut » sont obligatoires")
        return
    if pilote not in questions:
        rapport.erreur(ou, f"aucune question ne s'intitule « {pilote} »", list(questions))
        return
    choix = choix_par_question.get(pilote) or []
    if choix and str(valeur) not in choix:
        rapport.erreur(ou, f"la question « {pilote} » ne propose pas « {valeur} »", choix)


def controler_enchainement(formulaire, rapport, titres_etapes, questions, choix_par_question):
    enchainement = formulaire.get("enchainement")
    if enchainement is None or enchainement == "lineaire":
        return
    if not isinstance(enchainement, list):
        rapport.erreur("formulaire > enchainement",
                       "devrait valoir « lineaire » ou etre une liste de liaisons")
        return
    for liaison in enchainement:
        if not isinstance(liaison, dict):
            rapport.erreur("formulaire > enchainement", "chaque liaison devrait etre un objet")
            continue
        for champ in ("de", "vers"):
            valeur = liaison.get(champ)
            if not valeur:
                rapport.erreur("formulaire > enchainement", f"« {champ} » est obligatoire")
            elif valeur not in titres_etapes:
                rapport.erreur(f"formulaire > enchainement, champ « {champ} »",
                               f"etape inconnue : « {valeur} »", titres_etapes)
        controler_condition(liaison.get("si"), "formulaire > enchainement > si",
                            rapport, questions, choix_par_question)

    # Une sortie sans condition posee avant une autre rend toutes les suivantes inatteignables.
    par_depart = {}
    for liaison in enchainement:
        if isinstance(liaison, dict) and liaison.get("de"):
            par_depart.setdefault(liaison["de"], []).append(liaison)
    for depart, liaisons in par_depart.items():
        for rang, liaison in enumerate(liaisons[:-1]):
            if liaison.get("si") is None:
                rapport.avertir(
                    f"formulaire > enchainement, depuis « {depart} »",
                    f"la liaison {rang + 1} n'a pas de condition et precede d'autres liaisons : "
                    "Lutece emprunte la premiere satisfaite, les suivantes ne serviront jamais",
                )


def valider_parcours(parcours, rapport, etats, questions, choix_par_question, titres_etapes,
                     workflow, formulaire):
    noms_actions = [a.get("nom") for a in ((workflow or {}).get("actions") or [])
                    if isinstance(a, dict)]

    for rang, etape in enumerate(parcours, 1):
        ou = f"parcours > etape {rang}"
        if not isinstance(etape, dict):
            rapport.erreur(ou, "chaque etape devrait etre un objet")
            continue

        if "soumission" in etape:
            if formulaire is None:
                rapport.erreur(ou, "une soumission suppose un formulaire : la section est absente")
                continue
            valider_soumission(etape["soumission"] or {}, ou, rapport, questions,
                               choix_par_question, titres_etapes)
        elif "instruction" in etape:
            if workflow is None:
                rapport.erreur(ou, "une instruction suppose un workflow : la section est absente")
                continue
            valider_instruction(etape["instruction"] or {}, ou, rapport, etats, noms_actions)
        elif "daemon" in etape:
            if not etape.get("daemon"):
                rapport.erreur(ou, "« daemon » attend la cle du daemon")
        elif "export" in etape:
            fmt = str(etape.get("export") or "").lower()
            if fmt not in FORMATS_D_EXPORT:
                rapport.erreur(ou, f"format d'export inconnu : « {fmt} »", FORMATS_D_EXPORT)
        else:
            rapport.erreur(ou, "etape de parcours inconnue",
                           ["soumission", "instruction", "daemon", "export"])


def valider_soumission(soumission, ou, rapport, questions, choix_par_question, titres_etapes):
    for titre_q, valeur in (soumission.get("reponses") or {}).items():
        if titre_q not in questions:
            rapport.erreur(f"{ou} > reponses", f"aucune question ne s'intitule « {titre_q} »",
                           list(questions))
            continue
        choix = choix_par_question.get(titre_q) or []
        if choix and str(valeur) not in choix:
            rapport.erreur(f"{ou} > reponses",
                           f"la question « {titre_q} » ne propose pas « {valeur} »", choix)

    for saisie in soumission.get("valeurs") or []:
        if not isinstance(saisie, dict):
            rapport.erreur(f"{ou} > valeurs", "chaque valeur devrait etre un objet")
            continue
        titre_q = saisie.get("question")
        if not titre_q or saisie.get("valeur") is None:
            rapport.erreur(f"{ou} > valeurs", "« question » et « valeur » sont obligatoires")
        elif titre_q not in questions:
            rapport.erreur(f"{ou} > valeurs", f"aucune question ne s'intitule « {titre_q} »",
                           list(questions))
        nature = saisie.get("nature", "texte")
        if str(nature).lower() not in NATURES_DE_SAISIE:
            rapport.erreur(f"{ou} > valeurs", f"nature de saisie inconnue : « {nature} »",
                           NATURES_DE_SAISIE)
        elif str(nature).lower() == "nombre":
            rapport.avertir(f"{ou} > valeurs",
                            "« nombre » s'appuie sur un role ARIA que le theme n'expose pas : "
                            "employer « texte »")

    for controle in soumission.get("verifier") or []:
        if not isinstance(controle, dict):
            rapport.erreur(f"{ou} > verifier", "chaque controle devrait etre un objet")
            continue
        if "etape" in controle:
            if controle["etape"] not in titres_etapes:
                rapport.erreur(f"{ou} > verifier, champ « etape »",
                               f"etape inconnue : « {controle['etape']} »", titres_etapes)
        elif "refuse" in controle:
            if not controle.get("question") or controle.get("accepte") is None:
                rapport.erreur(f"{ou} > verifier",
                               "un controle « refuse » demande « question » et « accepte »")
        elif controle.get("question") not in questions:
            rapport.erreur(f"{ou} > verifier",
                           f"aucune question ne s'intitule « {controle.get('question')} »",
                           list(questions))

    for iteration in soumission.get("iterations") or []:
        if isinstance(iteration, dict) and iteration.get("etape") not in titres_etapes:
            rapport.erreur(f"{ou} > iterations, champ « etape »",
                           f"etape inconnue : « {iteration.get('etape')} »", titres_etapes)

    for cle, propos in LIMITES.items():
        if soumission.get(cle):
            rapport.avertir(f"{ou} > {cle}", f"limite connue : {propos}")


def valider_instruction(instruction, ou, rapport, etats, noms_actions):
    action = instruction.get("action")
    if not action:
        rapport.erreur(f"{ou} > instruction", "« action » est obligatoire")
    elif action not in noms_actions:
        rapport.erreur(f"{ou} > instruction", f"action inconnue : « {action} »", noms_actions)

    for champ in ("etat-attendu", "sur-etat"):
        valeur = instruction.get(champ)
        if valeur and valeur not in etats:
            rapport.erreur(f"{ou} > instruction, champ « {champ} »",
                           f"etat inconnu : « {valeur} »", etats)

    notification = instruction.get("notification")
    if notification is not None:
        if not isinstance(notification, dict) or not notification.get("canal") \
                or not notification.get("message"):
            rapport.erreur(f"{ou} > instruction > notification",
                           "« canal » et « message » sont obligatoires")


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("-")]
    strict = "--strict" in sys.argv
    if len(args) != 1:
        print(__doc__, file=sys.stderr)
        return 2

    chemin = Path(args[0])
    if not chemin.is_file():
        print(f"ERREUR : fichier introuvable : {chemin}", file=sys.stderr)
        return 2

    rapport = Rapport()
    contenu = charger(chemin, rapport)
    if contenu is not None:
        valider(contenu, rapport)

    if rapport.erreurs:
        print(f"✗ {chemin} — {len(rapport.erreurs)} erreur(s)\n")
        for e in rapport.erreurs:
            print(f"  • {e}")
        if rapport.avertissements:
            print()
    if rapport.avertissements:
        print(f"⚠ {len(rapport.avertissements)} avertissement(s)\n")
        for a in rapport.avertissements:
            print(f"  • {a}")

    if rapport.erreurs:
        return 1
    if strict and rapport.avertissements:
        print("\n✗ mode strict : les avertissements sont bloquants")
        return 1
    if not rapport.avertissements:
        print(f"✓ {chemin} — la suite est executable")
    else:
        print(f"\n✓ {chemin} — executable, sous reserve des avertissements ci-dessus")
    return 0


if __name__ == "__main__":
    sys.exit(main())
