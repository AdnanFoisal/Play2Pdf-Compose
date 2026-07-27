
import os
import sys
from server import StudyGuidePDF, THEMES, sanitize_for_pdf, format_views, QRCache

def test_pdf_generation():
    theme = THEMES["nordic_frost"]
    pdf = StudyGuidePDF("Test Subject", theme)
    
    # Mock data
    results = [
        {
            "topic": "Introduction to Python",
            "matched": True,
            "videos": [
                {
                    "id": "vid1",
                    "title": "Python for Beginners - Full Course",
                    "duration": "10:00",
                    "views": 1500000,
                    "url": "https://youtube.com/watch?v=vid1"
                }
            ],
            "study_note": "This video covers the basics of Python syntax and installation."
        },
        {
            "topic": "Advanced Data Structures",
            "matched": True,
            "videos": [
                {
                    "id": "vid2",
                    "title": "Data Structures and Algorithms",
                    "duration": "45:30",
                    "views": 50000,
                    "url": "https://youtube.com/watch?v=vid2"
                }
            ],
            "study_note": "Deep dive into trees, graphs, and hash maps."
        }
    ]
    
    req_author = "Test Author"
    req_subject = "Test Subject"
    req_playlist_urls = ["https://youtube.com/playlist?list=PL123"]
    
    qr_cache = QRCache()
    
    # --- Cover Page ---
    pdf.add_page()
    pdf.set_fill_color(*theme["bg"])
    pdf.rect(0, 0, pdf.w, pdf.h, "F")
    pdf.set_y(pdf.h / 2 - 30)
    pdf.set_font(theme["font_family"], "B", 48)
    pdf.set_text_color(*theme["text"])
    pdf.cell(0, 20, sanitize_for_pdf(req_subject.upper()), align="C")
    
    # --- Grid Page ---
    pdf.add_page()
    pdf.render_grid_headers()
    CW = pdf.col_widths
    
    row_counter = 0
    for topic_idx, res in enumerate(results):
        for v_idx, vid in enumerate(res["videos"]):
            row_counter += 1
            row_height = 20
            row_y = pdf.get_y()
            
            # Alternate row background
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
            idx_str = f"{topic_idx+1}"
            pdf.cell(CW[1], row_height, idx_str, border="B", align="C")
            topic_label = sanitize_for_pdf(res["topic"])
            pdf.cell(CW[2], row_height, topic_label, border="B")
            
            x_col3 = pdf.get_x()
            pdf.cell(CW[3], row_height, "", border="B")
            pdf.set_xy(x_col3, row_y + 2)
            pdf.set_font(theme["font_family"], "B", 9)
            pdf.cell(CW[3], 5, sanitize_for_pdf(vid["title"]))
            
            pdf.set_xy(x_col3, row_y + 8)
            pdf.set_font(theme["font_family"], "I", 7.5)
            pdf.set_text_color(100, 110, 125)
            note_text = f"Key Focus: {sanitize_for_pdf(res['study_note'])}"
            pdf.cell(CW[3], 5, note_text)
            
            pdf.set_xy(x_col3 + CW[3], row_y)
            pdf.set_font(theme["font_family"], "", 9)
            pdf.set_text_color(*theme["paper_text"])
            
            pdf.cell(CW[4], row_height, vid["duration"], border="B", align="C")
            pdf.cell(CW[5], row_height, format_views(vid["views"]), border="B", align="C")
            
            qr_path = qr_cache.get(vid["id"], vid["url"])
            x_qr = pdf.get_x()
            pdf.cell(CW[6], row_height, "", border="B")
            pdf.image(qr_path, x=x_qr + 4, y=row_y + 2, w=16, h=16)
            
            pdf.set_xy(x_qr + CW[6], row_y)
            pdf.set_text_color(*theme["accent"])
            pdf.set_font(theme["font_family"], "B", 9)
            pdf.cell(CW[7], row_height, "Watch Link", border="B", align="C", link=vid["url"])
            pdf.set_y(row_y + row_height)

    pdf.output("repro_output.pdf")
    qr_cache.cleanup()
    print("PDF generated: repro_output.pdf")

if __name__ == "__main__":
    test_pdf_generation()
