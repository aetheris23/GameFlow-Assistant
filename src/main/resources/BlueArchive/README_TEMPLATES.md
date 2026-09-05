# GameFlow Assistant — Template Resources: Blue Archive

Drop PNG image templates into this folder. The engine finds buttons by searching
for these templates inside the relevant ROI of a captured game frame.

## Expected files

| File                | Used by              | Typical ROI (fraction of 1920x1080) |
|---------------------|----------------------|--------------------------------------|
| `loading.png`       | state classifier     | whole frame                          |
| `main_menu.png`     | state classifier     | whole frame                          |
| `story_main.png`    | state classifier     | whole frame                          |
| `mission.png`       | state classifier     | whole frame                          |
| `dialog_choice.png` | dialogue choice      | whole frame                          |
| `next.png`          | STORY / DIALOG       | x 0.55..0.75, y 0.80..0.90          |
| `continue.png`      | STORY / DIALOG       | x 0.72..0.92, y 0.80..0.90          |
| `skip.png`          | STORY / DIALOG       | x 0.02..0.14, y 0.04..0.10          |
| `confirm.png`       | CONFIRMATION         | x 0.55..0.75, y 0.82..0.92          |
| `claim.png`         | REWARD               | x 0.55..0.75, y 0.82..0.92          |
| `start_mission.png` | MISSION              | x 0.55..0.75, y 0.82..0.92          |

## Notes

- Crop templates tightly around the button/label you want to click.
- Keep templates small (a button icon is ideal).
- If a template is missing, the session returns "no match" and the SafetyManager
  will pause/stop instead of clicking blindly — intentional.

Referenced from: `src/main/java/GameFlow/Games/BlueArchive/BlueArchiveProfile.java`