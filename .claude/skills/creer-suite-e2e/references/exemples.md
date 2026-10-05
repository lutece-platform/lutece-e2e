# Trois exemples, de structures opposées

Dans `lutece-e2e-tests/src/test/resources/suites/` du dépôt `lutece-e2e`, branche
`feature/suites-declaratives`. Les trois ont été exécutés contre une instance réelle.

Lire celui dont la forme ressemble le plus à la démarche du site — **pas pour le recopier**, mais
pour voir comment une forme se décrit.

| Fichier | Forme | Ce qu'il montre |
|---|---|---|
| `deontologie.e2e-suite.yml` | **linéaire**, 8 étapes | 7 rubriques partageant le même motif : une question fermée commande des questions de détail dans un groupe répétable. Affichage conditionnel à l'échelle, instruction. 150 étapes vertes |
| `subvention.e2e-suite.yml` | **à embranchement** | liaison conditionnée et sortie par défaut, organisation unittree, options, validation de saisie, deux branches soumises et vérifiées. 111 étapes vertes |
| `inscription.e2e-suite.yml` | **sans workflow** | démarche de simple collecte : les sections sont facultatives, le formulaire se publie sans workflow à associer. Catalogue de types de question peu employés ailleurs. 49 étapes vertes |

## Le motif qui revient le plus souvent

Une rubrique déclarative — « avez-vous quelque chose à déclarer ? », et le détail n'apparaît que
si oui :

```yaml
- titre: Activités professionnelles
  groupes:
    - titre: Activités rémunérées
      iterations: 10
  questions:
    - type: radio
      titre: "Avez-vous des activités professionnelles ?"
      choix: [Oui, Non]
    - type: texte
      titre: Nom de l'employeur
      groupe: Activités rémunérées
      affichee-si:
        question: "Avez-vous des activités professionnelles ?"
        vaut: Oui
```

Et le parcours qui l'éprouve des deux côtés — la face masquée se vérifie aussi bien que l'autre,
et c'est le parcours le plus fréquent :

```yaml
parcours:
  - soumission:
      nom: Rien à déclarer
      reponses:
        "Avez-vous des activités professionnelles ?": Non
      verifier:
        - question: Nom de l'employeur
          masquee: true
```

## L'embranchement

```yaml
  enchainement:
    - de: Identité
      vers: Dossier de subvention
      si:
        question: Nature de la demande
        vaut: Subvention
    - de: Identité                    # sortie par défaut, en second
      vers: Demande d'information
    - de: Dossier de subvention
      vers: Confirmation
    - de: Demande d'information
      vers: Confirmation
```

Ce qui le prouve, dans le parcours :

```yaml
      verifier:
        - etape: Dossier de subvention
        - etape: Demande d'information
          ecartee: true
```

Sans le second contrôle, une condition inopérante passerait inaperçue : le parcours suivrait
simplement la première liaison.
