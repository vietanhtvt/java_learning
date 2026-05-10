#!/bin/bash
# Chạy script này trong thư mục java_learning đã clone về:
#   git clone https://github.com/vietanhtvt/java_learning.git
#   cd java_learning
#   bash document/setup_local.sh   ← nếu đã có file này
#
# Hoặc chạy thẳng:
#   bash <(curl -s https://raw.githubusercontent.com/vietanhtvt/java_learning/master/document/setup_local.sh)

set -e

echo "Tạo cấu trúc thư mục claude-routines..."
mkdir -p document/claude-routines

# ── taskflow_status.py ──────────────────────────────────────────────────────
cat > document/claude-routines/taskflow_status.py << 'PYEOF'
import json, datetime, os, sys

f = os.path.expanduser('~/.claude/taskflow_progress.json')
if not os.path.exists(f):
    sys.exit(0)

with open(f) as fh:
    d = json.load(fh)

today = datetime.date.today().isoformat()
deadline = datetime.date.fromisoformat(d['deadline'])
days_left = (deadline - datetime.date.today()).days

if days_left < 0:
    sys.exit(0)

sessions = d.get('sessions', {})
done = [k for k, v in sessions.items() if v['status'] == 'done']
pending = [k for k, v in sessions.items() if v['status'] == 'pending' and v['date'] <= today]

print()
print('╔══════════════════════════════════════════╗')
print('║      TASKFLOW PROJECT COORDINATOR        ║')
print('╠══════════════════════════════════════════╣')
print(f'║  Hom nay : {today:<31}║')
print(f'║  Deadline: {d["deadline"]:<31}║')
print(f'║  Con lai : {days_left} ngay{" "*(26-len(str(days_left)))}║')
print(f'║  Da xong : {len(done)}/8 sessions{" "*(21-len(str(len(done))))}║')
if pending:
    s = pending[0]
    sv = sessions[s]
    title_short = sv['title'][:24]
    print(f'║  Tiep theo: {s} - {title_short:<22}║')
else:
    print(f'║  Tiep theo: Tat ca sessions da hoan thanh!║')
print('╚══════════════════════════════════════════╝')
print('  Go /coordinate de nhan task cu the')
print()
PYEOF

# ── taskflow_progress.json ──────────────────────────────────────────────────
cat > document/claude-routines/taskflow_progress.json << 'JSONEOF'
{
  "project": "TaskFlow API",
  "start_date": "2026-05-10",
  "deadline": "2026-05-14",
  "sessions": {
    "S1": {
      "date": "2026-05-10",
      "time": "morning",
      "title": "Project bootstrap, entities, Flyway",
      "status": "pending",
      "completed_tasks": []
    },
    "S2": {
      "date": "2026-05-10",
      "time": "afternoon",
      "title": "Task/Comment domain, CRUD APIs",
      "status": "pending",
      "completed_tasks": []
    },
    "S3": {
      "date": "2026-05-11",
      "time": "morning",
      "title": "Spring Security + JWT",
      "status": "pending",
      "completed_tasks": []
    },
    "S4": {
      "date": "2026-05-11",
      "time": "afternoon",
      "title": "Exception handling, Validation, Pagination",
      "status": "pending",
      "completed_tasks": []
    },
    "S5": {
      "date": "2026-05-12",
      "time": "morning",
      "title": "Redis Cache + AOP Logging",
      "status": "pending",
      "completed_tasks": []
    },
    "S6": {
      "date": "2026-05-12",
      "time": "afternoon",
      "title": "Kafka Events + Notifications + Virtual Threads",
      "status": "pending",
      "completed_tasks": []
    },
    "S7": {
      "date": "2026-05-13",
      "time": "morning",
      "title": "Testing (JUnit5 + Testcontainers)",
      "status": "pending",
      "completed_tasks": []
    },
    "S8": {
      "date": "2026-05-13",
      "time": "afternoon",
      "title": "Docker Compose + CI/CD + Actuator",
      "status": "pending",
      "completed_tasks": []
    }
  },
  "notes": ""
}
JSONEOF

echo "OK — files da duoc tao trong document/claude-routines/"
echo ""
echo "Tiep theo:"
echo "  git add document/"
echo "  git commit -m 'Add TaskFlow project plan and Claude Code coordinator routines'"
echo "  git push origin master"
echo ""
echo "Sau khi push xong, cai dat routines vao Claude Code:"
echo "  mkdir -p ~/.claude/commands"
echo "  cp document/claude-routines/taskflow_status.py ~/.claude/"
echo "  cp document/claude-routines/coordinate.md ~/.claude/commands/"
echo "  cp document/claude-routines/taskflow_progress.json ~/.claude/"
echo ""
echo "Them SessionStart hook vao ~/.claude/settings.json (xem SETUP_GUIDE.md)"
