#!/bin/zsh
set -eu

duration_seconds="${1:-300}"
requests_per_second="${2:-60}"
concurrency="${3:-20}"
base_hour="${4:-18}"

start_epoch=$(date +%s)
ok=0
fail=0

for sec in $(seq 0 $((duration_seconds - 1))); do
  minute=$((sec / 60))
  second=$((sec % 60))
  batch_file="/tmp/shaft_batch_${sec}.txt"
  : > "${batch_file}"

  for i in $(seq 1 "${requests_per_second}"); do
    part=$((i % 2 + 1))
    ts=$(printf "2026-03-24T%02d:%02d:%02d" "${base_hour}" "${minute}" "${second}")
    printf "%s|%s|%s\n" "${i}" "${part}" "${ts}" >> "${batch_file}"
  done

  batch_result=$(
    xargs -P "${concurrency}" -n 1 -I {} /bin/zsh -lc '
      IFS="|" read req_idx part ts <<< "$1"
      curl -sS -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/reports/measure \
        -X POST \
        -H "Content-Type: application/json" \
        -d "{\"topic\":\"/TMP/shaft-dev-01_TMP_th0${part}/Measure\",\"IedFullPath\":\"/IED/shaft-dev-01\",\"dataReference\":\"/TMP/shaft-dev-01_TMP_th0${part}\",\"MaxTemp\":80.0,\"MinTemp\":74.0,\"AvgTemp\":77.0,\"MaxTempPosition\":11.0,\"MinTempPosition\":3.0,\"MaxTempChannel\":1,\"MinTempChannel\":1,\"timestamp\":\"${ts}\"}"
    ' _ {} < "${batch_file}" | awk '/^200$/ { ok += 1 } END { print ok + 0 }'
  )

  ok=$((ok + batch_result))
  fail=$((fail + requests_per_second - batch_result))

  elapsed=$(( $(date +%s) - start_epoch ))
  target=$((sec + 1))
  if [ "${elapsed}" -lt "${target}" ]; then
    sleep $((target - elapsed))
  fi

  rm -f "${batch_file}"
done

end_epoch=$(date +%s)
elapsed=$((end_epoch - start_epoch))
rate=$(awk "BEGIN { if (${elapsed} == 0) print 0; else printf \"%.2f\", ${ok} / ${elapsed} }")

printf "ok=%s fail=%s duration=%s rate=%s\n" "${ok}" "${fail}" "${elapsed}" "${rate}"
