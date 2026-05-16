#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${GATEWAY_BASE_URL:-http://localhost:8088}"

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: command not found: $cmd" >&2
    exit 1
  fi
}

require_cmd curl
require_cmd jq

if ! curl -fsS "${BASE_URL}/v3/api-docs" >/dev/null 2>&1; then
  echo "ERROR: gateway is not ready at ${BASE_URL}" >&2
  exit 1
fi

ts="$(date +%s)"
campaign_type="E2E_${ts}"
long_url="https://example.com/promo/${ts}"

creator_username="gw_creator_${ts}"
approver1_username="gw_approver1_${ts}"
approver2_username="gw_approver2_${ts}"

json_post() {
  local url="$1"
  local body="$2"
  curl -sS -X POST "${url}" -H 'Content-Type: application/json' -d "${body}"
}

json_put() {
  local url="$1"
  local body="$2"
  curl -sS -X PUT "${url}" -H 'Content-Type: application/json' -d "${body}"
}

assert_success() {
  local label="$1"
  local json="$2"
  local code
  code="$(echo "${json}" | jq -r '.code')"
  if [[ "${code}" != "200" ]]; then
    echo "ERROR: ${label} failed"
    echo "${json}" | jq .
    exit 2
  fi
}

echo "== create users =="
creator_resp="$(json_post "${BASE_URL}/api/users" "{\"username\":\"${creator_username}\",\"password\":\"123456\",\"email\":\"${creator_username}@test.com\",\"phone\":\"13800138000\",\"role\":\"USER\"}")"
approver1_resp="$(json_post "${BASE_URL}/api/users" "{\"username\":\"${approver1_username}\",\"password\":\"123456\",\"email\":\"${approver1_username}@test.com\",\"phone\":\"13800138001\",\"role\":\"APPROVER\"}")"
approver2_resp="$(json_post "${BASE_URL}/api/users" "{\"username\":\"${approver2_username}\",\"password\":\"123456\",\"email\":\"${approver2_username}@test.com\",\"phone\":\"13800138002\",\"role\":\"APPROVER\"}")"
assert_success "create creator user" "${creator_resp}"
assert_success "create approver1 user" "${approver1_resp}"
assert_success "create approver2 user" "${approver2_resp}"

creator_id="$(echo "${creator_resp}" | jq -r '.data')"
approver1_id="$(echo "${approver1_resp}" | jq -r '.data')"
approver2_id="$(echo "${approver2_resp}" | jq -r '.data')"

echo "== config approvers =="
cfg1_resp="$(json_post "${BASE_URL}/api/approvers" "{\"campaignType\":\"${campaign_type}\",\"approverId\":${approver1_id},\"approverLevel\":1}")"
cfg2_resp="$(json_post "${BASE_URL}/api/approvers" "{\"campaignType\":\"${campaign_type}\",\"approverId\":${approver2_id},\"approverLevel\":2}")"
assert_success "config level1 approver" "${cfg1_resp}"
assert_success "config level2 approver" "${cfg2_resp}"

echo "== create campaign =="
campaign_resp="$(json_post "${BASE_URL}/api/campaigns" "{\"name\":\"gw-campaign-${ts}\",\"description\":\"gateway e2e\",\"campaignType\":\"${campaign_type}\",\"creatorUserId\":${creator_id},\"startTime\":\"2026-05-16T00:00:00+08:00\",\"endTime\":\"2026-05-20T00:00:00+08:00\",\"budget\":199.99,\"longUrl\":\"${long_url}\"}")"
assert_success "create campaign" "${campaign_resp}"
campaign_id="$(echo "${campaign_resp}" | jq -r '.data')"

echo "== submit campaign =="
submit_resp="$(json_post "${BASE_URL}/api/campaigns/${campaign_id}/submit" "{}")"
assert_success "submit campaign" "${submit_resp}"

