# Scoreboard Sprite Sheet Template

## Image: scoreboard.png
**Dimensions: 256x128 pixels**

```
┌─────────────────────────────────────────────────────────────┐
│  X=0    32    64    96   128   160   192   224             │
│Y=0                                                          │
│   ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐               │
│   │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │ 8 │ 9 │  LARGE NUMS   │
│   └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘               │
│                                                             │
│Y=32                                                         │
│   ┌──┬──┬──┬──┬──┬──┬──┬──┬──┬──┐                        │
│   │0 │1 │2 │3 │4 │5 │6 │7 │8 │9 │  SMALL NUMS            │
│   └──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘                        │
│                                                             │
│Y=48                                                         │
│   ┌──┬──┬───┬───┬───┬───┬───┐                            │
│   │: │- │Q1 │Q2 │Q3 │Q4 │OT │  SPECIAL CHARS             │
│   └──┴──┴───┴───┴───┴───┴───┘                            │
│                                                             │
│Y=64                                                         │
│   ┌───┬───┬───┬───┬───┬───┬───┬───┐                      │
│   │WAS│PHI│CHI│BOS│BKN│MIA│ATL│GSW│  TEAM BG BARS        │
│   └───┴───┴───┴───┴───┴───┴───┴───┘                      │
│                                                             │
│Y=96  (empty space for future expansion)                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Exact Pixel Coordinates

### Row 0: Large Numbers (for scores)
- **Y position**: 0-32
- **Height**: 32 pixels
- **Width per character**: 32 pixels

| Char | Unicode | X Start | X End | Y Start | Y End |
|------|---------|---------|-------|---------|-------|
| 0    | \uE100  | 0       | 32    | 0       | 32    |
| 1    | \uE101  | 32      | 64    | 0       | 32    |
| 2    | \uE102  | 64      | 96    | 0       | 32    |
| 3    | \uE103  | 96      | 128   | 0       | 32    |
| 4    | \uE104  | 128     | 160   | 0       | 32    |
| 5    | \uE105  | 160     | 192   | 0       | 32    |
| 6    | \uE106  | 192     | 224   | 0       | 32    |
| 7    | \uE107  | 224     | 256   | 0       | 32    |
| 8    | \uE108  | 0       | 32    | 32      | 64    |
| 9    | \uE109  | 32      | 64    | 32      | 64    |

**Note**: Rows automatically continue - char 8 & 9 wrap to next 256px width

### Row 1: Small Numbers (for shot clock)
- **Y position**: 32-48 (or continue from large numbers)
- **Height**: 16 pixels  
- **Width per character**: 24 pixels

| Char | Unicode | X Start | X End | Y Start | Y End |
|------|---------|---------|-------|---------|-------|
| 0    | \uE200  | 64      | 88    | 32      | 48    |
| 1    | \uE201  | 88      | 112   | 32      | 48    |
| 2    | \uE202  | 112     | 136   | 32      | 48    |
| 3    | \uE203  | 136     | 160   | 32      | 48    |
| 4    | \uE204  | 160     | 184   | 32      | 48    |
| 5    | \uE205  | 184     | 208   | 32      | 48    |
| 6    | \uE206  | 208     | 232   | 32      | 48    |
| 7    | \uE207  | 232     | 256   | 32      | 48    |
| 8    | \uE208  | 0       | 24    | 48      | 64    |
| 9    | \uE209  | 24      | 48    | 48      | 64    |

### Row 2: Special Characters
- **Y position**: 48-64
- **Height**: 16 pixels
- **Width per character**: 16-24 pixels

| Char | Unicode | Description     | X Start | X End | Y Start | Y End |
|------|---------|-----------------|---------|-------|---------|-------|
| :    | \uE300  | Colon (time)    | 48      | 64    | 48      | 64    |
| -    | \uE301  | Dash (timeout)  | 64      | 80    | 48      | 64    |
| Q1   | \uE400  | Quarter 1       | 80      | 104   | 48      | 64    |
| Q2   | \uE401  | Quarter 2       | 104     | 128   | 48      | 64    |
| Q3   | \uE402  | Quarter 3       | 128     | 152   | 48      | 64    |
| Q4   | \uE403  | Quarter 4       | 152     | 176   | 48      | 64    |
| OT   | \uE404  | Overtime        | 176     | 200   | 48      | 64    |

### Row 3: Team Background Bars
- **Y position**: 64-96
- **Height**: 32 pixels
- **Width per bar**: 32 pixels (will stretch horizontally in display)

| Team | Unicode | Color          | X Start | X End | Y Start | Y End |
|------|---------|----------------|---------|-------|---------|-------|
| WAS  | \uE500  | Blue #5555FF   | 0       | 32    | 64      | 96    |
| PHI  | \uE501  | Red #FF5555    | 32      | 64    | 64      | 96    |
| CHI  | \uE502  | Red #FF5555    | 64      | 96    | 64      | 96    |
| BOS  | \uE503  | Green #00AA00  | 96      | 128   | 64      | 96    |
| BKN  | \uE504  | Black #000000  | 128     | 160   | 64      | 96    |
| MIA  | \uE505  | DkRed #AA0000  | 160     | 192   | 64      | 96    |
| ATL  | \uE506  | Red #FF5555    | 192     | 224   | 64      | 96    |
| GSW  | \uE507  | Blue #5555FF   | 224     | 256   | 64      | 96    |

---

## Design Notes

### For NBA-Style Appearance:

1. **Font**: Use Impact, Helvetica Bold, or similar bold sans-serif
2. **Numbers**: Make them slightly condensed (taller than wide)
3. **Anti-aliasing**: Use subtle anti-aliasing on white text
4. **Drop shadow**: Optional 1px black shadow at 135° angle
5. **Thickness**: Bold strokes (3-4px width on 32px height)
6. **Spacing**: Leave 2-3px padding inside each cell

### Color Specifications:

**All text elements**: Pure white #FFFFFF
**Team backgrounds**: 
- Solid color fill OR
- Vertical gradient (darker at top, lighter at bottom)
- Optional: 2px border in lighter shade

### Creating the Image:

**Tools**: Photoshop, GIMP, or online pixel art editor
**Grid**: Enable 16x16 or 32x32 grid for alignment
**Layers**: Separate layer for each row makes editing easier
**Export**: PNG-24 with transparency

---

## Quick Creation Steps:

1. Create 256x128px canvas with transparent background
2. Enable grid (16px or 32px)
3. Type large numbers 0-9 in Row 0 (white, bold, 28-30px font)
4. Type small numbers 0-9 in Row 1 (white, bold, 18-20px font)
5. Add colon (:) and dash (-) in Row 2
6. Add Q1, Q2, Q3, Q4, OT text in Row 2
7. Draw team color bars in Row 3 (solid colors or gradients)
8. Export as PNG with transparency
9. Save to: `assets/minecraft/textures/scoreboard/scoreboard.png`

---

## Testing:

After adding to your resource pack, test with:
```
/tellraw @s {"text":"\uE100\uE101\uE102","font":"minecraft:scoreboard"}
```
Should display: 012

```
/tellraw @s {"text":"\uE500 \uE001 WAS","font":"minecraft:scoreboard"}
```
Should display: [Blue bar] [WAS logo] WAS
