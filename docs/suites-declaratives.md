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
mvn test -pl lutece-e2e-tests -Dtest=LecteurDeSuiteTest
```

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

## Un exemple complet

`lutece-e2e-tests/src/test/resources/suites/deontologie.e2e-suite.yml` décrit la déclaration
d'intérêts de la commission de déontologie : 8 états, 7 actions, 8 étapes, groupes répétables,
contrôles conditionnels, soumission et instruction. C'est la transcription d'une suite qui existait
en Java, et le modèle à recopier.

## Ce que le fichier ne couvre pas

Les briques existent, le vocabulaire ne les expose pas encore : itérations de groupe en front
office, brouillons, exports, contrôles de validation, daemons autres que l'indexeur, unittree.
Une démarche qui en a besoin garde sa suite Java — les deux familles cohabitent et partagent les
mêmes briques.
