# Allure Agent Mode

Guide projet pour le travail sur les tests de `lutece-e2e`.

## Pourquoi le mode agent

Une execution de tests est un instrument, pas un verdict a gratter dans la console. Passee par
`allure agent`, elle devient un compte rendu relisible de ce qui s'est reellement produit — etapes,
captures, extraits d'etat — assorti de constats sur les tests eux-memes : un test sans assertion,
une preuve attendue absente. On y declare ce que l'execution doit etablir (`--goal`), et le rapport
porte ce but a cote des preuves.

Ce projet pilote un site Lutece reel a travers un navigateur. Les echecs y viennent rarement du
code de test : ils viennent de l'etat du site, d'un droit non ouvert, d'une propriete de
configuration absente. La console dit « rouge » ; le rapport dit lequel de ces cas s'est produit.

## Capacites locales

| | |
| --- | --- |
| CLI Allure | `npx allure` — le depot n'a pas de wrapper dedie |
| Commandes verifiees | `run`, `inspect`, `latest`, `query`, `rerun`, `select`, `stateDir` |
| Instantane | verifie le 2026-10-03 par `npx allure agent capabilities --json` |
| Rafraichir | `npx allure agent capabilities --json` et `npx allure agent --help` |

Le mode agent complet demande Allure `3.11.0` ou plus recent, et `inspect` demande `3.12.0`.
Verifier avec `npx allure --version` avant de s'appuyer sur ses conclusions.

## Surfaces de test

| | |
| --- | --- |
| Frameworks | JUnit 5, Playwright Java, Testcontainers |
| Module de tests | `lutece-e2e-tests` |
| Briques macro | `src/test/java/fr/paris/lutece/e2e/tests/macro/` |
| Fragments de scenario | `.../macro/scenarios/` |
| Suites | `.../tests/suites/` et `.../tests/bo/testsuites/` |
| Resultats Allure | `lutece-e2e-tests/target/allure-results` (declare dans `lutece-e2e-tests/pom.xml`) |
| Traces Playwright | `lutece-e2e-tests/target/traces` |
| Captures d'echec | `lutece-e2e-tests/target/screenshots/failures` |

## Profils d'execution

Le meme test ne se comporte pas pareil selon la cible : c'est le premier reflexe de diagnostic.

**Site deja en service** — droits ouverts, configuration en place :

```bash
mvn -o test -pl lutece-e2e-tests -Dtest=<Suite> \
  -Dlutece.base.url=<url> -Dtest.admin.username=<user> -Dtest.admin.password=<mdp> \
  -Dtest.admin.password.rotate=false -Dtest.headless=true
```

`-Dtest.admin.password.rotate=false` empeche le test de changer durablement le mot de passe d'un
site partage. Sur une instance jetable, la rotation peut rester active.

**Conteneur** — instance neuve, droits a ouvrir, identifiants par defaut :

```bash
mvn -o test -pl lutece-e2e-tests -Dtest=ContainerFormsParcoursCompletSuite \
  -Dlutece.image=<registre>/bild/p30/site-integration-forms:8.0.0-SNAPSHOT \
  -Dlutece.context.root=/lutece -Dtest.headless=true
```

En local, deux reglages d'environnement sont necessaires : le socket podman est active a la demande
(`systemctl --user start podman.socket`, puis `DOCKER_HOST=unix:///run/user/1000/podman/podman.sock`),
et `~/.testcontainers.properties` force une strategie qui ignore `DOCKER_HOST` — la surcharger avec
`-Ddocker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy`.

## Signal d'execution

Le POM global configure Surefire avec `testFailureIgnore=true` : **`BUILD SUCCESS` ne signifie pas
que les tests passent**. Le verdict se lit dans la sortie du mode agent, ou a defaut dans
`target/surefire-reports/*.txt`.

Une suite entiere peut aussi basculer en « skipped » si une brique leve une hypothese non verifiee
au milieu du parcours. Un run « 0 echec » merite donc qu'on regarde le nombre de tests ignores.

## Controles d'attente

Les options verifiees localement : `--expect-tests`, `--expect-test`, `--expect-prefix`,
`--expect-label`, `--forbid-label`, `--expect-step-containing`, `--expect-steps`,
`--expect-attachments`, `--expect-attachment`, `--expect-env`.

Elles servent quand une preuve doit etre etablie, pas ajoutees par principe. Exemple utile ici :

```bash
npx allure agent --goal "le parcours prouve le changement d'etat et la trace des taches" \
  --results-dir lutece-e2e-tests/target/allure-results \
  --expect-attachments 1 \
  --expect-step-containing "Verifier l'etat et l'historique de la reponse" \
  -- mvn -o test -pl lutece-e2e-tests -Dtest=FormsParcoursCompletSuite ...
```

