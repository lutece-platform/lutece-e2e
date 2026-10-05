---
name: creer-suite-e2e
description: Créer ou modifier le fichier .e2e-suite.yml qui décrit la suite de tests end-to-end d'un site Lutèce, puis le valider. À employer dès qu'il s'agit d'outiller un site de tests e2e, d'ajouter une démarche à sa suite, de faire évoluer une suite existante, ou de comprendre pourquoi un fichier de suite est refusé — y compris lorsque la demande ne nomme pas le fichier, par exemple « ajoute des tests e2e à ce site » ou « teste ce formulaire de bout en bout ».
---

# Décrire la suite de tests d'un site Lutèce

Un site décrit sa démarche dans un fichier ; la pipeline le lit et l'exécute avec des briques
Playwright déjà écrites. **Il n'y a pas de code de test à produire** — ni dans le dépôt du site, ni
dans `lutece-e2e`. Produire du Playwright ici serait passer à côté de l'outil.

```
depot-du-site/
├── .e2e-config.json     déclenchement (existant)
└── .e2e-suite.yml       la démarche décrite  ← ce que cette skill produit
```

> **État** — la fonctionnalité vit sur la branche `feature/suites-declaratives` du dépôt
> [lutece-e2e](https://github.com/lutece-platform/lutece-e2e), **non fusionnée**. Le fichier peut
> être écrit et validé dès maintenant ; il ne sera exécuté par la pipeline qu'après la fusion.
> Le dire au demandeur plutôt que de laisser croire que la suite tourne déjà.

## Méthode

### 1. Partir du site, jamais d'un modèle

Une suite décrit **cette** démarche-ci. La recopier d'un autre site produit un fichier qui valide
et ne prouve rien.

Chercher, dans cet ordre :

- un `.e2e-suite.yml` déjà présent — on le fait alors évoluer, on ne le réécrit pas ;
- le `.e2e-config.json`, qui dit quelle suite tourne aujourd'hui ;
- les sources du site : formulaires, workflows, modules installés ;
- ce que le demandeur sait de sa démarche, et qu'aucun fichier ne porte.

Quand une démarche existe sur un site en service et qu'on y a accès en lecture, la relever est
plus sûr que la deviner : l'export JSON d'une étape (`DoExportStepJson.jsp?id_step=N`) donne la
structure exacte, types de question et contrôles compris.

**Ne jamais écrire sur un site de recette ou de production.** Consulter, exporter, lire. Rien d'autre.

### 2. Demander ce qui ne se déduit pas

Deux choses ne se lisent nulle part et changent tout le fichier :

- **ce que la suite doit prouver** — qu'une démarche se configure, qu'un usager la mène à bien,
  qu'un agent l'instruit ? Les trois sections à remplir en découlent ;
- **les démarches qui comptent** — un site en porte souvent plusieurs ; toutes ne valent pas d'être
  éprouvées à chaque build.

### 3. Écrire le fichier

Le vocabulaire complet, les quatre sections et leurs champs : `references/vocabulaire.md`.
Trois fichiers réels, de structures opposées, à lire avant d'écrire : `references/exemples.md`.

Deux règles qui évitent l'essentiel des allers-retours :

- **les libellés sont la clé de tout.** Une condition, une réponse de parcours et une question se
  relient par leur libellé **exact**. Les choisir une fois, les recopier ensuite ;
- **un libellé contenant `?` ou `,`** ne passe pas dans le style `{ clé: valeur }`, où le YAML y
  voit ses propres indicateurs. Écrire en style bloc, ou guillemeter.

### 4. Valider — toujours, avant de rendre

```bash
python3 <skill>/scripts/valider-suite.py <chemin>/.e2e-suite.yml
```

Quelques millisecondes, aucun conteneur, aucun navigateur. Le script refuse ce que le lecteur
refusera : type hors vocabulaire, condition visant une question absente, valeur qu'une question ne
propose pas, action dont l'état n'existe pas, réponse de parcours sans question.

**Ne pas rendre un fichier qui n'a pas été validé.** Un fichier faux se découvre sinon au bout d'un
quart d'heure de conteneur, et le message parle alors d'un sélecteur introuvable plutôt que de la
ligne à corriger.

Les **avertissements** ne bloquent pas mais méritent une décision : ils signalent les limites
connues et les choix qui ne feront rien. `--strict` les rend bloquants.

Quand le dépôt `lutece-e2e` est à portée, son lecteur fait autorité :

```bash
mvn test -pl lutece-e2e-tests -Dtest=LecteurDeSuiteTest
```

### 5. Dire ce qui reste à faire

Un fichier posé ne suffit pas à ce que la suite tourne. Énoncer au demandeur :

- la bascule de `.e2e-config.json` sur `"testSuite": "SuiteDeclarative"`, **après la fusion** ;
- que ce libellé étant nouveau pour Jenkins, le job aval doit avoir tourné une fois avant qu'un
  déclenchement amont ne l'accepte ;
- que l'assemblage de l'image doit copier `.e2e-suite.yml` à sa racine, comme `.e2e-config.json`.
  **Ce point se vérifie** : si l'assemblage ne nomme que le second, le premier n'arrivera jamais
  jusqu'à la pipeline, et cela ne se verra qu'à la bascule.

## Garde-fous

- **Ne pas écrire de Playwright.** Si une démarche demande quelque chose que le vocabulaire ne
  couvre pas, le dire — et proposer une suite Java pour cette partie — plutôt que de contourner.
- **Ne pas inventer de mot.** Le vocabulaire est fermé ; le validateur énumère ce qui est accepté.
  Pour l'élargir, c'est `Vocabulaire.java` et le validateur ensemble, et le test
  `VocabulaireDuValidateurTest` vérifie qu'ils ne divergent pas.
- **Ne pas promettre ce qui n'est pas vérifié.** Cinq limites connues sont documentées : les
  signaler quand le fichier en approche, plutôt que de laisser le demandeur les découvrir.
- **Une suite qui ne prouve rien est pire qu'aucune.** Décrire un formulaire sans `parcours`, c'est
  vérifier qu'il se configure, pas qu'il fonctionne. Le dire au demandeur si c'est son choix.
