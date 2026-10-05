# Vocabulaire d'un fichier de suite

Format YAML, JSON accepté — le YAML en est un sur-ensemble. Préférer le YAML : il admet des
commentaires, et une suite gagne à dire pourquoi elle vérifie ce qu'elle vérifie.

La casse, les accents et les séparateurs sont ignorés : `Liste_Déroulante` vaut `liste-deroulante`.

## Les quatre sections

Toutes facultatives sauf `suite`. Un fichier peut ne décrire qu'un formulaire, ou ne rien bâtir et
seulement jouer un parcours sur une démarche déjà en place.

```yaml
suite:          # qui on est — obligatoire
organisation:   # entités unittree, pour les tâches d'affectation
workflow:       # états et actions à bâtir
formulaire:     # étapes, groupes, questions, conditions, enchaînement
parcours:       # ce qu'on joue ensuite : soumissions et instructions
```

L'ordre d'exécution n'est pas celui du fichier mais celui qu'impose le back-office : organisation,
workflow, formulaire, mise en service, puis parcours.

## `suite`

```yaml
suite:
  nom: Déclaration d'intérêts simplifiée      # obligatoire, porté par le rapport
  description: >
    Ce que la suite éprouve, pour qui lira le rapport.
```

## `organisation`

```yaml
organisation:
  entites:                       # créées à la racine
    - Direction des subventions
    - Service instruction
  affecter-un-agent: true        # rattache un agent à la dernière entité
```

Une tâche d'affectation a besoin d'entités, et une entité d'un agent pour que l'affectation ait un
destinataire. Les deux vont ensemble.

## `workflow`

```yaml
workflow:
  nom: Instruction des subventions
  etats:                         # le PREMIER est l'état initial
    - Nouvelle demande           # celui que prend une réponse à sa soumission
    - En cours d'instruction
  actions:
    - nom: Prendre en charge
      de: Nouvelle demande
      vers: En cours d'instruction
      taches:                    # exécutées dans cet ordre
        - type: statut-publication
          config:
            published: true
```

`config` porte les champs **tels que le plugin les nomme**. Leur nature — liste, bouton radio,
saisie — n'est pas à déclarer : elle est lue sur l'écran de la tâche au moment de la remplir.

L'ordre des tâches fait partie du comportement : une notification placée avant le commentaire
qu'elle doit transmettre partirait vide.

## `formulaire`

```yaml
formulaire:
  titre: Demande de subvention

  options:                       # page de modification du formulaire
    disponible-du: today         # format flatpickr
    disponible-au: "2033-12-31"
    message-indisponible: Formulaire momentanément indisponible
    reponses-max: 100            # 0 = illimité
    une-reponse-par-usager: true
    recapitulatif: true
    brouillon: true
    fil-ariane: true
    authentification: false

  questions-rouvertes:           # ce qu'une demande de correction rouvre à l'usager
    - Nom du demandeur
  mapping-notification: true     # où la notification lit les coordonnées

  etapes:
    - titre: Identité
      initiale: true             # l'étape par laquelle l'usager commence
      groupes:
        - titre: Coordonnées
        - titre: Postes de dépense
          iterations: 10         # > 1 = groupe répétable
      questions:
        - type: radio
          titre: "Nature de la demande ?"
          choix: [Subvention, Information]
        - type: texte
          titre: Intitulé du poste
          groupe: Postes de dépense      # range la question dans ce groupe
          affichee-si:                    # affichage conditionnel
            question: "Nature de la demande ?"
            vaut: Subvention
        - type: texte
          titre: Courriel
          validation:
            regle: Email                  # libellé de l'expression régulière du site
            message: Saisie invalide

    - titre: Validation
      finale: true               # conclut le parcours

    - titre: Instruction
      hors-parcours: true        # aucune liaison n'y mène, réservée au back-office

  enchainement: lineaire         # ou une liste de liaisons :
  # enchainement:
  #   - de: Identité
  #     vers: Dossier
  #     si:                      # liaison conditionnée
  #       question: "Nature de la demande ?"
  #       vaut: Subvention
  #   - de: Identité             # sortie par défaut, EN SECOND
  #     vers: Information
```