Chaque execution utilisant des attentes doit employer des attentes fraiches, propres a son
perimetre.

## Boucles principales

**Travailler sur un test** — lire les regles de conception du skill `$allure-agent-mode` avant
d'ecrire ; executer le seul perimetre touche avec un `--goal` ; lire la sortie agent (`index.md`,
les constats, les preuves) avant le code source ; enrichir si la preuve est faible, puis relancer.

**Trier un echec** — la sortie nomme l'echec avec son contexte. Confirmer une instabilite en
rejouant les seuls tests tombes : `npx allure agent --rerun-latest --rerun-preset <preset>`.

**Echec de CI** — recuperer les `allure-results` et les examiner sans reproduire localement :
`npx allure agent inspect <dossier-allure-results>`. Des archives de `allure run --dump` s'examinent
avec `--dump`.

**Deboguer** — un test met deja l'environnement en place et capture les preuves : le lancer sous
mode agent plutot qu'ecrire un script jetable ou piloter un navigateur a la main.

## Sortie, etat, executions concurrentes

`allure agent` cree et affiche un repertoire temporaire ; depuis `3.12.0`, chaque execution efface
celui de la precedente. Ne preciser `--output` que si un chemin particulier est necessaire — c'est
alors a l'appelant de le nettoyer.

Deux executions simultanees doivent chacune passer leur propre `--output` : la sortie temporaire par
defaut serait ecrasee par l'autre. Ne jamais partager un chemin de sortie ni un etat d'attente entre
executions paralleles.

Utiliser `--report off` pour les boucles de travail, `--report auto` pour une validation destinee a
etre relue ou partagee.

## Conventions de metadonnees

Deja en place dans le depot, a conserver :

- `@Epic` / `@Feature` / `@Story` sur chaque brique et chaque suite
- `@Step` sur les methodes `run(...)` des briques, dont le libelle dit l'action metier
- tags `macro`, `brick`, et le domaine (`forms`, `workflow`, `unittree`)
- `@Description` sur une suite, pour dire ce que le parcours etablit plutot que repeter son nom
- `@Param(excluded = true, mode = Parameter.Mode.HIDDEN)` sur le contexte partage entre briques :
  il s'affichait comme une adresse memoire, differente a chaque execution, et parasitait la
  comparaison entre deux runs

## Conventions de preuves

`fr.paris.lutece.e2e.tests.macro.Evidence` porte les trois usages du depot :

- `capture(page, titre)` — une capture aux moments ou l'etat observe fonde le verdict, pas a chaque
  action : leur volume masquerait le comportement eprouve
- `texte(titre, contenu)` — l'extrait lu, quand c'est lui qui fonde le verdict
- `contexteDExecution(url)` — site, navigateur, mode headless, portes par le test

`ScreenshotOnFailureExtension` ajoute par ailleurs capture et trace Playwright en cas d'echec.

Les moments instrumentes aujourd'hui : detail d'une reponse instruite, multivue apres soumission,
contenu d'une notification. Un test vert doit laisser de quoi juger ce que le site a repondu.

## Regles de conception des tests

Le detail vit dans le skill `$allure-agent-mode` (`references/test-design.md`). Le plancher, a tenir
meme sans le skill :

- un test est un contrat de comportement : ne pas affaiblir une assertion, ignorer un test ou
  supprimer une couverture pour faire passer une execution
- ne jamais conclure sur un message de confirmation : relire l'etat persiste. Plusieurs ecrans de ce
  site confirment une operation qu'ils viennent de refuser
- attendre l'etat d'un element plutot que le constater (`MacroSupport.exigerVisible` /
  `exigerPresent`) ; pas de pause arbitraire ni de boucle de reprise manuelle
- une etape doit representer un comportement reel, jamais du remplissage
- les pieces jointes proviennent de l'execution en cours
- ne pas sauter un test par un `if` ou un retour anticipe : utiliser les mecanismes du runner, avec
  une raison lisible

## Publication

`lutece-e2e-tests/scripts/publish-allure.sh` pousse les resultats vers le serveur Allure partage.
`ALLURE_SERVER_URL` est obligatoire et n'a pas de valeur par defaut. Le projet y est nomme d'apres
l'artifactId du site teste, lu dans le WAR de l'image.

## Acceptation

Une execution n'est un signal de confiance que si le perimetre correspond a l'intention, qu'aucun
test important n'est silencieusement exclu, que les preuves suffisent a relire, et que les limites
d'execution sont dites explicitement.
