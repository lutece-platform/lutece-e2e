# Décrire une suite de tests au lieu de la coder

Un site Lutèce porte un fichier qui décrit sa démarche. La pipeline le lit et l'exécute avec les
briques déjà écrites. Ajouter un site ne demande plus de toucher à ce dépôt.

Le fichier vit **à la racine de l'image du site**, à côté de `.e2e-config.json` :

```
image-du-site:/
├── .e2e-config.json     déclenchement (testSuite, …)
└── .e2e-suite.yml       la démarche décrite
```

YAML ou JSON — le YAML en est un sur-ensemble, le même lecteur traite les deux. Préférez le YAML :
il admet des commentaires, et une suite de tests gagne à dire pourquoi elle vérifie ce qu'elle
vérifie.

## Lancer

Dans Jenkins, choisir `SuiteDeclarative` comme `TEST_SUITE`. Le fichier est extrait de l'image,
rien d'autre à faire.

Pour éprouver une description **avant** de l'embarquer dans l'image, téléverser le fichier depuis
son poste dans le paramètre `SUITE_UPLOAD` du job. Renseigné, il fait foi : l'image n'est pas
ouverte. C'est aussi ce qui rend `SuiteDeclarative` utilisable en mode `EXTERNAL`, où aucune image
n'accompagne le site visé.

En local :

```bash
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.declaratif.SuiteDeclarativeTest \
  -Dsuite.fichier=/chemin/vers/.e2e-suite.yml \
  -Dlutece.base.url=https://mon-site/lutece
```

Ou, en montant l'instance soi-même :

```bash
mvn test -pl lutece-e2e-tests \
  -Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.ContainerSuiteDeclarativeSuite \
  -Dsuite.fichier=/chemin/vers/.e2e-suite.yml \
  -Dlutece.image=${DOCKER_REGISTRY}/mon/site:tag
```

**Vérifier un fichier sans rien lancer** — quelques millisecondes, pas besoin de navigateur :

```bash
# depuis le dépôt d'un site, sans rien d'autre que Python
python3 .claude/skills/creer-suite-e2e/scripts/valider-suite.py .e2e-suite.yml

# depuis ce dépôt, le lecteur fait autorité
mvn test -pl lutece-e2e-tests -Dtest=LecteurDeSuiteTest
```

Une skill, `creer-suite-e2e`, guide l'écriture d'un fichier et embarque ce validateur. Son
vocabulaire est comparé à celui du lecteur par `VocabulaireDuValidateurTest`, qui échoue si les
deux divergent.

## Les quatre sections

```yaml
suite:        # qui on est
workflow:     # les états et les actions à bâtir
formulaire:   # les étapes, groupes et questions à bâtir
parcours:     # ce qu'on joue ensuite sur la démarche
```

Toutes sont facultatives sauf `suite`. Un fichier peut ne décrire qu'un formulaire, ou ne rien
bâtir et seulement jouer un parcours sur une démarche déjà en place.

### `workflow`

```yaml
workflow:
  nom: Traitement des déclarations
  états:                              # le premier est l'état initial
    - Déclaration effectuée
    - En cours de vérification
  actions:
    - nom: Débuter la vérification
      de: Déclaration effectuée
      vers: En cours de vérification
      taches:
        - type: statut-publication
          config:
            published: true
```

`config` porte les champs tels que le plugin les nomme. Leur nature — liste, bouton radio, saisie —
n'est pas à déclarer : elle est lue sur l'écran de la tâche au moment de la remplir.

### `formulaire`

```yaml
formulaire:
  titre: Ma démarche
  enchainement: lineaire              # ou une liste de { de, vers }
  etapes:
    - titre: Identité
      initiale: true
      groupes:
        - titre: Coordonnées
        - titre: Activités
          iterations: 10              # groupe répétable
      questions:
        - type: radio
          titre: "Avez-vous une activité ?"
          choix: [Oui, Non]
        - type: texte
          titre: Nom de l'employeur
          groupe: Activités           # range la question dans ce groupe
          affichee-si:
            question: "Avez-vous une activité ?"
            vaut: Oui
    - titre: Validation
      finale: true
    - titre: Instruction
      hors-parcours: true             # réservée au back-office, aucune liaison n'y mène
```

### `parcours`

```yaml
parcours:
  - soumission:
      nom: Sans activité à déclarer
      reponses:
        "Avez-vous une activité ?": Non
      verifier:
        - question: Nom de l'employeur
          masquee: true
  - instruction:
      action: Débuter la vérification
      etat-attendu: En cours de vérification
      traces: [Voir les notifications]   # fragments attendus dans l'historique
      sur-etat: Déclaration effectuée    # cible une réponse dans cet état
```

Les réponses valent pour le parcours entier : chacune est retenue sur l'écran où sa question
figure. Une réponse qui ne trouve jamais sa question arrête la soumission en le disant.

## Le vocabulaire

La casse, les accents et les séparateurs sont ignorés : `Liste_Déroulante` vaut `liste-deroulante`.

**Questions** — `texte`, `texte-long`, `nombre`, `date`, `radio`, `case-a-cocher`,
`liste-deroulante`, `liste-ordonnee`, `fichier`, `image`, `camera`, `commentaire`,
`conditions-utilisation`, `numerotation`, `telephone`, `geolocalisation`, `creneau`, `session`,
`attribut-mylutece`

