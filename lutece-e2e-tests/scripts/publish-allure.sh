#!/usr/bin/env bash
#
# Publie les resultats Allure sur un serveur Allure Docker Service.
#
# Usage :
#   ./scripts/publish-allure.sh [repertoire-resultats]
#
# Variables d'environnement :
#   ALLURE_SERVER_URL  URL du service (defaut : serveur PIC Lutece P30)
#   ALLURE_PROJECT_ID  Projet cible, cree s'il n'existe pas (defaut : artifactId deduit)
#   ALLURE_SITE_ARTIFACT_ID artifactId du site teste, quand il n'est pas deductible
#   ALLURE_SITE_IMAGE  Image du site teste : l'artifactId est lu dans le war qu'elle
#                      embarque (mode conteneur)
#   ALLURE_SITE_URL    URL du site teste, sert a deduire l'artifactId (mode instance existante)
#   ALLURE_CONTAINER_RUNTIME  Runtime a utiliser pour lire l'image (defaut : podman, sinon docker)
#   ALLURE_RESULTS_DIR Repertoire des resultats (defaut : target/allure-results)
#   ALLURE_UI_URL      URL de l'IHM Allure (defaut : ALLURE_SERVER_URL suffixe de -ui)
#   ALLURE_CLEAN       1 = purge les resultats en attente avant l'envoi (defaut : 1)
#   ALLURE_EXEC_NAME   Libelle du build, porte par le rapport (defaut : job Jenkins ou
#                      branche git)
#   ALLURE_EXEC_FROM   Lien vers le build, porte par le rapport (defaut : BUILD_URL)
#   ALLURE_EXEC_TYPE   Type d'execution : jenkins, github, gitlab... (defaut : jenkins)
#   ALLURE_INSECURE    1 = ignore la verification TLS (defaut : 1, CA interne Ville de Paris)
#   ALLURE_NO_PROXY    1 = bypass le proxy HTTP (defaut : 1, le serveur est interne)
#   ALLURE_BATCH_BYTES Taille de lot visee au depart, en octets (defaut : 32000000)
#
# Projet cible : un projet Allure par site teste, identifie par son artifactId, pour que
# l'historique et les tendances d'un site ne soient pas melanges a ceux d'un autre.
# L'artifactId est resolu dans l'ordre : ALLURE_PROJECT_ID, ALLURE_SITE_ARTIFACT_ID, le
# pom.properties du war embarque dans l'image (ALLURE_SITE_IMAGE), l'URL cible
# (ALLURE_SITE_URL), puis le lutece.base.url du microprofile-config des tests. Si rien ne
# le determine, le script echoue plutot que de publier sous un nom approximatif.
#
# Limite d'envoi : la route devant le service plafonne la taille des requetes (nginx
# client_max_body_size, 1 Mo a l'origine) et rejette le surplus en 413 avant qu'Allure
# ne le voie. Le script ne suppose pas cette limite, il la decouvre : un lot refuse est
# scinde, et un fichier seul refuse est ecarte avec un avertissement. Tout relevement de
# proxy-body-size est donc pris en compte sans modification ici.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "${SCRIPT_DIR}")"

SERVER_URL="${ALLURE_SERVER_URL:-https://<serveur-allure>/allure-docker-service}"
SERVER_URL="${SERVER_URL%/}"
UI_URL="${ALLURE_UI_URL:-${SERVER_URL}-ui}"
UI_URL="${UI_URL%/}"
RESULTS_DIR="${1:-${ALLURE_RESULTS_DIR:-${MODULE_DIR}/target/allure-results}}"
# Purge des resultats en attente avant l'envoi : sans danger puisque le script genere le
# rapport dans la foulee, donc rien ne reste en attente d'un run a l'autre. Elle evite
# qu'un envoi interrompu laisse des resultats qui se melangeraient au run suivant.
CLEAN="${ALLURE_CLEAN:-1}"
EXEC_TYPE="${ALLURE_EXEC_TYPE:-jenkins}"
# Taille de lot visee au depart : volontairement large, pour tirer parti d'une route
# genereuse. Si elle refuse, la limite reelle est decouverte a l'envoi (cf. send_files).
BATCH_BYTES="${ALLURE_BATCH_BYTES:-32000000}"

