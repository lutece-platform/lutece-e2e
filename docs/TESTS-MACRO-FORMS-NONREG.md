# Non-régression Forms — inventaire et suites

> Plan validé le 2026-10-27 (plan « graceful-alpaca »). Ajouts seuls : aucune brique `*MacroTest` existante modifiée.

## Instance de référence (§0-bis)

- Banc lpe2e `lutece-forms-e2e` : `http://localhost:18084/lutece` (le port dépend du slot : `lpe2e port`), admin/adminadmin.
- Site pom 8.0.4, core 8.0.2-SNAPSHOT, plugin-liquibase 2.0.2-SNAPSHOT, **forms 4.0.2-SNAPSHOT** — upstream `develop` **bd4a558f** depuis le 2026-10-27 après-midi (passes du matin sur 64eb9c68, 83 commits en retard : constats 1 et 2 obsolètes). **Avant tout constat de défaut, comparer la copie locale à upstream** : `gh api repos/lutece-platform/lutece-form-plugin-forms/compare/<sha>...develop`,
  plugin-workflow, module-workflow-forms, mylutece + mylutece-database (compte FO test/testtest).
- `e2e/e2e.conf` (dans le dépôt forms, non commité) : `E2E_PLUGINS=…plugin-workflow,…module-workflow-forms`,
  `E2E_ENABLE=forms,genericattributes,filegenerator,htmltopdf,mermaidjs,regularexpression,workflow,workflow-forms`.
- **Seed obligatoire** `e2e/harness/db/seed-forms.sql` : `init_core_forms.sql` crée les droits `FORMS_*` sans les
  attribuer (l'attribution se fait à l'installation depuis l'admin, que le banc court-circuite) → sans seed,
  « Accès refusé » sur `ManageForms.jsp`. Le seed donne à l'admin (id 1) tous les droits et rôles déclarés.
- Lancement : `cd components/lutece-form-plugin-forms && lpe2e build && KEEP=1 lpe2e up`.

## Exploration Playwright (§1) — passes du 2026-10-27

Sonde jetable `tests/probe/TmpExploreFormsNonRegProbe` (hors `macro`) : provisionne avec les briques existantes
puis relève formulaires, champs, boutons et liens des écrans Forms ; les `action=` sont relevés, jamais suivis.
Sortie : `lutece-e2e-tests/target/probe/forms-nonreg/inventory.md` + captures.

### URLs et sélecteurs relevés
| Écran | URL / sélecteur |
|---|---|
| Modifier formulaire | `ManageForms.jsp?view=modifyForm&id_form=F&token=T` (onglets Description, Affichage, Dates, Restrictions, Récapitulatif, Groupe de travail) |
| Publication | `ManageForms.jsp?view=modifyPublication&id_form=F&token=T` ; questions publiées : `view=manageQuestionPublication` |
| Dupliquer formulaire | lien `ManageForms.jsp?action=duplicateForm&id_form=F&token=T` |
| Export formulaire | `export/DoExportFormJson.jsp?id_form=F&token=T` |
| Catégories | `ManageFormsCategories.jsp?view=manageCategories` / `view=createCategory` |
| Étapes | `ManageSteps.jsp?view=manageSteps&id_form=F` ; vue graphique `view=graphicView` ; duplication `action=duplicateStep&id_step=S&token=T` ; export `export/DoExportStepJson.jsp?id_step=S` ; suppression `view=confirmRemoveStep&id_step=S` |
| Modifier étape | form `#modify_step` (sur `ManageTransitions.jsp?view=manageTransitions&id_step=S`) : `#step-title`, `#titleDisplayed`, `#description`, `#initial`, `#final`, `button[name=action_modifyStep]` |
| Transitions | `ManageTransitions.jsp?view=createTransition&id_step=S` : form `#create_transition`, `select#nextStep`, `button[name=action_createTransition]` |
| Questions | `ManageQuestions.jsp?view=manageQuestions&id_step=S` ; ajout `button[name=view_createQuestion]` (un par type) ; menus « Actions » : `view=modifyQuestion&id_question=Q`, `action=duplicateQuestion&id_step=S&id_target=C`, `view=getConfirmRemoveComposite&id_display=D` |
| Export réponses (config) | `ManageFormExport.jsp?view=manageExport&id_form=F` ; `action=doSetDefaultOrder&token=T` |
| Multivue (config) | `ManageFormMultiviewConfig.jsp?view=manageMultiview&id_form=F` |
| Workflow | `ManageFormWorkflowConfig.jsp?view=manageWorkflow&id_form=F` |
| **Saisie BO** | `ManageFormResponse.jsp?view=stepView&id_form=F&init=true` (« Ajouter des données en BO ») |
| FO | `Portal.jsp?page=forms` (liste), `page=forms&view=stepView&id_form=F&init=true` |

À explorer encore, en suivant les menus et boutons (boucle §4, domaine par domaine) : choix du type de question,
groupe (création, modification, déplacement), `ModifyEntry` (valeurs de liste), contrôles, modification et priorité
des transitions, modification de catégorie, multivue avec réponses, détail d'une réponse.

### Constats
0. **Constats 1 et 2 relevés sur une copie en retard (64eb9c68)** : sur upstream `develop` bd4a558f, la première
   étape n'est plus créée finale et les duplications d'étape et de question fonctionnent (POST + `_csrftoken`,
   LUT-34002 / LUT-34208) ; la copie est titrée « Copie de <titre> ». Aucun ticket à ouvrir. Retirer la dernière
   étape finale est refusé (`ERROR_NO_FINAL_STEP`) → future brique négative `RefuseUnsetLastFinalStep`.
   La publication passe par `action_modifyFormPublication` (LUT-34237, dates seules).
1. **Briques existantes qui ne prouvent pas leur effet** (non modifiées, conformément à la règle) :
   - `SetStepInitial(A)` + `SetStepFinal(B)` laissent A cochée « Finale » (la 1re étape est créée
     initiale + finale) → « Aucune transition possible pour une étape finale ».
   - `CreateTransition` termine sans erreur alors que `ctx.transitionIds` reste vide.
   - `CreateTransition` asserte « Liaison » visible : vrai aussi sur l'écran de création (titre « Liaison ») → ne prouve rien.
   - `ValidateSummaryFO` termine avec `ctx.lastResponseId == null` : sans message de fin, Forms redirige vers le
     formulaire (`FormXPage.doSaveResponse`), l'UI FO ne prouve donc rien ; la preuve est en multivue BO.
   - `AddConditionalControl` : skip (« pilotage impossible »).
   - `NextStepFO` : skip systématique — cherche « Etape suivante » **sans accent**, le bouton réel est « Étape suivante ».
2. **Duplications BO cassées (défauts, 2026-10-27)** — chemin réel de l'UI, stack dans `messages.log` :
   - « Dupliquer l'étape » : le lien `ManageSteps.jsp?action=duplicateStep&id_step=S&token=T` porte le token
     généré pour `createStep` (`FormStepJspBean` l.252, vue manageSteps) ; l'action le refuse
     (`AdminTokenAccessDeniedException: Invalid security token for action: duplicateStep`) → « Internal error ».
   - « Dupliquer la question » : le lien `manage_questions.html:142` n'a **aucun** token alors que le contrôleur
     (`securityTokenEnabled=true`) l'exige → même refus, « Internal error ». Ce n'est donc pas une faille CSRF
     mais une fonctionnalité inutilisable.
   - Briques rouges qui les épinglent : `DuplicateStep`, `DuplicateQuestion`.
3. **Étape sans question = impasse FO** : une étape sans composite visible rend une chaîne vide
   (`StepDisplayTree.getCompositeHtml`, l.293) → ni contenu ni bouton, l'usager est bloqué. Comportement du
   plugin à qualifier (défaut ou contrainte de paramétrage) ; les suites mettent ≥ 1 question par étape.
4. **Multivue et index** : la multivue lit l'index Lucene. Juste après une soumission et une « indexation
   totale », la sonde a vu « Aucune réponse trouvée » alors que la réponse était en base ; avec l'indexation
   **incrémentale** puis relecture, `VerifyResponseRecorded` retrouve la réponse. Pas de défaut établi :
   délai d'indexation, à surveiller sur le volume.
5. **Mutations GET** : `ManageTransitions.jsp?action=moveUpPriority|moveDownPriority` — protection token à
   vérifier au moment des briques de priorité (même schéma probable que constat 2).
6. **Saisie BO** (bd4a558f, 2026-10-29) : lien « Ajouter des données en BO » de la liste
   (`ManageFormResponse.jsp?view=stepView&id_form=F&init=true`), formulaire non publié accepté. Même formulaire
   `#form-validate` et mêmes actions que le FO (`doSaveStep`, `formResponseSummary`, récapitulatif
   `#summary_validate` → `doSaveResponseSummary`), sans doublon `aria-hidden`. Le champ `displayed_questions` est
   un `input[text]` mais **masqué** à l'écran (constat initial clos) ; le `token=` vide est sans effet (`_csrftoken`
   porté). Après validation, retour sur une saisie vierge de l'étape initiale.
