"""
Reproduction test for the blank-page bug in Play2PDF.

Root cause analysis:
1. `set_auto_page_break(auto=True, margin=15)` is ON
2. In the row loop, we manually check `will_page_break(row_height + 4)` (= 24mm)
   and add a page + headers ourselves.
3. But then we draw the row content using `set_xy` to jump back to row_y and
   draw cells at specific positions. The cells at lines 667 and 673 use `ln=True`
   which advances the Y cursor DOWN. After line 673, Y = row_y + 13.
4. Then at line 675, we do `set_xy(x_col3 + CW[3], row_y)` to jump BACK UP.
5. We draw more cells, then `pdf.ln(row_height)` at line 691 advances Y by 20mm.
6. BUT: fpdf2's auto_page_break triggers during cell() if the cursor would
   land below (page_height - margin). When cells with ln=True push Y down
   while auto_page_break is on, fpdf can trigger an AUTOMATIC page break
   DURING cell rendering — creating a blank page with just the background fill.
7. Our manual `will_page_break` check at the top doesn't prevent this because
   it checks for 24mm of space, but the actual rendering path jumps Y around
   in complex ways.

The fix: DISABLE auto_page_break entirely and handle ALL page breaks manually
via will_page_break checks. This prevents fpdf from ever creating surprise pages.
"""

from fpdf import FPDF
import os, tempfile, hashlib, qrcode

class StudyGuidePDF(FPDF):
    def __init__(self, subject, theme):
        super().__init__(orientation="L", unit="mm", format="A4")
        self.subject = subject
        self.theme = theme
        self.set_auto_page_break(auto=True, margin=15)  # BUG: This causes blank pages
        self.set_margins(10, 10, 10)
        self.col_widths = [12, 12, 55, 95, 18, 18, 25, 25]

    def add_page(self, *args, **kwargs):
        super().add_page(*args, **kwargs)
        self.set_fill_color(*self.theme["paper_bg"])
        self.rect(0, 0, self.w, self.h, "F")

    def footer(self):
        self.set_y(-12)
        self.set_font(self.theme["font_family"], "I", 8)
        self.set_text_color(*self.theme["paper_text"])
        self.cell(140, 10, self.subject, align="L")
        self.cell(0, 10, f"Page {self.page_no()}", align="R")

    def render_grid_headers(self):
        CW = self.col_widths
        self.set_font(self.theme["font_family"], "B", 10)
        self.set_text_color(*self.theme["accent"])
        self.set_draw_color(*self.theme["paper_border"])
        self.cell(CW[0], 8, "[ ]", border="TB", align="C")
        self.cell(CW[1], 8, "#", border="TB", align="C")
        self.cell(CW[2], 8, "Topic", border="TB")
        self.cell(CW[3], 8, "Video & Note", border="TB")
        self.cell(CW[4], 8, "Length", border="TB", align="C")
        self.cell(CW[5], 8, "Views", border="TB", align="C")
        self.cell(CW[6], 8, "QR", border="TB", align="C")
        self.cell(CW[7], 8, "Link", border="TB", align="C")
        self.ln(8)


theme = {
    "bg": (240, 240, 240), "accent": (100, 150, 100), "text": (0, 0, 0),
    "subtext": (100, 100, 100), "paper_bg": (255, 255, 255),
    "paper_text": (30, 41, 59), "paper_border": (203, 213, 225),
    "font_family": "Helvetica",
}

def truncate(s, n):
    return s[:n-1] + ".." if len(s) > n else s

# Generate a QR code for testing
def make_qr():
    qr = qrcode.QRCode(version=1, box_size=4, border=1)
    qr.add_data("https://youtube.com/watch?v=test")
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    path = os.path.join(tempfile.gettempdir(), "test_qr.png")
    img.save(path)
    return path

qr_path = make_qr()

# Simulate 50 matched topics, each with 2 videos = 100 rows
matched_results = []
for i in range(50):
    matched_results.append({
        "topic": f"Topic {i+1}: Introduction to Data Structures and Algorithms Part {i+1}",
        "matched": True,
        "study_note": f"This video covers the fundamentals of topic {i+1} including key concepts.",
        "confidence": "high",
        "videos": [
            {"id": f"vid_{i}_1", "title": f"Video {i+1}.1 - Lecture on Topic {i+1}", "duration": "45:30", "views": 50000, "url": "https://youtube.com/watch?v=test"},
            {"id": f"vid_{i}_2", "title": f"Video {i+1}.2 - Tutorial on Topic {i+1}", "duration": "32:15", "views": 30000, "url": "https://youtube.com/watch?v=test"},
        ]
    })

pdf = StudyGuidePDF("Test Subject", theme)

# Cover page
pdf.add_page()
pdf.set_fill_color(*theme["bg"])
pdf.rect(0, 0, pdf.w, pdf.h, "F")
pdf.set_y(40)
pdf.set_x(30)
pdf.set_font(theme["font_family"], "B", 42)
pdf.set_text_color(*theme["text"])
pdf.multi_cell(0, 16, "TEST SUBJECT", align="L")