**Tâches** — `statut-publication`, `date-modification`, `commentaire`, `confirmation`,
`notification-usager`, `alerte-usager`, `piece-jointe`, `demande-complement`, `correction-saisie`,
`edition-reponse`, `edition-reponse-fo`, `edition-reponse-auto`, `duplication-reponse`,
`valorisation-auto`, `affectation-entite`, `affectation-entite-auto`, `desaffectation-entite`,
`notification-entite`, `assignation-utilisateur`, `archivage`, `choix`, `changement-etat-auto`,
`choix-etat-suivant`, `export-pdf`

Un mot inconnu arrête la lecture en listant ceux qui conviennent. Pour en ajouter un, une ligne
dans `Vocabulaire.java` suffit.

## Deux pièges de forme

**Les libellés qui contiennent `?` ou `,`** ne passent pas dans le style `{ clé: valeur }` du YAML,
qui y voit ses propres indicateurs. Écrire en style bloc, ou guillemeter :

```yaml
affichee-si:
  question: "Avez-vous une activité ?"      # et non { question: Avez-vous ... }
  vaut: Oui
```

**Les libellés sont la clé de tout.** Une condition, une réponse de parcours et une question se
relient par leur libellé exact. Le lecteur vérifie ces liens avant toute exécution : une condition
qui vise une question absente, ou une valeur que la question ne propose pas, est refusée en citant
ce qui existe.

## Trois exemples complets

Dans `lutece-e2e-tests/src/test/resources/suites/`, trois démarches de structures volontairement
différentes, toutes vérifiées contre une instance réelle. À recopier selon ce qui ressemble le
plus à la vôtre.

| Fichier | Forme | Ce qu'il montre |
|---|---|---|
| `deontologie.e2e-suite.yml` | **linéaire**, 8 étapes | affichage conditionnel à l'échelle, groupes répétables, instruction. Transcription d'une suite qui existait en Java |
| `subvention.e2e-suite.yml` | **à embranchement** | liaison conditionnée et sortie par défaut, organisation unittree, options, validation de saisie, deux branches soumises |
| `inscription.e2e-suite.yml` | **sans workflow** | démarche de simple collecte : les sections sont facultatives, le formulaire se publie sans workflow à associer |

## Les sections facultatives

```yaml
organisation:                 # entités unittree, pour les tâches d'affectation
  entites: [Direction, Service]
  affecter-un-agent: true

formulaire:
  options:                    # page de modification du formulaire
    disponible-du: today
    reponses-max: 100
    recapitulatif: true
  questions-rouvertes: [Nom]  # ce qu'une demande de correction rouvre à l'usager
  mapping-notification: true  # où la notification lit les coordonnées
  etapes:
    - questions:
        - type: texte
          titre: Courriel
          validation:         # règle de validation
            regle: Email
            message: Saisie invalide

  enchainement:               # embranchement : l'ordre compte
    - de: Identité
      vers: Subvention
      si:
        question: Nature de la demande
        vaut: Subvention
    - de: Identité            # sortie par défaut, en second
      vers: Information

parcours:
  - soumission:
      valeurs:                # valeurs précises, au lieu du remplissage automatique
        - question: Nom
          valeur: Dupont
        - question: Date de naissance
          nature: date        # texte | nombre | date
          valeur: 15/10/1980
      verifier:
        - etape: Subvention            # l'étape atteinte
        - etape: Information
          ecartee: true                # celle qui ne doit pas l'être
        - question: Courriel
          refuse: pas-une-adresse      # doit être refusé
          accepte: a@example.com       # doit passer
          message: Saisie invalide
      iterations:
        - etape: Subvention            # ajoute puis retire une itération
      brouillon: true
  - instruction:
      action: Prendre en charge
      etat-attendu: En cours d'instruction
      traces: [Commentaire d'instruction]
      sur-etat: Nouvelle demande
      notification:
        canal: Agent
        message: Votre demande est en cours
      lien-fo: true
  - daemon: formsIndexerDaemon
  - export: csv
```

## Ce que le fichier ne couvre pas

Trois limites, constatées en transcrivant deux démarches et vérifiées sur une instance réelle :

| Limite | Pourquoi | Contournement |
|---|---|---|
| **Tâches à configuration en plusieurs temps** (affectation à une entité, notification multicanal) | `config:` est une table plate ; ces écrans demandent une séquence avec des boutons intermédiaires | garder une suite Java pour ces tâches |
| **`reponses` sur une liste déroulante** | la brique de sélection ne reconnaît que les questions rendues dans un `fieldset` ou un `.form-group`, ce que le thème ne fait pas pour les listes | laisser le remplissage automatique s'en charger |
| **`nature: nombre`** | s'appuie sur le rôle ARIA `spinbutton`, que le thème n'expose pas | employer `nature: texte`, qui retombe sur le libellé et son champ voisin |
| **`iterations`** | la brique ne détecte pas le bloc d'itération sur un formulaire bâti ainsi | à reprendre dans la brique |
| **`brouillon`** | le contrôle de sauvegarde n'apparaît pas en front office : la reprise d'un brouillon suppose vraisemblablement un usager authentifié | employer `options.authentification: true`, non vérifié |

Le vocabulaire est posé pour les quatre : ce sont les briques sous-jacentes qu'il faudra reprendre,
et le fichier n'aura pas à changer.

Une démarche qui a besoin de ce qui manque garde sa suite Java — les deux familles cohabitent et
partagent les mêmes briques, donc rien ne diverge.
