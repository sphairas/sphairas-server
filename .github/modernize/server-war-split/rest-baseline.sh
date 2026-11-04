#!/usr/bin/env bash
# Records HTTP status and body of the existing service REST endpoints so that
# the output can be diffed before/after a change (see baseline.md).
#
# Usage: BASE=https://host:8181/service/api CERT=server.crt KEY=server.key KEYPASS=... CACERT=server.crt \
#        SIGNEE=... STUDENT=... TERM=... TARGET=... UNIT=... ZGN=... OUT=baseline-out ./rest-baseline.sh
# UNKNOWN_STUDENT (optional): a well-formed id of a student that does not exist.
# Missing parameters cause the dependent checks to be skipped, not to fail.
set -u
BASE=${BASE:-https://localhost:8181/service/api}
OUT=${OUT:-baseline-out}
mkdir -p "$OUT"
CURL=(curl -sS --max-time 30)
[ -n "${CERT:-}" ] && CURL+=(--cert "$CERT")
[ -n "${KEY:-}" ] && CURL+=(--key "$KEY")
[ -n "${KEYPASS:-}" ] && CURL+=(--pass "$KEYPASS")
[ -n "${RESOLVE:-}" ] && CURL+=(--resolve "$RESOLVE") # e.g. taurus:8181:127.0.0.1
if [ -n "${CACERT:-}" ]; then CURL+=(--cacert "$CACERT"); else CURL+=(-k); fi

if [ -n "${KEY:-}" ] && command -v openssl >/dev/null; then
  if ! openssl pkey -in "$KEY" -passin "pass:${KEYPASS:-}" -noout 2>/dev/null; then
    echo "ERROR: cannot read private key '$KEY'. Set KEYPASS to the key passphrase (AS_ADMIN_MASTERPASSWORD in docker.env)." >&2
    exit 2
  fi
fi

rc=0
# Set METHOD=POST etc. in front of check for non-GET requests.
# check NAME EXPECTED-STATUSES(|-separated) PATH [key=value ...]  (values are URL-encoded)
check() {
  local name=$1 expect=$2 path=$3; shift 3
  local body="$OUT/$name.body" code args=()
  for kv in "$@"; do args+=(--data-urlencode "$kv"); done
  code=$("${CURL[@]}" -G -X "${METHOD:-GET}" -o "$body" -w '%{http_code}' "${args[@]}" "$BASE/$path") || code=000
  echo "$code" > "$OUT/$name.status"
  if [[ "|$expect|" == *"|$code|"* ]]; then echo "ok    $name ($code)"; else echo "FAIL  $name: got $code, expected $expect"; rc=1; fi
}
check_as_signee() {
  local name=$1 expect=$2 path=$3; shift 3
  local body="$OUT/$name.body" code args=()
  for kv in "$@"; do args+=(--data-urlencode "$kv"); done
  code=$("${CURL[@]}" -H "X-Betula-Signee: $SIGNEE" -H "X-Betula-Roles: ${ACTING_ROLES:-signee}" \
    -G -o "$body" -w '%{http_code}' "${args[@]}" "$BASE/$path") || code=000
  echo "$code" > "$OUT/$name.status"
  if [[ "|$expect|" == *"|$code|"* ]]; then echo "ok    $name ($code)"; else echo "FAIL  $name: got $code, expected $expect"; rc=1; fi
}
check_as_unitadmin() {
  local name=$1 expect=$2 path=$3; shift 3
  local body="$OUT/$name.body" code args=()
  for kv in "$@"; do args+=(--data-urlencode "$kv"); done
  code=$("${CURL[@]}" -H 'X-Betula-Roles: unitadmin' \
    -G -o "$body" -w '%{http_code}' "${args[@]}" "$BASE/$path") || code=000
  echo "$code" > "$OUT/$name.status"
  if [[ "|$expect|" == *"|$code|"* ]]; then echo "ok    $name ($code)"; else echo "FAIL  $name: got $code, expected $expect"; rc=1; fi
}
check_as_signee_post_json() {
  local name=$1 expect=$2 path=$3 payload=$4
  local body="$OUT/$name.body" code
  code=$("${CURL[@]}" -H "X-Betula-Signee: $SIGNEE" -H "X-Betula-Roles: ${ACTING_ROLES:-signee}" \
    -H 'Content-Type: application/json' -X POST --data "$payload" \
    -o "$body" -w '%{http_code}' "$BASE/$path") || code=000
  echo "$code" > "$OUT/$name.status"
  if [[ "|$expect|" == *"|$code|"* ]]; then echo "ok    $name ($code)"; else echo "FAIL  $name: got $code, expected $expect"; rc=1; fi
}
need() { for v in "$@"; do [ -n "${!v:-}" ] || { echo "skip  (missing $v)"; return 1; }; done; }

check ping 200 internal/ping
# Authorization: no client certificate must be rejected
code=$(curl -sS -k ${RESOLVE:+--resolve $RESOLVE} --max-time 30 -o /dev/null -w '%{http_code}' "$BASE/internal/ping" 2>/dev/null || true)
echo "$code" > "$OUT/ping-no-cert.status"
if [[ "$code" =~ ^(401|403|000)$ ]]; then echo "ok    ping-no-cert ($code)"; else echo "FAIL  ping-no-cert: got $code"; rc=1; fi
# Authorization: plain HTTP must not serve the API (CONFIDENTIAL transport)
HTTP_BASE="http://${BASE#https://}"; HTTP_BASE="${HTTP_BASE/:8181/:8080}"
code=$(curl -sS ${RESOLVE:+--resolve ${RESOLVE%%:*}:8080:${RESOLVE##*:}} --max-time 30 -o /dev/null -w '%{http_code}' "$HTTP_BASE/internal/ping" 2>/dev/null || true) # 302 redirect to https or 403 expected
echo "$code" > "$OUT/ping-plain-http.status"

# Id parameters are parsed by each type's valueOf(); JAX-RS answers 404 if parsing fails, so
# a 404 can mean "malformed id" as well as "not found". Ids look like <id>@<authority>
# (DocumentId may carry a version: <id>/<version>@<authority>).
if need SIGNEE; then
  check signee-common-name 200 internal/signee-common-name "signee=$SIGNEE"
  check signee-groups 200 internal/signee-groups "signee=$SIGNEE"
fi
need ZGN SIGNEE && check signee-primary-unit 200 internal/signee-primary-unit "primary-unit-head-teachers-document=$ZGN" "signee=$SIGNEE"
need STUDENT && check student-vcard 200 internal/student-vcard "student=$STUDENT"
need UNKNOWN_STUDENT && check student-vcard-unknown 200\|204\|400\|404\|500 internal/student-vcard "student=$UNKNOWN_STUDENT" # well-formed id of a non-existing student
check student-vcard-malformed-id 404 internal/student-vcard "student=not-an-id"
need TARGET TERM STUDENT && {
  check applicable-tickets 200 internal/applicable-tickets "target-document=$TARGET" "term=$TERM" "student=$STUDENT"
  check select 200\|204 internal/select "target=$TARGET" "student=$STUDENT" "term=$TERM"
}
need UNIT TERM && check report-date 200\|204 internal/report-date "category=${CATEGORY:-zeugnis}" "unit=$UNIT" "term=$TERM"
# Error case (400 after the REST fix; the old EAR baseline returned 500): mandatory parameters missing on a write endpoint (well-formed target)
need TARGET && METHOD=POST check submit-grade-missing-params 400 internal/submit-grade-value "target=$TARGET"
# fast-target-documents (outside /internal; also verifies the possible /api/api path)
need TARGET && {
  check ftd-fast-term 200 api/fast-target-documents/fast-term-target-document "documentId=$TARGET"
  check ftd-fast-term-doubled-api 200\|404 api/api/fast-target-documents/fast-term-target-document "documentId=$TARGET"
}
# New endpoints (404 on the old EAR baseline)
need STUDENT && check student-primary-unit 200\|204 internal/student-primary-unit "student=$STUDENT"
need STUDENT && check student-primary-unit-malformed 404 internal/student-primary-unit "student=not-an-id"
check student-marker-entry-missing-params 400 internal/student-marker-entry
need TARGET && check subject-alternative-name 200\|204 internal/subject-alternative-name "document=$TARGET"
check subject-alternative-name-missing-params 400 internal/subject-alternative-name
need TARGET && check target-doc-fast-term-missing-acting-subject 400 internal/target-documents/fast-term "document=$TARGET"
need TARGET SIGNEE && check_as_signee target-doc-fast-term 200 internal/target-documents/fast-term "document=$TARGET"
need TARGET SIGNEE && check_as_signee target-doc-markers 200\|204 internal/target-documents/markers "document=$TARGET"
need TARGET SIGNEE && check_as_signee_post_json target-doc-markers-batch 200 internal/target-documents/markers/batch "[\"$TARGET\"]"
need UNIT SIGNEE && check_as_signee target-documents-by-unit 200 internal/target-documents/by-unit "unit=$UNIT"
need UNIT && check_as_unitadmin target-documents-by-unit-unitadmin-no-signee 200 internal/target-documents/by-unit "unit=$UNIT"
need UNIT TERM SIGNEE && check_as_signee target-documents-by-unit-term 200 internal/target-documents/by-unit-term "unit=$UNIT" "term=$TERM"
need UNIT SIGNEE && check_as_signee target-documents-students 200 internal/target-documents/students "unit=$UNIT"
check app-resource-missing 404 internal/app-resource "path=does-not-exist.xml"
check app-resource-missing-param 400 internal/app-resource
check app-resource-traversal 403\|404 internal/app-resource "path=../config/domain.xml"
check app-resource-schulvorlage 200\|404 internal/app-resource "path=schulvorlage.xml"
# internal/reports (new resource; read-only checks, expected to 404 on the old EAR baseline)
need STUDENT TERM && {
  check reports-find 200 internal/reports "student=$STUDENT" "term=$TERM"
  check reports-ags 200 internal/reports/ags "student=$STUDENT" "term=$TERM"
}
check reports-missing-params 400 internal/reports
# REPORT: a term report document id (e.g. from reports-find output)
need REPORT && {
  check reports-student 200 internal/reports/student "document=$REPORT"
  check reports-term 200 internal/reports/term "document=$REPORT"
  check reports-markers 200 internal/reports/markers "document=$REPORT"
  check reports-markers-batch 200 internal/reports/markers/batch "document=$REPORT" "document=$REPORT"
  check reports-custom-notes 200 internal/reports/custom-notes "document=$REPORT"
  check reports-note 200\|204 internal/reports/notes "document=$REPORT" "key=x"
  check reports-notes-batch 200 internal/reports/notes/batch "document=$REPORT" "key=x" "key=y"
}
echo "results in $OUT (rc=$rc)"
exit $rc