# Grid page
pdf.add_page()
pdf.set_draw_color(*theme["paper_border"])
pdf.set_text_color(*theme["paper_text"])

pdf.set_fill_color(*theme["accent"])
pdf.set_text_color(*theme["text"])
pdf.set_font(theme["font_family"], "B", 10)
pdf.cell(0, 8, "SUMMARY", align="C", fill=True, ln=True)
pdf.ln(4)

pdf.render_grid_headers()
CW = pdf.col_widths

row_counter = 0
for topic_idx, res in enumerate(matched_results):
    for v_idx, vid in enumerate(res["videos"]):
        row_counter += 1
        row_height = 20
        if pdf.will_page_break(row_height + 4):
            pdf.add_page()
            pdf.render_grid_headers()

        row_y = pdf.get_y()

        if row_counter % 2 == 1:
            bg_r, bg_g, bg_b = theme["paper_bg"]
            alt = lambda c: max(0, c - 8) if c > 128 else min(255, c + 15)
            pdf.set_fill_color(alt(bg_r), alt(bg_g), alt(bg_b))
            pdf.rect(10, row_y, sum(CW), row_height, "F")

        pdf.set_xy(10, row_y)
        pdf.set_font(theme["font_family"], "", 9)
        pdf.set_text_color(*theme["paper_text"])
        pdf.set_draw_color(*theme["paper_border"])

        pdf.cell(CW[0], row_height, "[  ]", border="B", align="C")
        idx_str = f"{topic_idx+1}" if len(res["videos"]) == 1 else f"{topic_idx+1}.{v_idx+1}"
        pdf.cell(CW[1], row_height, idx_str, border="B", align="C")
        topic_label = truncate(res["topic"], 26) if v_idx == 0 else f"  └ {truncate(res['topic'], 24)}"
        pdf.cell(CW[2], row_height, topic_label, border="B")

        x_col3 = pdf.get_x()
        pdf.cell(CW[3], row_height, "", border="B")
        pdf.set_xy(x_col3, row_y + 2)
        pdf.set_font(theme["font_family"], "B", 9)
        pdf.cell(CW[3], 5, truncate(vid["title"], 52), ln=True)  # <-- ln=True pushes Y

        pdf.set_xy(x_col3, row_y + 8)
        pdf.set_font(theme["font_family"], "I", 7.5)
        pdf.set_text_color(100, 110, 125)
        note_text = f"Key Focus: {truncate(res['study_note'], 70)}" if res.get("study_note") else ""
        pdf.cell(CW[3], 5, note_text, ln=True)  # <-- ln=True pushes Y again

        pdf.set_xy(x_col3 + CW[3], row_y)
        pdf.set_font(theme["font_family"], "", 9)
        pdf.set_text_color(*theme["paper_text"])

        pdf.cell(CW[4], row_height, vid["duration"], border="B", align="C")
        pdf.cell(CW[5], row_height, "50K", border="B", align="C")

        x_qr = pdf.get_x()
        pdf.cell(CW[6], row_height, "", border="B")
        pdf.image(qr_path, x=x_qr + 4, y=row_y + 2, w=16, h=16)

        pdf.set_xy(x_qr + CW[6], row_y)
        pdf.set_text_color(*theme["accent"])
        pdf.set_font(theme["font_family"], "B", 9)
        pdf.cell(CW[7], row_height, "Watch", border="B", align="C", link=vid["url"])
        pdf.ln(row_height)  # <-- This advances Y, potentially triggering auto page break

print(f"WITH auto_page_break=True: {pdf.page_no()} pages for {row_counter} rows")
pdf.output("test_with_auto.pdf")

# Now test WITHOUT auto_page_break
class StudyGuidePDFFixed(FPDF):
    def __init__(self, subject, theme):
        super().__init__(orientation="L", unit="mm", format="A4")
        self.subject = subject
        self.theme = theme
        self.set_auto_page_break(auto=False)  # FIX: disable auto breaks
        self.set_margins(10, 10, 10)
        self.col_widths = [12, 12, 55, 95, 18, 18, 25, 25]

    def add_page(self, *args, **kwargs):
        super().__init__  # just to note the fix
        super().add_page(*args, **kwargs)
        self.set_fill_color(*self.theme["paper_bg"])
        self.rect(0, 0, self.w, self.h, "F")

    def footer(self):
        self.set_y(-12)
        self.set_font(self.theme["font_family"], "I", 8)
        self.set_text_color(*self.theme["paper_text"])
        self.cell(140, 10, self.subject, align="L")
        self.cell(0, 10, f"Page {self.page_no()}", align="R")

    def render_grid_headers(self):
        CW = self.col_widths
        self.set_font(self.theme["font_family"], "B", 10)
        self.set_text_color(*self.theme["accent"])
        self.set_draw_color(*self.theme["paper_border"])
        self.cell(CW[0], 8, "[ ]", border="TB", align="C")
        self.cell(CW[1], 8, "#", border="TB", align="C")
        self.cell(CW[2], 8, "Topic", border="TB")
        self.cell(CW[3], 8, "Video & Note", border="TB")
        self.cell(CW[4], 8, "Length", border="TB", align="C")
        self.cell(CW[5], 8, "Views", border="TB", align="C")
        self.cell(CW[6], 8, "QR", border="TB", align="C")
        self.cell(CW[7], 8, "Link", border="TB", align="C")
        self.ln(8)

