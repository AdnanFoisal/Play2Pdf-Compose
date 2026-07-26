from fpdf import FPDF

class StudyGuidePDF(FPDF):
    def add_page(self, *args, **kwargs):
        super().add_page(*args, **kwargs)
        self.set_fill_color(240, 240, 240)
        self.rect(0, 0, self.w, self.h, "F")

pdf = StudyGuidePDF()
pdf.add_page()
pdf.set_fill_color(255, 255, 255)
pdf.rect(0, 0, pdf.w, pdf.h, "F")
pdf.set_fill_color(100, 100, 100)
pdf.rect(15, 0, 8, pdf.h, "F")
cx, cy = pdf.w / 2 + 5, pdf.h / 2 + 10
pdf.set_draw_color(0, 0, 0)
pdf.set_line_width(1.5)
pdf.ellipse(cx - 30, cy - 30, 60, 60, "D")
pdf.set_fill_color(40, 40, 40)
pdf.ellipse(cx - 10, cy + 10, 40, 40, "F")
pdf.line(15, cy - 40, pdf.w, cy - 40)
pdf.line(cx + 10, cy + 50, cx + 10, pdf.h)
pdf.set_y(40)
pdf.set_x(30)
pdf.set_font("helvetica", "B", 42)
pdf.multi_cell(0, 16, "TEST SUBJECT LONG TO SEE IF IT BREAKS THE PAGE", align="L")
pdf.output("test.pdf")
print("PDF created, pages:", pdf.page_no())
