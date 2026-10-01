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
#   ALLURE_CLEAN       1 = purge les resultats en attente avant l'envoi (defaut : 1)
#   ALLURE_EXEC_NAME   Libelle de l'execution dans le rapport (defaut : job Jenkins ou branche git)
#   ALLURE_EXEC_FROM   Lien source de l'execution (defaut : BUILD_URL Jenkins)
#   ALLURE_EXEC_TYPE   Type d'execution : jenkins, github, gitlab... (defaut : jenkins)
#   ALLURE_INSECURE    1 = ignore la verification TLS (defaut : 1, CA interne Ville de Paris)
#   ALLURE_NO_PROXY    1 = bypass le proxy HTTP (defaut : 1, le serveur est interne)
#   ALLURE_BATCH_BYTES Taille maximale d'un lot d'envoi en octets (defaut : 900000)
#   ALLURE_MAX_FILE_BYTES Taille maximale d'un fichier isole (defaut : 900000)
#
# Projet cible : un projet Allure par site teste, identifie par son artifactId, pour que
# l'historique et les tendances d'un site ne soient pas melanges a ceux d'un autre.
# L'artifactId est resolu dans l'ordre : ALLURE_PROJECT_ID, ALLURE_SITE_ARTIFACT_ID, le
# pom.properties du war embarque dans l'image (ALLURE_SITE_IMAGE), l'URL cible
# (ALLURE_SITE_URL), puis le lutece.base.url du microprofile-config des tests. Si rien ne
# le determine, le script echoue plutot que de publier sous un nom approximatif.
#
# Limite d'envoi : l'ingress nginx devant le service applique client_max_body_size 1m.
# Au-dela, la requete est rejetee en 413 avant d'atteindre Allure. Les lots sont donc
# plafonnes et les fichiers plus gros que la limite sont ecartes avec un avertissement :
# en pratique seules les traces Playwright (target/traces, plusieurs Mo) sont concernees.
# Pour les publier, faire relever proxy-body-size sur la route par l'equipe infra puis
# surcharger ALLURE_BATCH_BYTES / ALLURE_MAX_FILE_BYTES.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "${SCRIPT_DIR}")"

SERVER_URL="${ALLURE_SERVER_URL:-https://<serveur-allure>/allure-docker-service}"
RESULTS_DIR="${1:-${ALLURE_RESULTS_DIR:-${MODULE_DIR}/target/allure-results}}"
CLEAN="${ALLURE_CLEAN:-1}"
EXEC_TYPE="${ALLURE_EXEC_TYPE:-jenkins}"
BATCH_BYTES="${ALLURE_BATCH_BYTES:-900000}"
MAX_FILE_BYTES="${ALLURE_MAX_FILE_BYTES:-900000}"

SERVER_URL="${SERVER_URL%/}"

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

PROJECT_ID="$(resolve_project_id)"
if [ -z "${PROJECT_ID}" ]; then
    # Publier sous un nom approximatif melangerait les tendances de deux sites :
    # mieux vaut exiger l'artifactId que deviner.
    echo "ERREUR : artifactId du site teste indeterminable." >&2
    echo "  Renseigner ALLURE_SITE_ARTIFACT_ID (ou le parametre SITE_ARTIFACT_ID du job)." >&2
    exit 1
fi

[ -d "${RESULTS_DIR}" ] || { echo "ERREUR : repertoire de resultats introuvable : ${RESULTS_DIR}" >&2; exit 1; }

mapfile -t ALL_FILES < <(find "${RESULTS_DIR}" -maxdepth 1 -type f ! -name '.*' | sort)
[ "${#ALL_FILES[@]}" -gt 0 ] || { echo "ERREUR : aucun resultat Allure dans ${RESULTS_DIR}" >&2; exit 1; }

# Ecarte les fichiers que l'ingress rejetterait en 413 (traces Playwright volumineuses)
FILES=()
SKIPPED=()
for f in "${ALL_FILES[@]}"; do
    if [ "$(stat -c %s "${f}")" -gt "${MAX_FILE_BYTES}" ]; then
        SKIPPED+=("${f}")
    else
        FILES+=("${f}")
    fi
done
[ "${#FILES[@]}" -gt 0 ] || { echo "ERREUR : tous les fichiers depassent ${MAX_FILE_BYTES} octets" >&2; exit 1; }

echo "Serveur   : ${SERVER_URL}"
echo "Projet    : ${PROJECT_ID}"
echo "Resultats : ${RESULTS_DIR} (${#FILES[@]} fichiers)"
echo "Execution : ${EXEC_NAME}"
if [ "${#SKIPPED[@]}" -gt 0 ]; then
    echo "ATTENTION : ${#SKIPPED[@]} fichiers ecartes (> ${MAX_FILE_BYTES} octets, limite ingress) :"
    for f in "${SKIPPED[@]}"; do
        echo "  $(basename "${f}") ($(stat -c %s "${f}") o)"
    done
fi

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

# 3. Envoi par lots : les traces Playwright pesent plusieurs Mo, un envoi unique
#    depasserait la limite de taille de la route OpenShift.
batch=()
batch_count=0
batch_size=0
sent=0
flush_batch() {
    [ "${batch_count}" -gt 0 ] || return 0
    api POST "/send-results?project_id=${PROJECT_ID}" "${batch[@]}" > /dev/null
    sent=$(( sent + batch_count ))
    echo "  ${sent}/${#FILES[@]} fichiers envoyes"
    batch=()
    batch_count=0
    batch_size=0
}

echo "Envoi des resultats..."
for f in "${FILES[@]}"; do
    size=$(stat -c %s "${f}")
    if [ "${batch_count}" -gt 0 ] && [ $(( batch_size + size )) -gt "${BATCH_BYTES}" ]; then
        flush_batch
    fi
    batch+=(-F "files[]=@${f}")
    batch_count=$(( batch_count + 1 ))
    batch_size=$(( batch_size + size ))
done
flush_batch

# 4. Generation du rapport
echo "Generation du rapport..."
GEN_URL="/generate-report?project_id=${PROJECT_ID}&execution_name=$(urlencode "${EXEC_NAME}")&execution_type=${EXEC_TYPE}"
[ -n "${EXEC_FROM}" ] && GEN_URL+="&execution_from=$(urlencode "${EXEC_FROM}")"
api GET "${GEN_URL}" > /dev/null

echo
echo "Rapport publie :"
echo "  ${SERVER_URL}/projects/${PROJECT_ID}/reports/latest/index.html"