CURL_OPTS=(--silent --show-error --fail-with-body --max-time 300)
[ "${ALLURE_INSECURE:-1}" = "1" ] && CURL_OPTS+=(--insecure)
# Le no_proxy du poste utilise la syntaxe `*.mdp` que curl ne comprend pas : on force le bypass.
[ "${ALLURE_NO_PROXY:-1}" = "1" ] && CURL_OPTS+=(--noproxy '*')

# Libelle d'execution : parametre, sinon contexte Jenkins, sinon branche git locale
if [ -n "${ALLURE_EXEC_NAME:-}" ]; then
    EXEC_NAME="${ALLURE_EXEC_NAME}"
elif [ -n "${JOB_NAME:-}" ]; then
    EXEC_NAME="${JOB_NAME} #${BUILD_NUMBER:-?}"
else
    EXEC_NAME="local $(git -C "${MODULE_DIR}" rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
fi
EXEC_FROM="${ALLURE_EXEC_FROM:-${BUILD_URL:-}}"

api() {
    local method="$1" path="$2"
    shift 2
    curl "${CURL_OPTS[@]}" -X "${method}" "$@" "${SERVER_URL}${path}"
}

urlencode() {
    local s="$1" out="" c i
    for (( i=0; i<${#s}; i++ )); do
        c="${s:i:1}"
        case "${c}" in
            [a-zA-Z0-9.~_-]) out+="${c}" ;;
            *) out+="$(printf '%%%02X' "'${c}")" ;;
        esac
    done
    printf '%s' "${out}"
}

# Normalise un identifiant de projet Allure : minuscules, [a-z0-9-] uniquement
normalize_id() {
    printf '%s' "$1" | tr '[:upper:]' '[:lower:]' \
        | sed -e 's/[^a-z0-9-]\+/-/g' -e 's/^-\+//' -e 's/-\+$//'
}

# Source canonique de l'artifactId : le pom.properties genere par Maven dans le war que
# l'image embarque, sous apps/expanded/<war>/META-INF/maven/<groupId>/<artifactId>/.
# Ni le war (renomme lutece.war) ni le context root (/lutece) ne portent l'artifactId.
# L'image ne contient ni unzip ni jar ni python : on ne lit que le war deja deplie, ce
# qui est le cas des images produites par la chaine de build des sites.
artifact_id_from_image_war() {
    local image="$1" runtime=""
    for c in ${ALLURE_CONTAINER_RUNTIME:-} podman docker; do
        if command -v "${c}" > /dev/null 2>&1; then runtime="${c}"; break; fi
    done
    [ -n "${runtime}" ] || return 0
    timeout 120 "${runtime}" run --rm --entrypoint sh "${image}" -c '
        p=$(find /opt/ol/wlp/usr/servers/*/apps /opt/ibm/wlp/usr/servers/*/apps \
                 -path "*/META-INF/maven/*/pom.properties" ! -path "*/WEB-INF/*" \
                 2>/dev/null | head -1)
        [ -n "${p}" ] && sed -n "s/^artifactId=//p" "${p}"
    ' 2>/dev/null | tr -d '\r' | head -1
}

# Repli quand le war est illisible : l'image est taguee d'apres l'artifactId du site
# (nexus.../bild/f98/site-deontologie:1.0.0-SNAPSHOT -> site-deontologie)
artifact_id_from_image_tag() {
    local ref="${1##*/}"
    printf '%s' "${ref%%:*}"
}

# Le context root d'un site Lutece reprend son artifactId
# (https://host:port/site-deontologie -> site-deontologie), sauf quand le
# deploiement utilise un root generique qui, lui, n'identifie aucun site.
artifact_id_from_url() {
    local url="${1#*://}"
    local host="${url%%/*}"
    local root=""
    if [ "${url}" != "${host}" ]; then
        local rest="${url#*/}"
        root="${rest%%/*}"
    fi
    case "${root}" in
        ''|lutece|site|portal) printf '%s' '' ;;
        *) printf '%s' "${root}" ;;
    esac
}