7. **Suppression d'un formulaire publié** : message de confirmation différent (`MESSAGE_CONFIRM_REMOVE_ACTIVE_FORM`,
   `FormJspBean` l.448) → `RemoveForm` échoue sur un formulaire publié ; le `cleanup` des briques laisse ces
   formulaires. À traiter par une brique `UnpublishForm` appelée avant `RemoveForm` dans `cleanup`.
8. **DOM** : les div offcanvas (`modifyT__step_S_transition_T`) sont retirés par le JS après chargement ; seuls
   `btn-modifyT__…` et le lien `confirmRemoveTransition&id_transition=` restent → oracle sur ce lien.
   En FO, chaque bouton d'action a un doublon `visually-hidden aria-hidden` placé **avant** lui :
   viser `:not([aria-hidden='true'])` ou `getByRole`.
9. **`AddQuestion*` n'enregistre pas l'id** : `ctx.questions[].id` reste 0 ; `view=modifyQuestion&id_question=0`
   répond « Internal error » (robustesse : un id inconnu devrait rediriger). → `VerifyQuestionsListed` résout
   les ids par titre exact (ligne `li[data-url*=view=modifyQuestion]`, titre `h4 .searchable`).
10. **Modification de transition** (re-vérifié sur bd4a558f, 2026-10-29) : l'offcanvas charge en iframe
    `ManageTransitions.jsp?view=modifyTransition&id_step=S&id_transition=T` (form `#modify_transition`,
    `select#nextStep`, `action_modifyTransition`). La liste exclut toute étape déjà ciblée depuis S, **y compris
    la cible actuelle** de T (`getTransitionTargetStepReferenceList`) : impossible d'enregistrer T sans changer sa
    cible, et liste vide quand toutes les autres étapes sont liées. **Défaut candidat** (à valider avant ticket).
11. **Dépublication** : écran `view=modifyPublication` (dates flatpickr cachées), vider les deux dates puis
    `action_modifyForm` ; le FO répond alors par `SiteMessage.jsp` (« Formulaire indisponible »).
12. **Catégories** (bd4a558f) : liste en cartes, modification en offcanvas iframe `view=modifyCategory&id_category=C`
    (`#name`, `#code`, `action_modifyCategory`), suppression `view=confirmRemoveCategory` → AdminMessage
    `action=removeCategory`. **Supprimer une catégorie utilisée par un formulaire est accepté** et laisse
    `forms_form.id_category` pointer sur une catégorie inexistante (`doRemoveCategory` ne vérifie pas l'usage ;
    l'écran du formulaire ne plante pas, sa liste n'a plus de sélection). **Défaut candidat** (à valider).
13. **Liaison automatique à la création d'étape** (FORMS-546, 2021) : `create_step.html` propose « étape
    précédente » pré-remplie avec la dernière étape, et `doCreateStep` crée la transition précédente → nouvelle.
    Conséquence sur la brique existante `CreateTransition(A, B)` quand B est déjà liée : B n'est plus proposée,
    le repli par libellé échoue en silence et la **première option restante est soumise** (A→C créée), sans
    erreur ; avec 2 étapes la liste est vide et la création est refusée (LUT-34282), toujours sans erreur.
    `VerifyTransitionExists` passe alors sur la liaison automatique → `VerifyTransitionTargets` (ensemble exact).
14. **`NextStepFO`** reste en skip sur bd4a558f (libellé sans accent) ; `GoToNextStepFO` couvre le pas.
15. **`transitionsFrom`** ne lit plus le texte des `<script>` inline des cartes (le template en injecte un par
    bouton de modification).
16. **Groupes et déplacement** (bd4a558f, 2026-10-29) :
    - carte de groupe `.card-question#group-D` (h3 = titre) dans un `li` qui porte **aussi** un `data-url`
      modifyQuestion (id = id du groupe) → `questionsOfStep` exclut ces lignes ;
    - modification : `#btn-modify-group-D` → iframe `view=modifyGroup&id_step=S&id_group=G` (`#group-title`,
      itérations, `action_modifyGroup`) ;
    - suppression : `view=getConfirmRemoveComposite&id_display=D` → AdminMessage `action=removeComposite` ; supprime
      le groupe **et ses questions** (`deleteDisplayAndDescendants`), refus si réponses rattachées ou contrôle
      conditionnel sur la question ;
    - déplacement : `#btn-move-comp-D` → iframe `view=moveComposite`, assistant `#move_composite` en 3 temps
      (`id_step` + `validateStep`, `id_parent` 0/racine ou id_display du groupe + `validateGroup`, `displayOrder`
      + `action_moveComposite` en POST) ; placement relu sur `own_display_id_composite` / `own_display_id_parent`.
    - Les briques existantes `MoveQuestionIntoGroup` et `VerifyGroupHierarchy` se mettent en **skip** sur
      bd4a558f : GET `action=moveComposite` sans token, et `display_order` au lieu de `displayOrder`.
      → `MoveQuestion` les remplace pour la non-régression.
17. **Volume** (2026-10-29) : parcours UI complet par soumission, ≈ 1,5 s sur le banc local. Mesuré : 20 FO en 60 s,
    20 BO en 53 s, 60 BO en 103 s, compte exact retrouvé en multivue à chaque fois. La multivue est **paginée
    (50 par défaut)** : `multiviewResponseIds` force `items_per_page` sinon `VerifyResponseRecorded` échoue
    à tort au-delà de 50. Les briques FO existantes (`OpenFormFO`, `ViewSummaryFO`…) se mettent en skip
    (`Assumptions`) : les lots passent par `ResponseBatchEngine`, assertions strictes à chaque pas.