pdf2 = StudyGuidePDFFixed("Test Subject", theme)
pdf2.add_page()
pdf2.set_fill_color(*theme["bg"])
pdf2.rect(0, 0, pdf2.w, pdf2.h, "F")
pdf2.set_y(40)
pdf2.set_x(30)
pdf2.set_font(theme["font_family"], "B", 42)
pdf2.set_text_color(*theme["text"])
pdf2.multi_cell(0, 16, "TEST SUBJECT", align="L")

pdf2.add_page()
pdf2.set_draw_color(*theme["paper_border"])
pdf2.set_text_color(*theme["paper_text"])
pdf2.set_fill_color(*theme["accent"])
pdf2.set_text_color(*theme["text"])
pdf2.set_font(theme["font_family"], "B", 10)
pdf2.cell(0, 8, "SUMMARY", align="C", fill=True, ln=True)
pdf2.ln(4)
pdf2.render_grid_headers()
CW2 = pdf2.col_widths

row_counter2 = 0
for topic_idx, res in enumerate(matched_results):
    for v_idx, vid in enumerate(res["videos"]):
        row_counter2 += 1
        row_height = 20
        # Manual page break check with enough margin for row + footer
        if pdf2.get_y() + row_height + 15 > pdf2.h:
            pdf2.add_page()
            pdf2.render_grid_headers()

        row_y = pdf2.get_y()

        if row_counter2 % 2 == 1:
            bg_r, bg_g, bg_b = theme["paper_bg"]
            alt = lambda c: max(0, c - 8) if c > 128 else min(255, c + 15)
            pdf2.set_fill_color(alt(bg_r), alt(bg_g), alt(bg_b))
            pdf2.rect(10, row_y, sum(CW2), row_height, "F")

        pdf2.set_xy(10, row_y)
        pdf2.set_font(theme["font_family"], "", 9)
        pdf2.set_text_color(*theme["paper_text"])
        pdf2.set_draw_color(*theme["paper_border"])

        pdf2.cell(CW2[0], row_height, "[  ]", border="B", align="C")
        idx_str = f"{topic_idx+1}" if len(res["videos"]) == 1 else f"{topic_idx+1}.{v_idx+1}"
        pdf2.cell(CW2[1], row_height, idx_str, border="B", align="C")
        topic_label = truncate(res["topic"], 26) if v_idx == 0 else f"  └ {truncate(res['topic'], 24)}"
        pdf2.cell(CW2[2], row_height, topic_label, border="B")

        x_col3 = pdf2.get_x()
        pdf2.cell(CW2[3], row_height, "", border="B")
        pdf2.set_xy(x_col3, row_y + 2)
        pdf2.set_font(theme["font_family"], "B", 9)
        pdf2.cell(CW2[3], 5, truncate(vid["title"], 52))  # NO ln=True

        pdf2.set_xy(x_col3, row_y + 8)
        pdf2.set_font(theme["font_family"], "I", 7.5)
        pdf2.set_text_color(100, 110, 125)
        note_text = f"Key Focus: {truncate(res['study_note'], 70)}" if res.get("study_note") else ""
        pdf2.cell(CW2[3], 5, note_text)  # NO ln=True

        pdf2.set_xy(x_col3 + CW2[3], row_y)
        pdf2.set_font(theme["font_family"], "", 9)
        pdf2.set_text_color(*theme["paper_text"])

        pdf2.cell(CW2[4], row_height, vid["duration"], border="B", align="C")
        pdf2.cell(CW2[5], row_height, "50K", border="B", align="C")

        x_qr = pdf2.get_x()
        pdf2.cell(CW2[6], row_height, "", border="B")
        pdf2.image(qr_path, x=x_qr + 4, y=row_y + 2, w=16, h=16)

        pdf2.set_xy(x_qr + CW2[6], row_y)
        pdf2.set_text_color(*theme["accent"])
        pdf2.set_font(theme["font_family"], "B", 9)
        pdf2.cell(CW2[7], row_height, "Watch", border="B", align="C", link=vid["url"])
        # Manually set Y to next row position instead of ln()
        pdf2.set_y(row_y + row_height)

print(f"WITH auto_page_break=False: {pdf2.page_no()} pages for {row_counter2} rows")
pdf2.output("test_without_auto.pdf")

# Cleanup
os.remove(qr_path)