# Un projet Allure par site teste, identifie par son artifactId : fourni
# explicitement, sinon deduit de l'image, de l'URL cible, ou du lutece.base.url
# des tests (execution locale).
resolve_project_id() {
    local id=""
    if [ -n "${ALLURE_PROJECT_ID:-}" ]; then
        id="${ALLURE_PROJECT_ID}"
    elif [ -n "${ALLURE_SITE_ARTIFACT_ID:-}" ]; then
        id="${ALLURE_SITE_ARTIFACT_ID}"
    elif [ -n "${ALLURE_SITE_IMAGE:-}" ]; then
        id="$(artifact_id_from_image_war "${ALLURE_SITE_IMAGE}")"
        if [ -z "${id}" ]; then
            id="$(artifact_id_from_image_tag "${ALLURE_SITE_IMAGE}")"
            echo "ATTENTION : pom.properties illisible dans l'image, artifactId deduit du tag." >&2
        fi
    elif [ -n "${ALLURE_SITE_URL:-}" ]; then
        id="$(artifact_id_from_url "${ALLURE_SITE_URL}")"
    else
        local cfg="${MODULE_DIR}/src/test/resources/META-INF/microprofile-config.properties"
        local base_url
        base_url="$(sed -n 's/^lutece\.base\.url=//p' "${cfg}" 2>/dev/null | tail -1)"
        [ -n "${base_url}" ] && id="$(artifact_id_from_url "${base_url}")"
    fi
    printf '%s' "$(normalize_id "${id}")"
}

PROJECT_ID="$(resolve_project_id)"
if [ -z "${PROJECT_ID}" ]; then
    # Publier sous un nom approximatif melangerait les tendances de deux sites :
    # mieux vaut exiger l'artifactId que deviner.
    echo "ERREUR : artifactId du site teste indeterminable." >&2
    echo "  Renseigner ALLURE_SITE_ARTIFACT_ID (ou le parametre SITE_ARTIFACT_ID du job)." >&2
    exit 1
fi

[ -d "${RESULTS_DIR}" ] || { echo "ERREUR : repertoire de resultats introuvable : ${RESULTS_DIR}" >&2; exit 1; }

# Contexte du run, affiche par Allure dans son widget Environment. Complete l'executor
# du rapport (build et lien, cf. generate-report) avec ce qu'il ne porte pas : l'image
# ou l'URL reellement testee. Y redire le build sert de filet : une generation depuis
# l'IHM reecrit l'executor et perd le lien Jenkins, le widget Environment le conserve.
# On n'ecrit pas d'executor.json, reecrit lui aussi a la generation.
# Toujours reecrit : dans un target/ reutilise, le fichier du run precedent survivrait
# et le rapport afficherait le mauvais build.
{
    echo "Site=${PROJECT_ID}"
    echo "Build=${EXEC_NAME}"
    [ -n "${EXEC_FROM}" ] && echo "BuildUrl=${EXEC_FROM}"
    [ -n "${ALLURE_SITE_IMAGE:-}" ] && echo "Image=${ALLURE_SITE_IMAGE}"
    [ -n "${ALLURE_SITE_URL:-}" ] && echo "TargetUrl=${ALLURE_SITE_URL}"
    true
} > "${RESULTS_DIR}/environment.properties"

mapfile -t ALL_FILES < <(find "${RESULTS_DIR}" -maxdepth 1 -type f ! -name '.*' | sort)
[ "${#ALL_FILES[@]}" -gt 0 ] || { echo "ERREUR : aucun resultat Allure dans ${RESULTS_DIR}" >&2; exit 1; }

FILES=("${ALL_FILES[@]}")
SKIPPED=()

echo "Serveur   : ${SERVER_URL}"
echo "Projet    : ${PROJECT_ID}"
echo "Resultats : ${RESULTS_DIR} (${#FILES[@]} fichiers)"
echo "Execution : ${EXEC_NAME}"

# 1. Creation du projet si absent
if api GET "/projects/${PROJECT_ID}" -o /dev/null 2>/dev/null; then
    echo "Projet deja present."
else
    echo "Creation du projet ${PROJECT_ID}..."
    api POST "/projects" -H 'Content-Type: application/json' \
        -d "{\"id\":\"${PROJECT_ID}\"}" > /dev/null
fi