18. **Refus** (bd4a558f, 2026-10-29), conditions lues dans le code puis observées à la sonde (`exploreNegatives`) :
    - dernière étape finale décochée (`FormStepJspBean.doModifyStep`) : retour sur `view=modifyStep`, message
      « Vous n'avez défini aucune étape finale » rendu en **toast** (`formsToastr`, `.toast-body`), pas en `.alert` ;
      l'étape reste finale ;
    - question obligatoire vide en FO : même étape, champ `is-invalid`, « Ce champ est obligatoire. » ; l'interrupteur
      « Obligatoire » est `#mandatory` dans `#modifyEntryForm` (bouton « Appliquer » `action_saveQuestion`) ;
    - question / groupe portant une réponse (`getConfirmRemoveComposite`) : AdminMessage STOP, texte dans le corps
      de page (aucun `.alert`), pas de confirmation `removeComposite` ;
    - formulaire dépublié : `SiteMessage.jsp`, « Formulaire indisponible » ;
    - fichier qui n'est pas du JSON : liste des formulaires, « Erreur lors de l'import du formulaire », rien de créé ;
    - **JSON valide sans clé `form` (`{}`) : `NullPointerException` dans `FormJsonService.importForm:211`
      (`form.getTitle()`), page « Internal error »** → point C ci-dessous, cas remonté rouge.
