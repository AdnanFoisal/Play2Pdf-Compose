"""One-off A3 refactor: extract the bottom sheets from CompileScreen.kt
into CompileSheets.kt. Pure move; makes sheet functions internal."""
import io
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app/src/main/java/com/adnanfoisal/play2pdf/ui/compile/CompileScreen.kt")
DST = os.path.join(ROOT, "app/src/main/java/com/adnanfoisal/play2pdf/ui/compile/CompileSheets.kt")

content = io.open(SRC, encoding="utf-8").read()

marker = "// --- Bottom Sheets ---"
idx = content.index(marker)
sheets = content[idx:]
rest = content[:idx].rstrip() + "\n"

imports = re.findall(r"^import .*$", content, re.M)
pattern = re.compile(
    r"layout\.(Arrangement|Column|Spacer|fillMaxWidth|height|padding|size|Box)"
    r"|material3\.(ExperimentalMaterial3Api|ModalBottomSheet|Text$|rememberModalBottomSheetState)"
    r"|runtime\.Composable|ui\.Modifier|ui\.unit\.(dp|sp)|text\.font\.FontWeight"
    r"|foundation\.(background|shape)|ui\.draw\.clip|theme\.(AppShape|AppType|BrandColors)"
    r"|tokens\.Spacing|components\.(PremiumTextField|PrimaryButton)"
    r"|domain\.model\.PdfTheme|compile\.components\.PdfThemePreviewRow"
)
used = [i for i in imports if pattern.search(i)]

sheets = re.sub(
    r"private fun (PlaylistUrlDialog|TopicInputDialog|ThemePickerDialog|AdvancedDialog|SheetHandle)",
    r"internal fun \1",
    sheets,
)

out = (
    "package com.adnanfoisal.play2pdf.ui.compile\n\n"
    "// Extracted from CompileScreen.kt (A3): the four bottom sheets.\n"
    "// Pure move - no behaviour changes.\n\n"
    + "\n".join(sorted(set(used)))
    + "\n\n"
    + sheets
)

io.open(DST, "w", encoding="utf-8", newline="\n").write(out)
io.open(SRC, "w", encoding="utf-8", newline="\n").write(rest)
print("CompileSheets.kt:", len(out.splitlines()), "lines")
print("CompileScreen.kt:", len(rest.splitlines()), "lines")
print("imports carried:", len(set(used)))
