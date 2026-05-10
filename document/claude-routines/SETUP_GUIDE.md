# Hướng dẫn Cài đặt Claude Code Routines

Hai routines hỗ trợ quản lý tiến độ dự án TaskFlow API trong Claude Code.

---

## Cài đặt

### Bước 1 — Copy files về máy

```bash
# Tạo thư mục nếu chưa có
mkdir -p ~/.claude/commands

# Copy script hiển thị trạng thái
cp taskflow_status.py ~/.claude/

# Copy slash command coordinator
cp coordinate.md ~/.claude/commands/

# Copy file theo dõi tiến độ
cp taskflow_progress.json ~/.claude/
```

### Bước 2 — Thêm SessionStart hook vào settings.json

Mở `~/.claude/settings.json` và thêm block `SessionStart` vào `hooks`:

```json
{
  "hooks": {
    "SessionStart": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "python3 ~/.claude/taskflow_status.py 2>/dev/null || true",
            "timeout": 5
          }
        ]
      }
    ]
  }
}
```

> Nếu `settings.json` đã có hooks khác, chỉ thêm block `SessionStart` vào bên trong `hooks`, không xóa hooks cũ.

### Bước 3 — Kiểm tra

Test script chạy đúng:
```bash
python3 ~/.claude/taskflow_status.py
```

Kết quả mong đợi:
```
╔══════════════════════════════════════════╗
║      TASKFLOW PROJECT COORDINATOR        ║
╠══════════════════════════════════════════╣
║  Hom nay : 2026-05-10                    ║
║  Deadline: 2026-05-14                    ║
║  Con lai : 4 ngay                        ║
║  Da xong : 0/8 sessions                  ║
║  Tiep theo: S1 - Project bootstrap...    ║
╚══════════════════════════════════════════╝
  Go /coordinate de nhan task cu the
```

---

## Sử dụng hàng ngày

### Routine 1: SessionStart Hook (tự động)

Mỗi khi mở Claude Code, dashboard sẽ **tự động hiện** — không cần làm gì thêm.

Dashboard hiển thị:
- Ngày hôm nay và deadline
- Số ngày còn lại
- Số sessions đã hoàn thành
- Session tiếp theo cần làm

### Routine 2: `/coordinate` Slash Command

Gõ `/coordinate` trong Claude Code để nhận:
- Danh sách 3-5 task cụ thể cho session hiện tại
- Prompt mẫu sẵn sàng copy-paste
- Ước tính thời gian
- Cảnh báo nếu có nguy cơ trễ deadline

**Khi nào dùng `/coordinate`:**
- Đầu mỗi session để biết cần làm gì
- Khi bị mất định hướng giữa chừng
- Sau khi xong 1 phần, muốn biết tiếp theo là gì

---

## Cập nhật tiến độ

Sau khi hoàn thành 1 session, mở `~/.claude/taskflow_progress.json` và đổi status:

```json
{
  "sessions": {
    "S1": {
      "status": "done",          ← đổi từ "pending" thành "done"
      "completed_tasks": [
        "Spring Boot init",
        "BaseEntity + Auditing",
        "User + Project entities",
        "Flyway migrations"
      ]
    }
  }
}
```

Dashboard sẽ tự động cập nhật lần mở Claude Code tiếp theo.

---

## Cấu trúc Files

```
~/.claude/
├── settings.json              ← SessionStart hook (tự động chạy)
├── taskflow_status.py         ← Script hiển thị dashboard
├── taskflow_progress.json     ← File theo dõi tiến độ (cập nhật thủ công)
└── commands/
    └── coordinate.md          ← Slash command /coordinate
```

---

## Mẹo Quản lý Usage Limit Pro

| Quy tắc | Chi tiết |
|---------|----------|
| Tối đa 2 sessions/ngày | Mỗi session ~2 giờ, đủ để hoàn thành 1 topic |
| Chuẩn bị prompt trước | Copy prompt mẫu từ `PROJECT_PLAN.md` trước khi mở Claude |
| Dùng `/compact` khi context dài | Giải phóng tokens, tránh bị cut off |
| Copy code ra IDE ngay | Không cần giữ Claude mở để test |
| 1 topic = 1 session | Không gộp nhiều topic vào 1 lần hỏi |

---

## Yêu cầu

- Python 3.6+
- Claude Code CLI
- File `taskflow_progress.json` phải có ở `~/.claude/`
