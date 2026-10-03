## Browser Automation

Use `agent-browser` for web automation. Run `agent-browser --help` for all commands.

Core workflow:
1. `agent-browser open <url>` - Navigate to page
2. `agent-browser snapshot -i` - Get interactive elements with refs (@e1, @e2)
3. `agent-browser click @e1` / `fill @e2 "text"` - Interact using refs
4. Re-snapshot after page changes

## Travail sur les tests — Allure Agent Mode

Pour tout travail sur les tests de ce depot, suivre [Allure Agent Mode](docs/allure-agent-mode.md).

Une execution de tests est un instrument, pas un verdict a gratter dans la console. Passee par
`allure agent`, elle devient un compte rendu relisible de ce qui s'est produit — etapes, preuves,
pieces jointes — assorti de constats sur les tests eux-memes, par exemple un test qui s'est execute
sans rien verifier. On declare avec `--goal` ce que l'execution doit etablir ; le rapport porte ce
but a cote des preuves, de sorte qu'un humain ou un autre agent puisse le valider.

- Lancer les commandes de test dont le resultat compte sous la forme
  `npx allure agent --goal "<ce que l'execution doit etablir>" -- <commande de test>`, puis lire la
  sortie agent (`index.md`, les constats, les preuves) plutot que la fin de la console.
- En cas d'echec : lire l'echec et ses preuves dans la sortie, corriger sur place ; confirmer une
  instabilite en rejouant les seuls tests tombes sous mode agent. Pour un echec de CI, examiner les
  `allure-results` telecharges avec `npx allure agent inspect` au lieu de reproduire localement.
- Pour deboguer, lancer le test concerne sous mode agent : il met deja l'environnement en place et
  capture les preuves, la ou un script jetable ne garde rien.
- Avant d'ecrire un test ou de le modifier substantiellement, invoquer le skill `$allure-agent-mode`
  et lire ses regles de conception ; s'il n'est pas installe, suivre `docs/allure-agent-mode.md`.
- `docs/allure-agent-mode.md` porte les commandes locales, les profils d'execution et les
  conventions du depot.

L'execution affiche un resume sur la sortie standard — le lire, puis ouvrir la sortie agent. La
reduire avec `tail`, `grep` ou `>/dev/null`, ou s'arreter aux compteurs, jette les constats et les
preuves.

Attention : le POM global configure Surefire avec `testFailureIgnore=true`. `BUILD SUCCESS` ne
signifie pas que les tests passent.
