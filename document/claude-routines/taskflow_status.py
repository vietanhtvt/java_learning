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