**L'ordre des liaisons compte.** Lutèce examine celles d'une étape dans l'ordre et emprunte la
première dont la condition est satisfaite : une sortie sans condition placée avant une autre rend
toutes les suivantes inatteignables. Le validateur le signale.

### Types de question

`texte` · `texte-long` · `nombre` · `date` · `radio` (ou `bouton-radio`) · `case-a-cocher` ·
`liste-deroulante` · `liste-ordonnee` · `fichier` · `image` · `camera` · `commentaire` ·
`conditions-utilisation` · `numerotation` · `telephone` · `geolocalisation` · `creneau` ·
`session` · `attribut-mylutece`

`choix` est requis pour les types à choix (`radio`, `case-a-cocher`, `liste-deroulante`,
`liste-ordonnee`) — une question à choix sans réponses possibles ne sert à rien, et une condition
qui la vise ne pourra jamais être satisfaite.

## `parcours`

```yaml
parcours:
  - soumission:
      nom: Demande de subvention             # porté par le rapport
      reponses:                              # questions à choix
        "Nature de la demande ?": Subvention
      valeurs:                               # valeurs précises, sinon remplissage automatique
        - question: Nom du demandeur
          valeur: Dupont
        - question: Date de naissance
          nature: date                       # texte | nombre | date
          valeur: 15/10/1980
      verifier:
        - etape: Dossier                     # l'étape atteinte
        - etape: Information
          ecartee: true                      # celle qui ne doit pas l'être
        - question: Intitulé du poste
          masquee: true                      # visibilité attendue
        - question: Courriel
          refuse: pas-une-adresse            # doit être refusé
          accepte: a@example.com             # doit passer
          message: Saisie invalide
      iterations:
        - etape: Dossier                     # ajoute puis retire une itération
      brouillon: true

  - instruction:
      action: Prendre en charge
      etat-attendu: En cours d'instruction
      traces: [Commentaire d'instruction]    # fragments attendus dans l'historique
      sur-etat: Nouvelle demande             # cible une réponse dans cet état
      notification:
        canal: Agent
        message: Votre demande est en cours
      lien-fo: true                          # le lien qui ramène l'usager sur sa réponse

  - daemon: formsIndexerDaemon
  - export: csv                              # csv | pdf
```

Les réponses et valeurs valent pour le parcours entier : chacune est posée sur l'écran où sa
question figure. Une qui n'en trouve aucun arrête la soumission en le disant.

**Vérifier l'étape atteinte *et* celle qui a été écartée** est ce qui prouve un embranchement :
vérifier seulement la première laisserait passer une condition inopérante, le parcours suivant
alors simplement la première liaison sans que rien ne le signale.

### Types de tâche

`statut-publication` · `date-modification` · `commentaire` · `confirmation` ·
`notification-usager` · `alerte-usager` · `piece-jointe` · `demande-complement` ·
`correction-saisie` · `edition-reponse` · `edition-reponse-fo` · `edition-reponse-auto` ·
`duplication-reponse` · `valorisation-auto` · `affectation-entite` · `affectation-entite-auto` ·
`desaffectation-entite` · `notification-entite` · `assignation-utilisateur` · `archivage` ·
`choix` · `changement-etat-auto` · `choix-etat-suivant` · `export-pdf`

## Limites connues

Le vocabulaire les accepte, les briques sous-jacentes ne les honorent pas encore. Le validateur
avertit quand un fichier en approche.

| Limite | Contournement |
|---|---|
| Tâches à configuration en plusieurs temps (affectation à une entité, notification multicanal) | garder une suite Java pour ces tâches |
| `reponses` sur une liste déroulante | laisser le remplissage automatique s'en charger |
| `nature: nombre` | employer `nature: texte` |
| `iterations` de groupe en front office | non disponible |
| `brouillon` | suppose vraisemblablement un usager authentifié, non vérifié |

Le jour où une brique sera reprise, les fichiers n'auront pas à changer.

## Élargir le vocabulaire

Un mot se déclare à deux endroits, qui doivent rester alignés :

1. `Vocabulaire.java` du dépôt `lutece-e2e` — une ligne ;
2. `scripts/valider-suite.py` de cette skill — une entrée dans la liste correspondante.

`VocabulaireDuValidateurTest` compare les deux et échoue s'ils divergent.