19. **Requalification des briques à `Assumptions`** (bd4a558f, 2026-10-29) — aucune n'était sensible à la version :
    - `AddConditionalControl`, `AddValidationControl`, `VerifyValidationErrorFO`, `RemoveControl` : vertes en standalone ;
    - `VerifyConditionalDisplayFO` (skip) : le standalone pose la condition sur une pilote **texte** avec la valeur « test »,
      et `AddConditionalControl` remplace en silence une valeur absente par la 1re option (« Fichier JPG ») → condition
      jamais vraie (vert mais faux pour `AddConditionalControl`). Une pilote radio exige aussi de choisir le validateur
      « Valeur exacte » (`forms.listValueValidator`), non choisi par la brique, et des valeurs de liste (`AddQuestion`
      crée une radio sans option). Chemin réel : offcanvas `view=manageConditionControl&id_target=<id_display>` →
      « Ajouter un contrôle » → `#modify_condition_control` en 3 temps. FO : bloc `.display_field_<id>_0`
      `display:none` → visible avec « Oui » → masqué avec « Non ». La vue des conditions n'affiche pas la valeur.
      → `AddDisplayCondition` + `VerifyConditionalRevealFO` ;
    - `AddIterationFO` / `RemoveIterationFO` (skip) : seul leur standalone échoue (`MoveQuestionIntoGroup`) ; composées
      avec `MoveQuestion`, elles passent (`fieldset.step-group-item` 1 → 2 → 1, `action_addIteration` /
      `action_removeIteration`) ;
    - `SaveDraftFO` (skip) : le brouillon exige un **usager FO connecté** (`step_view.html` : `user?? && backupEnabled` ;
      `doSaveForBackup` renvoie l'anonyme vers la connexion). Banc : MyLutece database présent. Un usager créé en BO a un
      mot de passe **expiré dès la 1re connexion** (« Votre mot de passe n'est plus valide » → `DoChangePassword.jsp`).
      Recherche d'usager dans l'offcanvas `#offcanvasSearch` ; id `first_password` en double dans `CreateUser`
      (champ + jauge, défaut HTML mylutece mineur). Brouillon : « Reprise de la sauvegarde du … », absent une fois
      déconnecté, restauré après reconnexion → `CreateFrontUserBO`, `LoginFrontUserFO`, `LogoutFrontUserFO`,
      `RemoveFrontUserBO`, `SaveBackupFO`, `VerifyBackupFO`.

## Briques ajoutées (option C) — passe live du 2026-10-27 (mardi), banc forms 4.0.2-SNAPSHOT

| Brique | Rôle | Oracle | Live |
|---|---|---|---|
| `UnsetStepFinal` | décoche « Finale » (1re étape créée initiale+finale) | badges de `#step_<id>` | ✅ |
| `VerifyStepFlags` | derrière SetStepInitial/Final/UnsetStepFinal | badges exacts Initiale/Finale | ✅ |
| `VerifyTransitionExists` | existence d'une transition, résout l'id ; ne prouve pas une création (liaison auto) : encadrer `CreateTransition` par `VerifyTransitionTargets` avant/après | lien `confirmRemoveTransition&id_transition`, texte feuille = titre cible | ✅ |
| `GoToNextStepFO` | complète `NextStepFO` (accent) | `input[name=id_step]` = id de l'étape attendue | ✅ |
| `UnpublishForm` | avant `RemoveForm` (utilisée par `cleanup`) | dates de `modifyPublication` vides, titre inchangé, FO = `SiteMessage.jsp` sans `#form-validate` | ✅ |
| `VerifyQuestionsListed` | derrière `AddQuestion*` ; résout les ids | titre exact listé une seule fois dans l'étape | ✅ |
| `ModifyForm` | titre + description | relecture `modifyForm`, même id dans la liste | ✅ |
| `DuplicateStep` | menu Actions, formulaire POST | 1 carte nouvelle, titre « Copie de … », mêmes questions | ✅ (bd4a558f) ; ❌ sur 64eb9c68 |
| `DuplicateQuestion` | menu Actions, formulaire POST | 1 question nouvelle, titre « Copie de … » | ✅ (bd4a558f) ; ❌ sur 64eb9c68 |
| `VerifyTransitionTargets` | derrière les créations d'étapes/transitions | ensemble exact des étapes d'arrivée, une transition par cible | ✅ |
| `ModifyTransition` | iframe `modifyTransition` | même id de transition, nouvelle cible, plus aucune vers l'ancienne | ✅ |
| `CreateFormCategory` | iframe `createCategory` | 1 id nouveau, libellé code + nom | ✅ |
| `ModifyFormCategory` | iframe `modifyCategory` | même id, nouveau libellé | ✅ |
| `RemoveFormCategory` | corbeille + AdminMessage | id absent de la liste | ✅ |
| `AssignFormCategory` | `modifyForm` `#id_category` | catégorie relue sélectionnée | ✅ |
| `AddListValue` | iframe `view_createField` | 1 id_field nouveau, « titre > valeur » | ✅ |
| `ModifyListValue` | iframe `modifyField` | même ensemble d'ids, nouveau libellé | ✅ |
| `RemoveListValue` | corbeille + AdminMessage `removeField` | seule la valeur visée disparaît | ✅ |
| `ModifyGroup` | iframe `modifyGroup` | même id_display, nouveau titre, aucun groupe créé/supprimé | ✅ |
| `RemoveGroup` | corbeille + AdminMessage `removeComposite` | groupe absent, ses questions aussi, les autres intactes | ✅ |
| `MoveQuestion` | assistant `moveComposite` (3 temps, POST) | question dans l'étape d'arrivée avec le parent attendu, absente de l'étape de départ | ✅ |
| `OpenFormResponseBO` | lien « Ajouter des données en BO » | saisie BO du formulaire, étape initiale | ✅ |
| `FillFieldBO` | délègue à `FillFieldFO` après marqueur BO | valeur relue | ✅ |
| `GoToNextStepBO` | délègue à `GoToNextStepFO` | `id_step` = étape cible, écran BO | ✅ |
| `ValidateResponseBO` | récapitulatif + validation | saisie vierge de l'étape initiale ; + `VerifyResponseRecorded` | ✅ |
| `SubmitResponsesFOBatch` | N parcours FO (`-Dforms.volume.fo`) | chaque parcours abouti ; + `VerifyResponseRecorded(N)` | ✅ (3, 20) |
| `SubmitResponsesBOBatch` | N saisies BO (`-Dforms.volume.bo`) | idem | ✅ (3, 20, 60) |
| `RenameStep` | paramètres de l'étape `#modify_step` | même id, nouveau titre, autres étapes inchangées | ✅ |
| `DuplicateFormFromList` | menu Actions, POST `duplicateForm` | 1 formulaire nouveau (différence d'ids), titre dérivé | ✅ |
| `VerifyFormStructureEqual` | derrière copie/import | signature `structureOf` identique : étapes (rang, badges) ; composants (questions et groupes dans l'ordre, groupe parent, type, obligation, valeurs de liste, itérations, conditions d'affichage résolues par id dans le formulaire lu) ; transitions (priorité, arrivée par rang, lue sur la vue graphique) ; « hors formulaire » si une référence reste dans l'original | ✅ (2026-10-30) |
| `ExportFormJsonFile` | lien « Télécharger » `DoExportFormJson.jsp` | JSON valide contenant étapes et questions | ✅ |
| `ImportFormJsonFile` | offcanvas `#import-json`, `action_doImportJson` | 1 formulaire nouveau ; + `VerifyFormStructureEqual` | ✅ |
| `ProcessWorkflowAction` | détail réponse, POST `doProcessAction` | action historisée et retirée du détail ; état d'arrivée en multivue après indexation | ✅ |
| `AddDisplayCondition` (2026-11-06) | pilote sur une autre étape : `id_step` + `validateStep` avant la question ; condition relue sur sa fiche (étape, question, valeur par id, constat 10) | fiche `modifyConditionControl&id_control` | ✅ |
| `ChooseValueFO` | cocher une réponse (radio, case) par son libellé exact dans le bloc `.display_field_<id>_0` | entrée cochée | ✅ |
| `VerifyQuestionShownFO` | question de l'étape FO courante affichée ou rendue masquée (pilote possible à une étape précédente) | bloc présent, état visible/masqué, `id_step` = étape de la question | ✅ |
| `GoToSummaryFO` | complète `ViewSummaryFO` (skip) : `action_formResponseSummary` visible | bouton `action_doSaveResponseSummary`, plus de formulaire d'étape ; sinon échec nommant l'étape restée affichée | ✅ |
| `SetMaxResponses` (2026-11-09) | paramètre « Nombre maximum de réponses » (`#maxNumberResponse`, `action_modifyForm`, 0 = pas de limite) | « Formulaire modifié », valeur relue, titre inchangé | ✅ |
| `RefuseMaxResponsesReachedFO` | maximum atteint : saisie FO refusée | `SiteMessage.jsp`, pas de `#form-validate`, `.alert-content` = « Formulaire indisponible car le nombre de réponses est atteint » ; ce contrôle passe avant celui de l'état actif | ✅ |
| `MoveGroup` (2026-11-10) | groupe déplacé dans un autre groupe de son étape : bouton `#btn-move-<id_display>`, assistant `moveComposite` | `compositesOfStep` : seul le groupe change de parent ; standalone = structure imbriquée complète relue | ✅ |
| `RefuseInvalidValueFO` *(négative)* (2026-11-12) | saisie sur l'étape FO courante, bouton `action_doSaveStep` ou `action_formResponseSummary` | même `id_step`, champ `is-invalid` qui garde la valeur, message du contrôle dans le `.invalid-feedback` du bloc | ✅ |
| `GoToStepFO` (2026-11-12) | fil des étapes : bouton `action_doGoToStep` (valeur = rang, libellé = titre de l'étape ; pas de bouton « précédente ») | `id_step` de l'étape visée ; standalone : saisie de l'étape conservée | ✅ |
| `VerifyResponseRecorded` | derrière `ValidateSummaryFO` / saisie BO | ids `data-url…id_form_response` de la multivue, différence d'ensembles, nombre exact | ✅ |
| `VerifyResponseValues` | derrière `VerifyResponseRecorded` ; lien `data-url…view_form_response_details` de la multivue | détail BO de chacune des N dernières réponses : `li[displaycontrol] > .row` (titre `.col-sm-3`, valeur `.col-sm-9`), fil d'Ariane « n° id » ; chaque table attendue ↔ exactement une réponse (lots : valeurs `{i}` résolues) | ✅ (rouge prouvé sur une valeur fausse) |
| `SetQuestionMandatory` | `view=modifyQuestion`, `#mandatory`, « Appliquer » | interrupteur toujours coché au rechargement | ✅ |
| `RefuseUnsetLastFinalStep` *(négative)* | `#modify_step`, décocher `#final` | toast « aucune étape finale », badge « Finale » conservé | ✅ |
| `RefuseMissingMandatoryFO` *(négative)* | bouton de validation visible de l'étape FO | même `id_step`, champ `is-invalid`, « Ce champ est obligatoire. » | ✅ |
| `RefuseRemoveCompositeWithResponses` *(négative)* | corbeille `getConfirmRemoveComposite`, question ou groupe | message STOP, pas de confirmation, élément toujours listé | ✅ |
| `RefuseUnavailableFormFO` *(négative)* | URL FO du formulaire | `SiteMessage.jsp`, « Formulaire indisponible », pas de `#form-validate` | ✅ |
| `AddDisplayCondition` | offcanvas `manageConditionControl` → « Ajouter un contrôle » → pilote / « Valeur exacte » / valeur | `forms.info.control.created` ; la vue des conditions cite la pilote et « Valeur exacte » | ✅ |
| `VerifyConditionalRevealFO` | étape FO affichée | cible masquée → visible (valeur d'affichage) → masquée (autre valeur) | ✅ |
| `CreateFrontUserBO` / `RemoveFrontUserBO` | MyLutece database `CreateUser.jsp` / `RemoveUser.jsp` | usager retrouvé (puis absent) par la recherche par login | ✅ |
| `LoginFrontUserFO` / `LogoutFrontUserFO` | `DoMyLuteceLogin.jsp` (+ changement de mot de passe imposé) / `DoMyLuteceLogout.jsp` | lien de déconnexion présent / absent | ✅ |
| `SaveBackupFO` | `action_doSaveForBackup` | « Reprise de la sauvegarde », valeur réaffichée, « Supprimer l'avancement » | ✅ |
| `VerifyBackupFO` | réouverture `init=true` | valeur + message de reprise, ou champ vide sans message | ✅ |
| `RefuseImportInvalidJson` *(négative)* | `#btn-import-json`, fichier invalide | « Erreur lors de l'import du formulaire », pas d'« Internal error », aucun formulaire nouveau | ✅ texte non JSON ; ❌ `{}` (point C, vert avec le correctif v8 LUT-34429) |

Support : `macro/FormsNonRegSupport` (lectures d'état avec marqueur d'écran : `stepsOfForm`, `questionsOfStep`, `transitionsFrom`, `multiviewResponseIds` ; `linkHref`/`intParam` stricts ; `cleanup` = dépublier puis supprimer).
Datasets : `FormModifyDataSet`, `FormCategoryDataSet`, `ListValueDataSet`, `TransitionTargetsDataSet`, `TransitionModifyDataSet`, `GroupModifyDataSet`, `MoveQuestionDataSet`, `CompositeTargetDataSet`, `InvalidJsonDataSet`, `DisplayConditionDataSet`, `FrontUserDataSet`, `BackupValueDataSet`. Support (`frontUserId`, nettoyage de l'usager FO dans `cleanup`) : `questionPlacement`, `groupsOfStep`. Volume : `BatchSubmissionDataSet` (`{i}` dans les valeurs), moteur `forms/ResponseBatchEngine` (pas une brique). Contexte : `FormsContext.listValueIds` (ajout pur).
Contexte : `FormsContext.responseIds`, `categoryIds`, `duplicatedFormId` (ajouts purs).
Rejeu à une seconde date (§4-bis.7) : fait le 2026-11-12 (jeudi), cf. « Rejeu du 2026-11-12 » ci-dessous.

## Tickets LUT à ouvrir (validés sur upstream `develop` bd4a558f, aucun ticket existant au 2026-10-29)

Recherche Redmine « Plugin Forms » (titres, tickets fermés compris) sans résultat. **Ne pas créer sans accord.**

| # | Sujet proposé | Constat | Pistes / tickets liés |
|---|---|---|---|
| A | Modifying a transition: the current arrival step is not offered, the transition cannot be saved unchanged | constat 10 : `getTransitionTargetStepReferenceList` exclut toutes les cibles déjà liées, y compris celle de la transition modifiée ; liste vide quand toutes les autres étapes sont liées | origine probable #20276 (filtre anti-doublon appliqué aussi à la modification) ; #34282 (refus silencieux d'une liste vide) |
| B | Removing a form category used by forms is accepted and leaves the forms pointing to a deleted category | constat 12 : `doRemoveCategory` ne vérifie pas l'usage, `forms_form.id_category` orphelin | #34239, #30627 (messages), #26080 (Won't fix, catégorie jugée inutile → priorité à discuter) |
| C | Importing a JSON file without a `form` key shows an error page instead of the import error | constat 18 : NPE `FormJsonService.importForm:211` sur `develop` bd4a558f | **Port v8 prêt (2026-10-30)** : la PR v7 n'existe pas en v8. Ticket V8 dédié LUT-34429 (lié à LUT-34387), commit `LUT-34429: A failed form or step import or copy shows an error page in v8`, branche locale `LUT-34429` sur `upstream/develop`, à pousser depuis LUT PR Tracker. Le `@Transactional` JTA de v8 est gardé ; un échec est relancé en `AppException` après reset du cache ; `doImportJson`, `doDuplicateForm`, la copie et l'import d'étape et l'import de modèle l'attrapent. Banc corrigé : `{}` et l'import interrompu après insertion sont refusés proprement, aucun formulaire n'est laissé. |
| D | Copying a form keeps the original list value in a display condition: the condition never triggers in the copy (**LUT-34494**, correctif prêt, branche `LUT-34494`) | 2026-10-30, forms 4.0.2-SNAPSHOT local (bd4a558f + LUT-34429) : la question pilote est remappée, la valeur « Valeur exacte » garde l'id de la valeur de l'original (export JSON de la copie : `value` = id de l'original) ; en FO la cible reste masquée sur la copie, s'affiche sur l'original. Copie = export + import JSON : l'import d'un fichier a le même défaut. Cause probable : `updateControlWithNewQuestion` ne remappe que si `validator instanceof ICustomImportableControl`, or le validateur est un proxy CDI (`ValidatorProducer`, `@Produces @ApplicationScoped` de type `IValidator`) | à confirmer en v7 (Spring, pas de proxy) ; aucune recherche Redmine faite |

## Briques existantes rejouées sur bd4a558f (2026-10-29), non modifiées

Vertes : `ModifyQuestion`, `RemoveQuestion`, `RemoveStep`, `RemoveTransition`, `AddQuestion{Text,Textarea,Number,Date,Radio,Checkbox,Select}`,
`OpenMultiview`, `OpenResponseDetail`, `ExportResponses`, `SetStepInitial`, `SetStepFinal`, `OpenFormFO`, `ViewSummaryFO`,
`ValidateSummaryFO`, `VerifyResponseSubmitted`, `RemoveForm`, `PublishForm`, `CreateGroup`, workflow (`Create`, `AddState`, `AddAction`, `Activate`).

| Brique existante | Résultat | Cause | Remplacée par |
|---|---|---|---|
| `ModifyStep` | ❌ | renommage enregistré, mais l'oracle `getByText` sur la carte ne le voit pas | `RenameStep` |
| `DuplicateForm` | skip | cherche un lien GET, la liste poste un formulaire (LUT-34208) | `DuplicateFormFromList` |
| `ExportFormJson` | skip | attend `action=doExportJson`, le téléchargement est `export/DoExportFormJson.jsp` | `ExportFormJsonFile` |
| `ImportFormJson` | skip | aucun fichier fourni par défaut | `ImportFormJsonFile` (rejoue l'export) |
| `AssociateWorkflow` | skip en standalone | `#idWorkflow` n'est rendu que si un workflow activé existe ; vert en suite | — |
| `RunWorkflowActionOnResponse` | skip | cherche un lien `doProcessAction`, c'est un formulaire POST (LUT-34208) | `ProcessWorkflowAction` |
| `NextStepFO` | skip | libellé sans accent | `GoToNextStepFO` |
| `MoveQuestionIntoGroup`, `VerifyGroupHierarchy` | skip | GET `action=moveComposite` sans token | `MoveQuestion` |
| `CreateTransition` | vert mais faux | cible déjà liée automatiquement → autre cible ou rien, sans erreur | `VerifyTransitionTargets` derrière |

Constat : après une action de workflow, l'état n'apparaît dans la multivue qu'après indexation (index Lucene) ;
le détail de la réponse, lui, est à jour immédiatement.

## Suites de non-régression (passe du 2026-10-29, banc bd4a558f) — 8/8 vertes (6 en 9 min 13 s, + refus 82 s, + FO dynamique 157 s)

| Suite | Scénario | Durée |
|---|---|---|
| `FormsNonRegStructureSuite` | catégorie → formulaire → 3 étapes liées → 7 types de questions → valeurs de liste → duplication/modif de question → groupe + déplacement → transition modifiée → suppressions inverses | 106 s |
| `FormsNonRegCopySuite` | duplication question/étape/formulaire, export JSON → import, structures comparées | 101 s |
| `FormsNonRegFrontOfficeSuite` | publication, parcours FO 2 étapes, réponse en multivue, dépublication | 61 s |
| `FormsNonRegBackOfficeEntrySuite` | 2 saisies agent en BO sur 2 étapes, chacune en multivue | 60 s |
| `FormsNonRegVolumeSuite` | 20 FO + 20 BO (`-Dforms.volume.fo/bo`), comptes exacts, export CSV | 145 s |
| `FormsNonRegResponsesSuite` | workflow activé et associé, réponse FO, action « Traiter » → état « Traite » | 81 s |
| `FormsNonRegDynamicFOSuite` | contrôle de validation (regex Email), condition Oui/Non, enfants en groupe répétable (ajout/retrait d'itération), brouillon d'un usager FO (sauvegarde, absent déconnecté, restauré reconnecté) | 157 s |
| `FormsNonRegNegativeSuite` | dernière étape finale, obligatoire vide en FO, suppression question puis groupe avec réponse, formulaire au maximum de réponses (limite 1, refus, limite levée et formulaire rouvert), formulaire dépublié, import non JSON — chaque refus sans effet | 82 s |
| `FormsNonRegCrossStepConditionSuite` | étape 1 « Votre statut ? » (radio obligatoire, Contractuel/Fonctionnaire/Autre), étape 2 « Préciser Autre » (texte obligatoire) affichée si Autre ; 1er lancement Autre (affichée, remplie, validée, relue en BO), 2e lancement Fonctionnaire (masquée, récapitulatif sans erreur attendu) | ✅ (2026-11-09, `develop`) ; ❌ sur bd4a558f (2026-11-06 : 2e lancement bloqué sur l'étape 2, sans message, LUT-34462), corrigé upstream par LUT-34348 (`a20bf5bb`, visibilité serveur avec les réponses des autres étapes) |
| `FormsNonRegCrossStepControlSuite` (2026-11-12) | même formulaire + contrôle de validation sur « Préciser Autre » (regex « Email », « Saisie invalide ») ; 1er lancement Autre (saisie invalide refusée sous le champ, valide acceptée, relue en BO), 2e lancement Fonctionnaire (masquée, récapitulatif, relue sans précision), 3e lancement changement d'avis (Autre, « @@@ » à l'étape 2, retour à l'étape 1 par le fil des étapes, Fonctionnaire) | ❌ défaut F = LUT-34074 non fusionné sur `develop` (PR #677 ouverte ; 2026-11-12, `develop` af1d2196) : 3e lancement bloqué sur l'étape 2, « Saisie invalide » sur le champ masqué, aucun message visible ; lancements 1 et 2 verts |

Agrégateur (9 suites depuis le 2026-11-06) : `bo/testsuites/FormsNonRegressionSuite` (`-Dtest=fr.paris.lutece.e2e.tests.bo.testsuites.FormsNonRegressionSuite`).
Agrégateur complet rejoué d'une traite le 2026-10-30 : 8/8, 0 skip, 572 s (banc avec le correctif LUT-34429) ; rejoué après correction des constats 1 et 2 (skip interdit, valeurs relues) : 8/8, 0 skip, 579 s ; après correction des
constats 3, 4 et 5 : 8/8 + 5 standalones touchés, 0 skip, 645 s pour l'agrégateur (suite Copie enrichie : 116 s). Suite
Copie, standalones `VerifyFormStructureEqual`, `ProcessWorkflowAction` et suite Réponses rejoués verts après la seconde revue.
Agrégateur complet le 2026-11-09 (lundi, `develop` af1d2196 = upstream 84afcdbd + LUT-34491/34494/34500) : 9/9, 0 skip, 826 s.
Après correction des constats 6 à 9 (2026-11-04, mardi, banc relancé sur base neuve) : agrégateur 8/8, 0 skip, 573 s ;
12 standalones touchés et 2 sondes de preuve verts.
Correctif D (2026-11-06, banc reconstruit, core 8.0.2-SNAPSHOT du 2026-11-04) : cas D vert, agrégateur 8/8, standalones de copie,
duplication d'étape, import, condition et révélation FO verts. Instabilité non expliquée : à la 1re passe après le redémarrage
du banc, les deux cas remontés d'import (LUT-34429) ont échoué, le message d'erreur porté par la redirection
(`fr.paris.lutece.redirect.param.ScopeId`) n'étant pas affiché (chemin d'erreur bien pris d'après les logs, aucun formulaire
laissé) ; non reproduit en 4 rejeux (seuls ×2, après le cas D, enchaînement exact). Reproduit à la 1re passe après
le redémarrage suivant (develop rebasé sur 53151b74), vert au rejeu sur le même banc.
**Cause trouvée (2026-11-06), pas le core** : `manage_forms.html` (v8, refonte LUT-29997) ne rend `<@messages errors infos>` que
dans `<#if form_list?size gt 0>` ; la branche « aucun formulaire » n'affiche que `@empty`. Sur une base neuve (aucun
formulaire), tout message d'erreur ou d'information après redirection vers la liste est perdu (3 imports sur 3 dans la
même session après démarrage à froid ; `ScopeId` et session corrects). En v7, `<@messages>` est rendu hors condition.
Défaut E = **LUT-34500** (2026-11-09) : `<@messages>` sorti de la condition, prouvé sur base neuve (cas remontés LUT-34429 verts
sans aucun formulaire, suite des refus verte) ; branche `LUT-34500` à pousser. LUT-34429 intégré upstream (`84afcdbd`).

Revue adversariale du 2026-10-30 (lecture seule) : les briques neuves respectent les règles de preuve, mais les suites
ne sont pas encore un filet fiable. Constats vérifiés, à corriger :
1. ✅ corrigé le 2026-10-30 — (haute) Les suites composent des briques historiques à `Assumptions` (`VerifyValidationErrorFO`, `ValidateSummaryFO`,
   `AssociateWorkflow`, `ExportResponses`, `ModifyQuestion`, `RemoveQuestion`, `RemoveStep`, `RemoveForm`) : une
   régression sur ces écrans rend la suite entière « skipped », pas rouge. Correction : les 8 suites portent
   `@ExtendWith(SkipIsFailureExtension.class)` (package `suites`) : toute `TestAbortedException` devient un échec
   (« skip interdit »), prouvé rouge par une sonde ; briques historiques inchangées.
2. ✅ corrigé le 2026-10-30 — (haute) `VerifyResponseRecorded` compte les ids nouveaux de la multivue sans relire
   les valeurs saisies. Correction : brique `VerifyResponseValues` + `ResponseValuesDataSet` derrière chaque
   `VerifyResponseRecorded` des suites FO, saisie BO, volume (lots FO et BO, valeur par valeur), réponses, refus.
3. ✅ corrigé le 2026-10-30 — (moyenne) `structureOf` compare les transitions par titre d'étape (aveugle à un remappage
   perdu vers l'original) et ignore valeurs de liste, types de question et groupes. Correction (`FormsNonRegSupport`) :
   - transitions lues sur la vue graphique `ManageSteps.jsp?view=graphicView` (source Mermaid, scripts neutralisés par
     `page.route`) : seul écran qui donne l'id de l'étape d'arrivée ; arrivée par rang dans le formulaire lu, priorité,
     contrôles ; recoupées par id avec la liste des transitions de chaque étape ;
   - composants lus par `compositesOfStep` : champ caché `own_display_id_composite` apparié au contenu qui le suit
     (le gabarit referme mal son balisage après un groupe ; l'ancien lecteur prenait un groupe qui suit un groupe pour
     une question sans titre, `questionsOfStep` et `questionPlacement` en héritaient : corrigés) ;
   - fiche question (type, obligation, valeurs de liste), fiche groupe (itérations), fiche de chaque condition
     d'affichage (étape, question et valeur pilotes par id, option `selected`).
   Preuve : sonde sur deux formulaires aux titres identiques, ancienne signature égale, nouvelle différente (type,
   valeur de liste, groupe). La lecture des conditions a révélé le cas remonté D.
4. ✅ corrigé le 2026-10-30 — (moyenne) `VerifyTransitionExists` après `CreateTransition` ne prouve pas la création
   (liaison auto FORMS-546). Standalones `GoToNextStepFO` et `VerifyResponseRecorded` : la liaison auto A→B est vérifiée
   par `VerifyTransitionTargets` au lieu d'une création redondante ; standalone `VerifyTransitionExists` : A, B, C, absence
   de A→C prouvée, `CreateTransition(A, C)`, ensemble exact {B, C} ; javadoc de la brique corrigée.
5. ✅ corrigé le 2026-10-30 — (moyenne) Workflow jamais supprimé (`ProcessWorkflowAction`, `FormsNonRegResponsesSuite`).
   Correction : `FormsNonRegSupport.cleanup(forms, wf)` supprime le workflow (`RemoveWorkflow`, inchangée) seulement si
   tous les formulaires ont été supprimés, et seulement si le workflow d'id `wf.workflowId` porte exactement
   `wf.workflowName` (l'id vient d'une recherche par sous-chaîne). Suppression vérifiée dans Allure.
6. ✅ corrigé le 2026-11-04 — (moyenne) Catégorie laissée si la suite Structure échoue avant `RemoveFormCategory`
   (cleanup ignore `categoryIds`). Correction : `FormsNonRegSupport.cleanup` supprime les catégories du contexte après
   les formulaires, et seulement si tous les formulaires ont été supprimés (une catégorie encore utilisée laisserait
   `forms_form.id_category` orphelin, défaut B). Prouvé par une sonde : échec simulé après création de la catégorie et
   du formulaire, catégorie absente de la liste après nettoyage.
7. ✅ corrigé le 2026-11-04 — (moyenne) Absence d'usager FO prouvée par « lien RemoveUser introuvable », sans marqueur
   d'écran. Correction (`frontUserId`) : après la recherche, le champ `search_login` doit garder le login cherché et la
   liste afficher son compteur `.paginator-item-count` ; un compteur à 0 exige l'état vide `.empty` (« Aucun résultat
   trouvé ! », relevé du 2026-11-04). Une page d'erreur ou de connexion fait échouer la lecture.
8. ✅ corrigé le 2026-11-04 — (moyenne) `UnsetStepFinal` sans précondition « était finale » en composition. Relevé
   du 2026-11-04 : la première étape est créée **initiale seulement** ; les trois appels `UnsetStepFinal(0)`
   (standalones `GoToNextStepFO`, `VerifyResponseRecorded`, `VerifyStepFlags`) ne faisaient rien. Correction : la
   brique asserte le badge « Finale » avant le retrait (rouge sinon, prouvé par une sonde) ; les deux premiers
   standalones prouvent l'état par `VerifyStepFlags(initialOnly(0))`, l'appel est retiré du troisième.
9. ✅ corrigé le 2026-11-04 — (moyenne) `DuplicateFormFromList` enregistre la copie pour le nettoyage après les
   assertions (`ImportFormJsonFile` aussi). Correction : `FormsNonRegSupport.registerCreatedForms` enregistre les
   formulaires nouveaux de ce run (titre porteur de `ctx.formTitle`, jamais ceux d'un autre run) avant toute assertion.
10-15. ✅ corrigés le 2026-11-06 (10) et le 2026-11-09 (11 à 15) — (basse) :
   - 10. `AddDisplayCondition` sans relecture de la valeur → condition relue sur sa fiche (étape, question, valeur
     pilotes par id) ;
   - 11. relu faible de `ProcessWorkflowAction` → marqueur d'écran du détail (`#form-response-details`, fil d'Ariane
     « n° <id> ») avant toute absence de bouton, et une entrée « Action <libellé> » de plus dans l'historique ;
   - 12. relu faible de `ValidateResponseBO` → récapitulatif disparu, aucune `.alert-danger`, étape initiale, saisie
     vierge prouvée sur au moins un champ (texte vide, aucune case cochée) ; pas de message de confirmation en BO :
     l'enregistrement reste prouvé par `VerifyResponseRecorded` + `VerifyResponseValues` ;
   - 13. `RefuseUnavailableFormFO` accepte aussi « … car le nombre de réponses est atteint » → message exact
     (`getByText` exact) et absence du message `forms.error.form.MaxResponse` ; prouvé rouge sur un formulaire au maximum
     atteint (sonde du 2026-11-09) ;
   - 14. `LogoutFrontUserFO` reconstruit l'URL → clic sur le lien de déconnexion de la page (menu ouvert si besoin) ;
   - 15. `ModifyQuestion` (suivie, non modifiée) cible par texte → `VerifyQuestionsListed` composée derrière dans la
     suite Structure : l'id renommé porte le nouveau titre, les autres titres sont inchangés.
   Rejoués verts le 2026-11-09 : suites Structure, saisie BO, réponses, refus, FO dynamique et les 4 standalones.

Seconde revue adversariale du 2026-10-30 (corrections 3 à 5, lecture seule, constats vérifiés) :
- ✅ (moyenne) condition d'affichage comparée par sa seule présence → lue sur sa fiche (cf. 3) ;
- ✅ (basse) `cleanup(forms, wf)` pouvait supprimer un workflow d'un autre run (id par sous-chaîne du nom) → garde
  sur le nom exact ;
- ouvert (basse) : un workflow enregistré dont `CreateWorkflow` n'a pas résolu l'id (liste paginée à 50, triée par nom
  décroissant, page 1 seule) reste en place ; l'échec du test est rouge, seul le nettoyage manque ;
- ✅ corrigé le 2026-11-10 (basse) : groupes imbriqués, le gabarit referme jusqu'à `#question-list` ; `compositesOfStep` échoue alors
  bruyamment (« sans contenu unique »), jamais en faux vert ; aucune suite n'imbrique de groupes. **Vérifié sur le banc le
  2026-11-10** (G1 { G2 { Qa }, Q3, Q4, Q5 }, G2 déplacé dans G1 par l'assistant réel, bouton `#btn-move-<id_display>` pour
  un groupe) : champs cachés tous dans la liste avec les bons parents, mais le `h4` de Q5 sort de `#question-list` ;
  `compositesOfStep` échoue sur « Composant de l'etape … sans contenu unique : {display=…, contents=[]} ». Défaut
  d'affichage candidat côté forms (`manage_questions.html`, logique de fermeture `idCurrentGroup`). Correction :
  `compositesOfStep` apparie dans tout le document à partir du premier champ caché (titres `h4` portant `.searchable` et
  cartes de groupe seulement), un contenu manquant ou en trop reste un échec ; garde-fou permanent : standalone de la
  nouvelle brique `MoveGroup`, qui construit ce cas et compare toute la structure relue ;
- hors périmètre (briques suivies, non modifiées) : `RunWorkflowActionOnResponse`, `DemandeSubventionSuite`,
  `FormWithWorkflowSuite`, `MultiviewResponsesSuite` laissent aussi leur workflow ; contrôles de validation (`+controle`)
  encore comparés par leur seule présence.

Refus non couverts : suppression d'une question portant une condition (le nombre maximal de réponses atteint est
couvert depuis le 2026-11-09).

### Rejeu du 2026-11-12 (jeudi, seconde date §4-bis.7) — 13/13 verts, 0 skip, 844 s

Banc relancé à neuf (lutecepowers 3.2.13, base neuve), forms `develop` af1d2196 = upstream 84afcdbd (inchangé au
2026-11-12) + LUT-34491, LUT-34494 (D), LUT-34500 (E), PR #701-#703 ouvertes ; correctifs D et E vérifiés dans le jar
et le gabarit déployés. Lancement unique : agrégateur `FormsNonRegressionSuite` (9 suites), 3 cas remontés
(`regression.forms.*RegressionTest`) et standalone `MoveGroup` ; rapport Allure `mvn -o allure:report`.

Nettoyage (étapes en échec dans des tests verts, visibles dans Allure seulement) :
- ✅ corrigé le 2026-11-12 (moyenne) : `FormsNonRegDynamicFOSuite` laissait son formulaire et son usager FO. `LogoutFrontUserFO` passe par
  `DoMyLuteceLogout.jsp`, qui invalide toute la session HTTP, admin comprise (vérifié : `ManageForms` répond ensuite
  302 vers `AdminMessage.jsp`) ; le nettoyage, en fin de suite, tombe hors session admin (`frontUserId` en délai
  dépassé sur `#offcanvasSearch`, dépublication et suppression sans écran). Correction :
  `FormsNonRegSupport.ensureAdminSession` (menu d'administration, sinon reconnexion par `LoginPage.loginAs`) en tête
  de `removeForms` ;
- ✅ corrigé le 2026-11-12 (basse, fausse alerte) : `RemoveForm` (brique suivie) vérifie l'absence par `MacroSupport.extractFormId`,
  qui retient le premier titre qui *contient* le titre cherché ; quand un formulaire est supprimé avant un autre du
  même run dont le titre le contient (copie « Copie de … », import), l'assertion échoue alors que la suppression a eu lieu, et l'élément
  suivant est supprimé normalement (suite Copie, cas D ; base vérifiée : seul le formulaire de la suite FO dynamique
  reste). L'ordre de suppression ne suffit pas (copie et import peuvent porter le même titre). Correction : le
  nettoyage supprime par `FormsNonRegSupport.removeFormById` (lien `view=confirmRemoveForm` de la ligne, bouton OK du
  formulaire `action=removeForm&id_form=N` de l'AdminMessage, absence prouvée par l'id dans `formIds`) ; `RemoveForm`
  reste la brique des suites qui testent la suppression.

Rejeu des cas touchés (2026-11-12) : suites FO dynamique et Copie, cas D — 3/3 verts, aucune étape non passée dans
Allure, nettoyage complet (base relue : seuls restent le formulaire et l'usager du rejeu d'avant le correctif).

### Défaut F (2026-11-12) — contrôle de validation appliqué à une question masquée

Forms `develop` af1d2196 (upstream 84afcdbd), même code sur `develop_core7`. `FormsResponseUtils.fillResponseManagerWithResponses` :
la 1re boucle ne valide que les questions visibles (`question.isVisible() && bValidateQuestionStep`), la 2nde applique les
contrôles `VALIDATION` à toutes les réponses de l'étape sans regarder la visibilité (ajoutée par `48b44728`, #24991,
2023-02-14). Une saisie conservée sur une question devenue masquée (retour à l'étape précédente, changement de réponse
pilote) bloque l'étape sans message visible ; vide, elle passe (le validateur regex ignore une valeur vide). Régression de
#20304 (fermé : « a non shown conditional field … with a validation, like regexp, you can't validate the form »).
**Couvert par LUT-34074** (revérifié le 2026-11-12) : son correctif `dd77198d` ajoute exactement la garde
`bValidateQuestionStep && formQuestionResponse.getQuestion().isVisible()` dans la 2nde boucle (plus le contexte des étapes
précédentes pour la visibilité). Fusionné seulement sur `develop_4.0.2-beta-04.b` (PR #678, statut Redmine « Delivered ») ;
PR #677 (`develop`) et #676 (`develop_core7`) ouvertes, non fusionnées. Pas de nouveau ticket : la suite repassera au vert
quand #677 sera fusionnée et le banc reconstruit. Preuve (2026-11-12) : `develop` af1d2196 + la seule garde `isVisible()`
de #677 (2nde boucle, sans commit, fichier restauré après), banc reconstruit → `FormsNonRegCrossStepControlSuite` verte,
47/47 étapes Allure, 3e lancement compris (récapitulatif atteint, réponse relue sans la saisie abandonnée). La 1re partie
de #677 (contexte des étapes précédentes) recoupe LUT-34348 déjà sur `develop` : GitHub la marque `CONFLICTING`, seule la
garde est utile. Rejeu complet sur ce banc : `FormsNonRegressionSuite` 10/10 verts, 0 skip, 665 s, 366 étapes Allure toutes
vertes. Briques : `RefuseInvalidValueFO`,
`GoToStepFO` ; `GoToSummaryFO` liste désormais les champs en erreur, masqués compris ; `VerifyResponseValues` accepte des
questions sans valeur (`ResponseValuesDataSet.without`).

### Adaptation à forms 4.1.0 / core 8.1.0 (2026-11-17)

Au 2026-11-17, les SNAPSHOT tirent core 8.1.0 (ranges) et `module-workflow-forms` 3.1.0 exige forms 4.1.0 :
forms 4.0.2 n'est plus assemblable sur le banc, et casse en BO sur core 8.1 (`htmlAttributes: params must be a map`
dans `manage_forms.html` / `create_form.html`). Banc construit sur upstream `develop` 021e18f2 (4.1.0-SNAPSHOT, sans la
garde de #677) depuis le worktree `components/lutece-form-plugin-forms-demo`. Premier rejeu : 8/10 rouges, tous côté
tests. Adaptations (les deux balisages acceptés, relevés sur l'instance) :

| Écart 4.1.0 | Origine | Brique adaptée |
|---|---|---|
| détail BO d'une réponse : `li[data-display-control]` au lieu de `li[displaycontrol]` | LUT-33387 / LUT-33153 | `VerifyResponseValues` |
| fiche d'une question : badge de type sous `h2.card-title` (et non `h3`) | macros core 8.1 | `FormsNonRegSupport.questionDetail` |
| import JSON : modale `#import-json` ouverte par `button[data-bs-target]`, plus d'offcanvas `#btn-import-json` | LUT-34136 | `ImportFormJsonFile`, `RefuseImportInvalidJson` |
| fil d'étapes FO : texte `.visually-hidden` « (étape N sur M, terminée) » dans le bouton | LUT-34135 | `GoToStepFO` |
| onglets `@tabLink` vers une JSP sans `role="tab"` | LUT-33387 | Page Objects + `ModifyStep`, `VerifyResponseSubmitted`, `AddTaskToAction` : patch repris du clone `lutece-e2e-autoescape` (helper `AdminTabs`) |

Résultat : 7/10 au rejeu complet après les deux premières adaptations, puis les 3 suites restantes et les tests
autonomes des briques adaptées rejoués : tout vert sauf `FormsNonRegCrossStepControlSuite`, rouge sur le défaut F
(attendu sans #677). En 4.1.0, l'usager voit désormais un résumé d'erreurs en haut de l'étape (« Préciser Autre : Saisie
invalide ») mais reste bloqué sur une question qu'il ne peut pas afficher.
Rejeu complet de contrôle le 2026-11-17 : `FormsNonRegressionSuite` 9/10 verts, 0 skip, 651 s, 363 étapes Allure ;
seule `FormsNonRegCrossStepControlSuite` est rouge (défaut F, LUT-34074 / PR #677 non fusionnée).
Portage sur upstream `lutece-platform/lutece-e2e` `develop` 9412fdf (historique sans base commune avec le fork) :
même résultat, 9/10 verts en 741 s, 363 étapes, suite 10 rouge sur le défaut F. Collisions résolues au profit d'upstream
(`UnsetStepFinal`, `OpenResponseDetail`, `RunWorkflowActionOnResponse`, `ModifyStep`, modale « add task » d'`AddTaskToAction`) ;
`AdminTabs` ajouté. Témoin upstream seul : `FormsCreationTest` et `WorkflowCreationTest` tombent sur les onglets,
`AddTaskToAction` en skip ; avec `AdminTabs`, `FormsCreationTest` 9/9 et `AddTaskToAction` vert.

## Cas remontés (`tests.regression.forms`, `@Tag("regression")`, une classe par cas, briques seules)

| Classe | Symptôme | Brique(s) | Ticket | Statut (bd4a558f) |
|---|---|---|---|---|
| `FormsImportJsonWithoutFormRegressionTest` | import de `{}` → « Internal error » (NPE) | `RefuseImportInvalidJson` + `InvalidJsonDataSet.withoutForm()` | C, LUT-34429 | ❌ sur `develop` bd4a558f ; ✅ avec le correctif v8 |
| `FormsCopyDisplayConditionRegressionTest` | copie d'un formulaire : la condition « Valeur exacte » garde la valeur de l'original, la cible ne s'affiche plus en FO | `AddDisplayCondition`, `DuplicateFormFromList`, `VerifyFormStructureEqual` | D, LUT-34494 (branche à pousser) | ❌ sur `develop` (bd4a558f, code inchangé sur 53151b74) ; ✅ avec le correctif local (2026-11-06) : `getNewValidatorValue` en méthode par défaut de `IValidator`, appelée sans `instanceof` |
| `FormsImportJsonAtomicityRegressionTest` | import `{"form":{…}}` sans `formMessage` : formulaire inséré puis NPE | `RefuseImportInvalidJson` + `InvalidJsonDataSet.formWithoutMessage()` | C, LUT-34429 (atomicité) | ❌ sur `develop` bd4a558f ; ✅ avec le correctif v8 |