# 2. Purge des resultats en attente (l'historique des rapports generes est conserve)
if [ "${CLEAN}" = "1" ]; then
    echo "Purge des resultats en attente..."
    api GET "/clean-results?project_id=${PROJECT_ID}" > /dev/null
fi

# 3. Envoi par lots, en decouvrant la limite de la route plutot qu'en la supposant :
#    un lot refuse en 413 est scinde en deux et la limite courante est abaissee, de
#    sorte que les lots suivants la respectent sans nouvel aller-retour. Un fichier
#    seul refuse est ecarte. Le script suit ainsi tout relevement de proxy-body-size
#    sur la route sans modification : les traces Playwright passeront d'elles-memes.
sent=0
cur_limit="${BATCH_BYTES}"

post_files() {
    local args=() f code
    for f in "$@"; do args+=(-F "files[]=@${f}"); done
    # stderr masque : un 413 fait partie du fonctionnement normal (decouverte de la
    # limite), le code HTTP suffit a decider. Une vraie panne ressort en HTTP 000.
    code="$(curl "${CURL_OPTS[@]}" -o /dev/null -w '%{http_code}' \
        -X POST "${args[@]}" \
        "${SERVER_URL}/send-results?project_id=${PROJECT_ID}" 2>/dev/null || true)"
    printf '%s' "${code}"
}

send_files() {
    # f est local : la boucle d'envoi principale itere sur la meme variable et un
    # ecrasement ici lui ferait reempiler le mauvais fichier.
    local files=("$@") code total half f
    [ "${#files[@]}" -gt 0 ] || return 0
    code="$(post_files "${files[@]}")"
    case "${code}" in
        200|201)
            sent=$(( sent + ${#files[@]} ))
            echo "  ${sent}/${#FILES[@]} fichiers envoyes"
            return 0
            ;;
        413)
            if [ "${#files[@]}" -eq 1 ]; then
                SKIPPED+=("${files[0]}")
                return 0
            fi
            total=0
            for f in "${files[@]}"; do total=$(( total + $(stat -c %s "${f}") )); done
            [ "${total}" -lt "${cur_limit}" ] && cur_limit=$(( total / 2 + 1 ))
            half=$(( ${#files[@]} / 2 ))
            send_files "${files[@]:0:${half}}"
            send_files "${files[@]:${half}}"
            return 0
            ;;
        *)
            echo "ERREUR : envoi refuse par le serveur (HTTP ${code})" >&2
            return 1
            ;;
    esac
}

echo "Envoi des resultats..."
batch=()
batch_size=0
for f in "${FILES[@]}"; do
    size=$(stat -c %s "${f}")
    if [ "${#batch[@]}" -gt 0 ] && [ $(( batch_size + size )) -gt "${cur_limit}" ]; then
        send_files "${batch[@]}"
        batch=()
        batch_size=0
    fi
    batch+=("${f}")
    batch_size=$(( batch_size + size ))
done
[ "${#batch[@]}" -gt 0 ] && send_files "${batch[@]}"

if [ "${#SKIPPED[@]}" -gt 0 ]; then
    echo "ATTENTION : ${#SKIPPED[@]} fichiers refuses par la route (trop volumineux) :"
    for f in "${SKIPPED[@]}"; do
        echo "  $(basename "${f}") ($(stat -c %s "${f}") o)"
    done
    echo "  Relever proxy-body-size sur la route pour les publier."
fi

# 4. Generation du rapport : le service ne la declenche pas de lui-meme
#    (check_results_every_seconds=NONE), les resultats resteraient en attente.
#    C'est aussi cet appel qui ecrit l'executor du rapport : ses parametres donnent le
#    build d'origine et son lien, la ou une generation depuis l'IHM affiche seulement
#    "Allure Docker Service UI" sans lien.
echo "Generation du rapport..."
GEN_URL="/generate-report?project_id=${PROJECT_ID}&execution_name=$(urlencode "${EXEC_NAME}")&execution_type=${EXEC_TYPE}"
if [ -n "${EXEC_FROM}" ]; then
    GEN_URL+="&execution_from=$(urlencode "${EXEC_FROM}")"
fi
api GET "${GEN_URL}" > /dev/null

echo
echo "Rapport publie :"
echo "  ${UI_URL}/projects/${PROJECT_ID}"