poll_task() {
  local approver_id="$1"
  local task_id=""
  local process_instance_id=""
  local pending_resp=""

  for _ in $(seq 1 25); do
    pending_resp="$(json_post "${BASE_URL}/api/workflows/tasks/pending/query" "{\"approverId\":${approver_id},\"businessType\":\"CAMPAIGN_APPROVAL\",\"campaignType\":\"${campaign_type}\",\"pageNum\":1,\"pageSize\":10}")"
    assert_success "query pending tasks for approver ${approver_id}" "${pending_resp}"
    task_id="$(echo "${pending_resp}" | jq -r '.data.records[0].taskId // empty')"
    process_instance_id="$(echo "${pending_resp}" | jq -r '.data.records[0].processInstanceId // empty')"
    if [[ -n "${task_id}" && -n "${process_instance_id}" ]]; then
      break
    fi
    sleep 1
  done

  if [[ -z "${task_id}" || -z "${process_instance_id}" ]]; then
    echo "ERROR: no pending task found for approver ${approver_id}"
    echo "${pending_resp}" | jq .
    exit 3
  fi

  echo "${process_instance_id}|${task_id}"
}

echo "== approve level1 =="
pair1="$(poll_task "${approver1_id}")"
process_instance_id="${pair1%%|*}"
task1_id="${pair1##*|}"
approve1_resp="$(json_post "${BASE_URL}/api/workflows/approve" "{\"processInstanceId\":\"${process_instance_id}\",\"taskId\":\"${task1_id}\",\"approverId\":${approver1_id},\"approverName\":\"${approver1_username}\",\"comment\":\"approve-l1\"}")"
assert_success "approve level1" "${approve1_resp}"

echo "== approve level2 =="
pair2="$(poll_task "${approver2_id}")"
task2_id="${pair2##*|}"
approve2_resp="$(json_post "${BASE_URL}/api/workflows/approve" "{\"processInstanceId\":\"${process_instance_id}\",\"taskId\":\"${task2_id}\",\"approverId\":${approver2_id},\"approverName\":\"${approver2_username}\",\"comment\":\"approve-l2\"}")"
assert_success "approve level2" "${approve2_resp}"

echo "== poll campaign status =="
campaign_status=""
campaign_get_resp=""
for _ in $(seq 1 25); do
  campaign_get_resp="$(curl -sS "${BASE_URL}/api/campaigns/${campaign_id}")"
  assert_success "get campaign by id" "${campaign_get_resp}"
  campaign_status="$(echo "${campaign_get_resp}" | jq -r '.data.status // empty')"
  if [[ "${campaign_status}" == "APPROVED" ]]; then
    break
  fi
  sleep 1
done

if [[ "${campaign_status}" != "APPROVED" ]]; then
  echo "ERROR: campaign status is not APPROVED, current=${campaign_status}"
  echo "${campaign_get_resp}" | jq .
  exit 4
fi

echo "== create short link =="
short_link_resp="$(json_post "${BASE_URL}/api/short-links" "{\"longUrl\":\"${long_url}\"}")"
assert_success "create short link" "${short_link_resp}"
short_code="$(echo "${short_link_resp}" | jq -r '.data.shortCode // empty')"
if [[ -z "${short_code}" ]]; then
  echo "ERROR: shortCode is empty"
  echo "${short_link_resp}" | jq .
  exit 5
fi

echo "== bind short code =="
bind_resp="$(json_put "${BASE_URL}/api/campaigns/${campaign_id}/short-code" "{\"shortCode\":\"${short_code}\"}")"
assert_success "bind short code" "${bind_resp}"

echo "== resolve short code =="
resolve_resp="$(curl -sS "${BASE_URL}/api/short-links/${short_code}/url")"
assert_success "resolve short code" "${resolve_resp}"
resolved_url="$(echo "${resolve_resp}" | jq -r '.data')"
if [[ "${resolved_url}" != "${long_url}" ]]; then
  echo "ERROR: resolved url mismatch"
  echo "expected=${long_url}"
  echo "actual=${resolved_url}"
  exit 6
fi

echo "== E2E SUCCESS =="
echo "campaign_type=${campaign_type}"
echo "creator_id=${creator_id}"
echo "approver1_id=${approver1_id}"
echo "approver2_id=${approver2_id}"
echo "campaign_id=${campaign_id}"
echo "process_instance_id=${process_instance_id}"
echo "level1_task_id=${task1_id}"
echo "level2_task_id=${task2_id}"
echo "short_code=${short_code}"
echo "resolved_url=${resolved_url}"
