# -*- coding: utf-8 -*-
"""
Builds "Hospital Smart Queue Reportbook.docx" - a polished academic report
for the Hospital Smart Queue Management System, formatted in the same
Times New Roman / spacing style as the SkillBridge.docx reference, but
containing only Hospital Smart Queue project content (extracted from the
existing "Hospital Smart Queue Reportbook.html", which already holds the
final wording and the real screenshots).

Documentation-only: this script does not touch any application source code.
"""
import re
import os
import base64
import io

from docx import Document
from docx.shared import Pt, Inches, RGBColor, Emu
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.section import WD_SECTION_START
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

HERE = os.path.dirname(os.path.abspath(__file__))
HTML_PATH = os.path.join(HERE, "Hospital Smart Queue Reportbook.html")
OUT_PATH = os.path.join(HERE, os.environ.get("REPORTBOOK_OUT", "Hospital Smart Queue Reportbook.docx"))

FONT = "Times New Roman"
MONO = "Courier New"

html = open(HTML_PATH, encoding="utf-8").read()

# ---------------------------------------------------------------------------
# Extract the nine real screenshots embedded in the HTML (skip the cover logo)
# ---------------------------------------------------------------------------
img_matches = list(re.finditer(
    r'<div class="fig[^"]*"[^>]*>\s*<img[^>]*src="data:image/(?P<ext>png|jpeg);base64,(?P<data>[^"]+)"[^>]*alt="(?P<alt>[^"]*)"[^>]*>\s*<div class="caption">(?P<cap>.*?)</div>',
    html, re.S))

figures = []
for m in img_matches:
    ext = "jpg" if m.group("ext") == "jpeg" else "png"
    data = base64.b64decode(m.group("data"))
    figures.append({
        "alt": m.group("alt"),
        "caption": re.sub(r"<.*?>", "", m.group("cap")).replace("&mdash;", "—").strip(),
        "bytes": data,
        "ext": ext,
    })
print(f"Extracted {len(figures)} figures")

# ---------------------------------------------------------------------------
# Document setup
# ---------------------------------------------------------------------------
doc = Document()

section = doc.sections[0]
section.page_width = Inches(8.5)
section.page_height = Inches(11)
section.top_margin = Inches(1)
section.bottom_margin = Inches(1)
section.left_margin = Inches(1)
section.right_margin = Inches(1)

normal = doc.styles["Normal"]
normal.font.name = FONT
normal.font.size = Pt(12)
normal._element.rPr.rFonts.set(qn("w:eastAsia"), FONT)
normal.paragraph_format.line_spacing = 1.5
normal.paragraph_format.space_after = Pt(12)

# Give the built-in Heading 1-4 styles the same Times New Roman look used
# throughout the report. Using the real Heading styles (instead of bold
# Normal paragraphs) is what lets the Word TOC field actually find and list
# them - a manually-bolded paragraph carries no outline level, so a TOC
# field built on top of it comes back empty.
_HEADING_SPECS = {
    "Heading 1": dict(size=16, bold=True, italic=False, before=18, after=10),
    "Heading 2": dict(size=14, bold=True, italic=False, before=14, after=8),
    "Heading 3": dict(size=12.5, bold=True, italic=False, before=10, after=6),
    "Heading 4": dict(size=12, bold=True, italic=True, before=8, after=4),
}
for _name, _spec in _HEADING_SPECS.items():
    _st = doc.styles[_name]
    _st.font.name = FONT
    _st.font.size = Pt(_spec["size"])
    _st.font.bold = _spec["bold"]
    _st.font.italic = _spec["italic"]
    _st.font.color.rgb = RGBColor(0, 0, 0)
    _st.paragraph_format.space_before = Pt(_spec["before"])
    _st.paragraph_format.space_after = Pt(_spec["after"])
    _st.paragraph_format.line_spacing = 1.0
    _st.paragraph_format.keep_with_next = True
    _rPr = _st.element.get_or_add_rPr()
    _rFonts = _rPr.find(qn("w:rFonts"))
    if _rFonts is None:
        _rFonts = OxmlElement("w:rFonts")
        _rPr.append(_rFonts)
    _rFonts.set(qn("w:eastAsia"), FONT)

# Chapter titles ("1. Introduction", "2. Project Planning...") get the same
# black underline rule the original HTML/PDF report uses under its <h1>
# headings, so the two documents read as one consistent design rather than
# the DOCX looking like a flatter, unrelated draft of the PDF.
_h1_pPr = doc.styles["Heading 1"].element.get_or_add_pPr()
_h1_pbdr = OxmlElement("w:pBdr")
_h1_bottom = OxmlElement("w:bottom")
_h1_bottom.set(qn("w:val"), "single")
_h1_bottom.set(qn("w:sz"), "12")
_h1_bottom.set(qn("w:space"), "4")
_h1_bottom.set(qn("w:color"), "000000")
_h1_pbdr.append(_h1_bottom)
_h1_pPr.append(_h1_pbdr)


def set_run_font(run, size=12, bold=False, italic=False, font=FONT, color=None):
    run.font.name = font
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    if color:
        run.font.color.rgb = RGBColor(*color)
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.find(qn("w:rFonts"))
    if rFonts is None:
        rFonts = OxmlElement("w:rFonts")
        rPr.append(rFonts)
    rFonts.set(qn("w:eastAsia"), font)


def add_para(text="", size=12, bold=False, italic=False, align=None,
             space_after=12, space_before=0, line_spacing=1.5, font=FONT,
             color=None):
    p = doc.add_paragraph()
    if align is not None:
        p.alignment = align
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.space_before = Pt(space_before)
    p.paragraph_format.line_spacing = line_spacing
    if text:
        r = p.add_run(text)
        set_run_font(r, size, bold, italic, font, color)
    return p


def add_bullet(text, size=12):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.line_spacing = 1.5
    r = p.add_run(text)
    set_run_font(r, size)
    return p


def add_chapter(number_title, bookmark=None):
    """Chapter-level heading, e.g. '1. Introduction' -> Heading 1 (16pt bold).
    Real Word heading style, so it is picked up by the TOC field."""
    return doc.add_heading(number_title, level=1)


def add_h2(text):
    """'1.1 Overview' style heading -> Heading 2 (14pt bold)."""
    return doc.add_heading(text, level=2)


def add_h3(text):
    """'2.1.1 Functional Requirements' style heading -> Heading 3 (12.5pt bold)."""
    return doc.add_heading(text, level=3)


def add_h4(text):
    """Category heading (e.g. 'User Management') -> Heading 4 (12pt bold
    italic). Deliberately left out of the TOC field's "1-3" outline range."""
    return doc.add_heading(text, level=4)


def add_diagram(text):
    """Monospace ASCII-art block (folder tree / ER relationship tree)."""
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(12)
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.line_spacing = 1.0
    lines = text.split("\n")
    for i, line in enumerate(lines):
        r = p.add_run(line)
        set_run_font(r, 10, font=MONO)
        if i != len(lines) - 1:
            p.add_run().add_break()
    # light shading for readability
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:fill"), "F2F2F2")
    p.paragraph_format.element.get_or_add_pPr().append(shd)
    return p


def add_flow(boxes):
    """Centered 'User -> Browser -> ... -> Database' flow, each stage drawn
    as an actual small box (a narrow 1-cell table), not a full page-width
    paragraph border - a plain <w:pBdr> on a centered short line still
    stretches edge-to-edge in Word, which reads as a stack of ugly bars
    rather than a flowchart."""
    for i, box in enumerate(boxes):
        table = doc.add_table(rows=1, cols=1)
        table.alignment = WD_TABLE_ALIGNMENT.CENTER
        table.autofit = False
        cell = table.rows[0].cells[0]
        cell.width = Inches(2.4)
        tcPr = cell._tc.get_or_add_tcPr()
        tcW = OxmlElement("w:tcW")
        tcW.set(qn("w:w"), str(Inches(2.4).twips))
        tcW.set(qn("w:type"), "dxa")
        tcPr.append(tcW)
        borders = OxmlElement("w:tcBorders")
        for edge in ("top", "left", "bottom", "right"):
            el = OxmlElement(f"w:{edge}")
            el.set(qn("w:val"), "single")
            el.set(qn("w:sz"), "10")
            el.set(qn("w:space"), "0")
            el.set(qn("w:color"), "000000")
            borders.append(el)
        tcPr.append(borders)
        shade_cell(cell, "F2F2F2")
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.space_before = Pt(4)
        r = p.add_run(box)
        set_run_font(r, 12, bold=True)
        if i != len(boxes) - 1:
            add_para("↓", size=13, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER,
                     space_after=2, space_before=2)
    add_para("", size=2, space_after=6)


def style_table(table):
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl = table._tbl
    tblPr = tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{edge}")
        el.set(qn("w:val"), "single")
        el.set(qn("w:sz"), "4")
        el.set(qn("w:space"), "0")
        el.set(qn("w:color"), "000000")
        borders.append(el)
    tblPr.append(borders)


def shade_cell(cell, color="D9E2F3"):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:fill"), color)
    tcPr.append(shd)


def add_table(headers, rows, no_break=False):
    table = doc.add_table(rows=1, cols=len(headers))
    style_table(table)
    hdr = table.rows[0].cells
    for i, h in enumerate(headers):
        hdr[i].text = ""
        p = hdr[i].paragraphs[0]
        r = p.add_run(h)
        set_run_font(r, 12, bold=True)
        shade_cell(hdr[i])
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.0
    for row in rows:
        cells = table.add_row().cells
        for i, val in enumerate(row):
            p = cells[i].paragraphs[0]
            r = p.add_run(val)
            set_run_font(r, 12)
            p.paragraph_format.space_after = Pt(4)
            p.paragraph_format.line_spacing = 1.0
    add_para("", size=2, space_after=6)
    return table


def add_figure(fig, number):
    tmp_path = os.path.join(HERE, f"_tmp_fig_{number}.{fig['ext']}")
    with open(tmp_path, "wb") as f:
        f.write(fig["bytes"])
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(10)
    p.paragraph_format.space_after = Pt(4)
    run = p.add_run()
    run.add_picture(tmp_path, width=Inches(5.6))
    os.remove(tmp_path)
    cap = add_para(f"Figure {number}. {fig['caption']}", size=11, italic=True,
                   align=WD_ALIGN_PARAGRAPH.CENTER, space_after=14)
    return cap


def add_page_break():
    doc.add_page_break()


def add_toc_field():
    """Insert a real Word TOC field that updates when the user opens/updates it."""
    p = doc.add_paragraph()
    run = p.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = 'TOC \\o "1-3" \\h \\z \\u'
    fld_sep = OxmlElement("w:fldChar")
    fld_sep.set(qn("w:fldCharType"), "separate")
    fld_text = OxmlElement("w:t")
    fld_text.text = "Right-click here and choose “Update Field” (or press F9) to generate the table of contents."
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")

    r_element = run._r
    r_element.append(fld_begin)
    r_element.append(instr)
    r_element.append(fld_sep)
    r_element.append(fld_text)
    r_element.append(fld_end)
    set_run_font(run, 12)


def add_page_number_field(paragraph):
    run = paragraph.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = "PAGE"
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_end)
    set_run_font(run, 10)


def enable_update_fields_on_open():
    settings = doc.settings.element
    upd = OxmlElement("w:updateFields")
    upd.set(qn("w:val"), "true")
    settings.insert(0, upd)


def start_numbered_body_section():
    """Break into a new section for the main body: its own footer with a
    PAGE field, restarting the page count at 1 so the cover/details/Contents
    pages stay unnumbered and Chapter 1 becomes page 1."""
    body_section = doc.add_section(WD_SECTION_START.NEW_PAGE)
    body_section.footer.is_linked_to_previous = False
    fp = body_section.footer.paragraphs[0] if body_section.footer.paragraphs \
        else body_section.footer.add_paragraph()
    fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    add_page_number_field(fp)
    pg_num_type = OxmlElement("w:pgNumType")
    pg_num_type.set(qn("w:start"), "1")
    body_section._sectPr.append(pg_num_type)
    return body_section


# Front matter (cover, details, contents) intentionally has no footer/page
# number - it is added at the top of the body section, right before Chapter 1.

# ===========================================================================
# PAGE 1 - COVER
# ===========================================================================
# One deliberate spacer (rather than several stacked blank paragraphs with
# default spacing, which just reads as leftover dead space) pushes the title
# block down to roughly the vertical center of the page.
add_para("", space_after=0, space_before=210)
add_para("Hospital Smart Queue Management System", size=26, bold=True,
         align=WD_ALIGN_PARAGRAPH.CENTER, space_after=30)
add_para("UNIVERSITY OF INFORMATION TECHNOLOGY", size=16, bold=True,
         align=WD_ALIGN_PARAGRAPH.CENTER, space_after=6)
add_para("SEPTEMBER 2026", size=13, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=0)
add_page_break()

# ===========================================================================
# PAGE 2 - PROJECT / GROUP DETAILS
# ===========================================================================
add_para("Course Name: CST-6108  Enterprise Applications Development using Java", size=12)
add_para("Project Title: Hospital Smart Queue Management System", size=12, bold=True)
add_para("Group Members", size=13, bold=True, space_before=10)

members = [
    ("Mg La Yaung Htut", "TNT-2384", "layaunghtut@uit.edu.mm"),
    ("Ma Hsu Shoon Wutyi", "TNT-2346", "hswuttye@uit.edu.mm"),
    ("Ma Noo Noo Aung", "TNT-2357", "nonoaung@uit.edu.mm"),
    ("Ma Ei Pone Chit", "TNT-2357", "eiponechit@uit.edu.mm"),
    ("Ma Phoo Thet Ngone", "TNT-2348", "phuthetngone@uit.edu.mm"),
]
for i, (name, sid, email) in enumerate(members, start=1):
    add_para(f"{i}. {name}", size=12, bold=True, space_after=2)
    add_para(f"     Student ID: {sid}", size=12, space_after=2)
    add_para(f"     Email: {email}", size=12, space_after=10)

add_para("Supervisor: Dr. Daw Ei Moh Moh Aung", size=12, space_before=8)
add_para("Department: Faculty of Computer Science", size=12)
add_para("Submission Date: September 2026", size=12)
add_page_break()

# ===========================================================================
# PAGE 3 - TABLE OF CONTENTS
# ===========================================================================
add_para("Contents", size=18, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=16)
add_toc_field()
start_numbered_body_section()

# ===========================================================================
# 1. INTRODUCTION
# ===========================================================================
add_chapter("1. Introduction")

add_h2("1.1 Overview")
add_para("The Hospital Smart Queue System is a web-based application designed to improve "
         "the management of patients, doctors, appointments, and hospital queues. In many "
         "hospitals, patients have to wait for a long time and staff members need to "
         "manually manage queues and appointments. This can cause confusion, delays, and "
         "inefficient use of hospital resources.")
add_para("The proposed system provides a centralized platform where hospital staff can "
         "manage patient information, doctor information, departments, appointments, "
         "emergency cases, and patient queues. Patients can be registered and assigned to "
         "appropriate doctors, while staff can monitor the queue and update its status.")
add_para("The system is developed using Java Spring Boot, MySQL, HTML/CSS, and related web "
         "technologies. The application follows a database-driven architecture so that "
         "information can be stored and retrieved efficiently.")

add_h2("1.2 Project Scope")
add_para("The scope of the Hospital Smart Queue System covers user account management, "
         "patient registration and management, doctor registration and management, and "
         "hospital department management. It also covers appointment management, queue "
         "number generation and management, emergency case management, and consultation "
         "and diagnosis records, along with staff management and queue status monitoring. "
         "Access throughout the system is role-based, with separate permissions for "
         "administrators, doctors, staff, and patients.")
add_para("The system is mainly intended for use within a hospital environment. It focuses "
         "on improving the organization of patient queues and reducing unnecessary waiting "
         "and manual work.")

add_h2("1.3 Objectives")
add_para("The main objective of the project is to develop a computerized hospital queue "
         "management system that reduces patient waiting time and queue confusion while "
         "providing an organized method for managing appointments. The system is intended "
         "to let hospital staff manage patients and doctors efficiently, provide emergency "
         "priority handling, and store hospital information in a centralized database, so "
         "that communication between patients, doctors, and hospital staff improves.")
add_para("Beyond these core goals, the project also aims to provide a simple and "
         "user-friendly interface, reduce errors caused by manual queue management, and "
         "lay a foundation that future hospital management features can build on.")
add_page_break()

# ===========================================================================
# 2. PROJECT PLANNING AND REQUIREMENT ANALYSIS
# ===========================================================================
add_chapter("2. Project Planning and Requirement Analysis")

add_h2("2.1 Functional vs. Non-Functional Requirements")
add_para("Requirement analysis for the Hospital Smart Queue System distinguishes between "
         "functional requirements, which describe what the system must do, and "
         "non-functional requirements, which describe how well the system must do it. Both "
         "are organized below by module and by quality attribute respectively.")

add_h3("2.1.1 Functional Requirements")
add_para("The functional requirements describe what the system should be able to do.")

functional = [
    ("User Management", "The system should allow administrators to create user accounts, "
     "manage usernames and passwords, and assign user roles, covering administrator, "
     "doctor, staff, and patient accounts alike."),
    ("Patient Management", "Staff should be able to register new patients, view and update "
     "their information, search for a patient by name or other details, and associate each "
     "patient with the appointments and queues they are part of."),
    ("Doctor Management", "Administrators or authorized staff should be able to register "
     "doctors, assign them to departments, and record their specializations, contact "
     "information, and room numbers, as well as activate or deactivate a doctor's account "
     "when needed."),
    ("Department Management", "The system should maintain the hospital's departments, "
     "including Cardiology, Dental, Emergency, Neurology, Pediatrics, Dermatology, "
     "Orthopedics, and General Medicine."),
    ("Appointment Management", "Staff should be able to create appointments, assign "
     "patients to doctors, set appointment dates and times, and view or update appointment "
     "status as a visit progresses."),
    ("Queue Management", "The system should create queue entries and generate queue "
     "numbers, associating each entry with the relevant patient and doctor. It should also "
     "let staff set queue priority and update queue status, giving emergency patients "
     "higher priority than routine cases."),
    ("Emergency Management", "Staff should be able to record emergency cases, capturing "
     "the patient involved, the staff member handling the case, the reason for the "
     "emergency, and its current status."),
    ("Consultation Management", "Doctors should be able to record consultation details "
     "against a queue entry, including the doctor involved, the diagnosis, and any "
     "consultation notes."),
]
for title, body in functional:
    add_h4(title)
    add_para(body)

add_h3("2.1.2 Non-Functional Requirements")
add_para("The system should also satisfy several non-functional requirements that describe "
         "the quality attributes it must exhibit.")

nonfunctional = [
    ("Performance", "The system should respond to normal user requests quickly and "
     "efficiently."),
    ("Security", "User information and passwords should be protected, with access to "
     "system functions depending on the user's role."),
    ("Reliability", "The system should store information consistently and reduce errors "
     "caused by manual record keeping."),
    ("Usability", "The interface should be simple enough for hospital staff to use without "
     "extensive technical knowledge."),
    ("Maintainability", "The application should be organized into separate components, "
     "such as models, repositories, services, and controllers, so that future developers "
     "can maintain and extend it."),
    ("Scalability", "The system should be designed so that additional features, users, "
     "doctors, departments, and hospital branches can be added in the future."),
]
for title, body in nonfunctional:
    add_h4(title)
    add_para(body)

add_h2("2.2 Creating Use Cases and User Stories")
roles = [
    ("Administrator", "An administrator can log into the system, manage users, doctors, "
     "and departments, view hospital information, and manage overall system settings.",
     "User Story: As an administrator, I want to manage users and doctors so that the "
     "hospital system contains accurate information."),
    ("Doctor", "A doctor can log into the system, view assigned appointments and the "
     "patients currently in the queue, look up patient information, and record diagnoses "
     "and consultation notes as consultations are completed.",
     "User Story: As a doctor, I want to see my patient queue so that I can treat patients "
     "in the correct order."),
    ("Staff", "Staff members can register patients, create appointments, manage queues, "
     "handle emergency cases, update appointment status, and monitor patients who are "
     "waiting for consultation.",
     "User Story: As a staff member, I want to manage the patient queue so that patients "
     "can be served efficiently."),
    ("Patient", "A patient can log into the system to view their appointment and queue "
     "information and check their current queue status.",
     "User Story: As a patient, I want to know my queue number and status so that I "
     "understand when I may be called for consultation."),
]
for title, body, story in roles:
    add_h4(title)
    add_para(body, space_after=4)
    add_para(story, italic=True, space_after=12)
add_page_break()

# ===========================================================================
# 3. SYSTEM DESIGN AND ARCHITECTURE
# ===========================================================================
add_chapter("3. System Design and Architecture")

add_h2("3.1 System Architecture Design")
add_para("The Hospital Smart Queue System uses a layered application architecture. The "
         "main layers are:")

layers = [
    ("Presentation Layer", "This layer provides the user interface through web pages. "
     "Users interact with the system through their browser."),
    ("Controller Layer", "The controller receives requests from users and determines "
     "which operation should be performed."),
    ("Service Layer", "The service layer contains the main business logic of the "
     "application. It processes operations such as creating appointments, managing "
     "queues, and handling consultations."),
    ("Repository Layer", "The repository layer communicates with the database and "
     "performs operations such as inserting, updating, deleting, and retrieving records."),
    ("Database Layer", "MySQL is used to store system data."),
]
for title, body in layers:
    add_h4(title)
    add_para(body)

add_para("The basic flow is:")
add_flow(["User", "Web Browser", "Controller", "Service", "Repository", "MySQL Database"])
add_para("This architecture separates different responsibilities and makes the "
         "application easier to maintain.", space_before=10)

add_h2("3.2 Database Design: ER Diagrams")
add_para("The database contains several related entities. The main tables include users, "
         "departments, doctors, staff, patients, appointments, queue, emergency, and "
         "consultation.")
add_para("The major relationships are:")
add_diagram(
"""Users
 +-- Doctors
 +-- Staff
 +-- Patients

Departments
     |
     +-- Doctors

Patients
     |
     +-- Appointments
     +-- Queue
     +-- Emergency

Doctors
     |
     +-- Appointments
     +-- Queue
     +-- Consultation

Appointments
     |
     +-- Queue

Queue
     |
     +-- Consultation""")
add_para("For example, a patient can have multiple appointments. An appointment is "
         "associated with a doctor, and an appointment can result in a queue entry. A "
         "queue entry can then be associated with a consultation.")
add_para("The database uses primary keys to uniquely identify records and foreign keys to "
         "establish relationships between tables.")
add_page_break()

# ===========================================================================
# 4. DEVELOPMENT ENVIRONMENT SETUP
# ===========================================================================
add_chapter("4. Development Environment Setup")

add_h2("4.1 Tools and Technologies")
add_para("The following technologies are used to develop the system:")
add_table(
    ["Technology", "Purpose"],
    [
        ["Java", "Main programming language"],
        ["Spring Boot", "Backend application framework"],
        ["Spring Data JPA", "Database access"],
        ["Hibernate", "ORM framework"],
        ["MySQL", "Database management"],
        ["Maven", "Project and dependency management"],
        ["HTML", "Web page structure"],
        ["CSS", "Web page styling"],
        ["JavaScript", "Client-side functionality"],
        ["IntelliJ IDEA", "Development environment"],
        ["XAMPP / phpMyAdmin", "MySQL database administration"],
        ["Firefox / Chrome", "Application testing"],
    ])

add_h2("4.2 Development Environment")
add_para("The project is developed using IntelliJ IDEA. The backend is created using "
         "Spring Boot and managed using Maven. MySQL is used as the database server, "
         "while phpMyAdmin can be used to view and manage database records during "
         "development.")
add_para("The project follows a structure similar to:")
add_diagram(
"""HospitalSmartQueueSystem
|
+-- src
|   +-- main
|   |   +-- java
|   |   |   +-- com.hospitalqueue
|   |   |       +-- controller
|   |   |       +-- model
|   |   |       +-- repository
|   |   |       +-- service
|   |   |
|   |   +-- resources
|   |       +-- templates
|   |       +-- static
|   |       +-- application.properties
|   |
|   +-- test
|
+-- pom.xml
+-- mvnw""")

add_h2("4.3 Coding Standards")
add_para("The project follows a consistent set of coding practices: meaningful class and "
         "variable names, standard Java naming conventions, and a clear separation of "
         "responsibilities into different packages. Entity classes represent database "
         "tables, repositories handle database operations, services contain business "
         "logic, and controllers handle incoming web requests. Code duplication is "
         "avoided where possible, comments are added where they improve understanding, "
         "and indentation and formatting are kept consistent throughout.")
add_page_break()

# ===========================================================================
# 5. SCREEN LAYOUT AND DESIGN
# ===========================================================================
add_chapter("5. Screen Layout and Design")
add_para("The system provides different screens depending on the user's role.")

screens = [
    ("Login Screen", "The login page allows users to enter their username and password. "
     "After successful authentication, the user is directed to the appropriate dashboard "
     "based on their role."),
    ("Administrator Dashboard", "The administrator dashboard provides access to user "
     "management, doctor management, staff management, patient management, department "
     "management, and appointment management, giving administrators a single place to "
     "oversee the whole system."),
    ("Staff Dashboard", "The staff dashboard focuses on daily hospital operations, "
     "including patient registration, appointment management, queue management, "
     "emergency cases, and patient information."),
    ("Doctor Dashboard", "The doctor dashboard shows today's appointments and the current "
     "queue, along with patient information, consultation, diagnosis, and consultation "
     "notes."),
    ("Patient Dashboard", "The patient dashboard can display personal information, "
     "appointment information, and the patient's queue number and status."),
]
for title, body in screens:
    add_h2(title)
    add_para(body)

add_h3("Queue Screen")
add_para("The queue screen should clearly display information such as:")
add_table(
    ["Queue Number", "Patient", "Doctor", "Priority", "Status"],
    [
        ["A001", "Mg Mg", "Dr. Su Su", "NORMAL", "WAITING"],
        ["A002", "Ma Ma", "Dr. Su Su", "NORMAL", "WAITING"],
        ["B001", "Ko Ko", "Dr. James", "EMERGENCY", "WAITING"],
    ])
add_para("Emergency cases can be visually prioritized so staff and doctors can identify "
         "them easily.")
add_page_break()

# ===========================================================================
# 6. CONCLUSION AND REFLECTION
# ===========================================================================
add_chapter("6. Conclusion and Reflection")

add_h2("6.1 Project Summary")
add_para("The Hospital Smart Queue System was developed to improve the management of "
         "patients and hospital queues. The system combines patient management, doctor "
         "management, appointments, emergency cases, queues, and consultations into one "
         "centralized application.")
add_para("The use of a relational database allows information to be stored systematically "
         "and relationships between patients, doctors, appointments, queues, and "
         "consultations to be maintained.")
add_para("The system also provides a foundation for reducing manual queue management and "
         "improving hospital workflow.")

add_h2("6.2 Challenges and Solutions")
add_para("During development, several challenges were encountered.")

challenges = [
    ("Database Relationships", "Managing relationships between multiple tables was "
     "challenging because foreign keys must reference valid records. This was solved by "
     "inserting related data in the correct order. For example, users and doctors need to "
     "exist before dependent records such as appointments and queues can be created."),
    ("Duplicate Data", "Duplicate values occurred when unique fields such as usernames or "
     "queue numbers were inserted more than once. This was handled by checking existing "
     "records before inserting new data and using unique identifiers appropriately."),
    ("Application Configuration", "Connecting the Spring Boot application to MySQL "
     "required correct database configuration and compatible entity mappings. The "
     "database connection and JPA configuration were tested during development."),
    ("Queue Management", "The queue system required relationships between appointments, "
     "patients, and doctors. The queue table was therefore designed to reference these "
     "records using foreign keys."),
]
for title, body in challenges:
    add_h4(title)
    add_para(body)

add_h2("6.3 Lessons Learned")
add_para("Through this project, the team developed several important skills, including "
         "Java programming, Spring Boot development, database design, SQL and MySQL, and "
         "working with JPA and Hibernate. Building the application also strengthened the "
         "team's understanding of REST and web application concepts, HTML and CSS, "
         "software architecture, debugging, foreign-key relationship management, and "
         "project planning.")
add_para("The project also demonstrated the importance of testing each part of a system "
         "before connecting all components together.")

add_h2("6.4 Future Enhancements")
add_para("Looking ahead, the system could be extended with online patient registration "
         "and online appointment booking, along with automatic queue number generation "
         "and real-time queue updates. SMS and email notifications, together with "
         "real-time emergency alerts, would keep patients and staff better informed, and "
         "a mobile application would make the system easier to reach outside the "
         "hospital.")
add_para("On the administrative side, doctor availability schedules, hospital branch "
         "management, and detailed reports and statistics would support day-to-day "
         "planning, while dashboard charts and analytics would give administrators a "
         "clearer view of overall performance. Stronger authentication and password "
         "encryption, along with support for patient medical history, would further "
         "improve the security and usefulness of the system.")
add_page_break()

# ===========================================================================
# 7. APPENDICES
# ===========================================================================
add_chapter("7. Appendices")

add_h2("7.1 Screen Design")
add_para("Screen designs of the implemented system are presented below.")
add_para("Note: The following pages show the UI/UX design references (high-fidelity "
         "wireframes) produced during the design phase of the project. They illustrate "
         "the intended screen layout and information architecture described in Section "
         "5; minor branding and wording differences from the deployed application may "
         "exist.", italic=True, space_after=16)

for i, fig in enumerate(figures, start=1):
    add_figure(fig, i)

add_h2("7.2 References")
references = [
    "[1] L. V. Green, \"Queueing analysis in healthcare,\" in Patient Flow: Reducing "
    "Delay in Healthcare Delivery, R. W. Hall, Ed. Boston, MA: Springer, 2006, pp. 281–307.",
    "[2] N. Gilboy, P. Tanabe, D. Travers, and A. M. Rosenau, Emergency Severity Index "
    "(ESI): A Triage Tool for Emergency Department Care, Version 4. Implementation "
    "Handbook 2012 Edition, AHRQ Pub. No. 12-0014, Rockville, MD: Agency for Healthcare "
    "Research and Quality, 2012.",
    "[3] F. Y. Lin and H. C. Chen, \"Machine learning algorithms for predicting outpatient "
    "waiting time,\" International Journal of Environmental Research and Public Health, "
    "vol. 17, no. 18, p. 6542, 2020.",
    "[4] I. Mohammadi, H. Wu, A. Turkcan, T. Toscos, and B. N. Doebbeling, \"Data analytics "
    "and modeling for appointment no-show in outpatient clinics,\" Journal of Healthcare "
    "Engineering, vol. 2018, Article ID 4187050, 2018.",
    "[5] P. Lewis et al., \"Retrieval-augmented generation for knowledge-intensive NLP "
    "tasks,\" in Advances in Neural Information Processing Systems (NeurIPS 2020), vol. "
    "33, pp. 9459–9474, 2020.",
    "[6] R. C. Martin, Clean Architecture: A Craftsman's Guide to Software Structure and "
    "Design. Boston, MA: Prentice Hall, 2017.",
    "[7] J. Bloch, Effective Java, 3rd ed. Boston, MA: Addison-Wesley Professional, 2018.",
    "[8] C. Walls, Spring in Action, 6th ed. Shelter Island, NY: Manning Publications, "
    "2022.",
    "[9] A. Silberschatz, H. F. Korth, and S. Sudarshan, Database System Concepts, 7th "
    "ed. New York: McGraw-Hill Education, 2020.",
    "[10] R. T. Fielding, \"Architectural styles and the design of network-based software "
    "architectures,\" Ph.D. dissertation, Department of Information and Computer Science, "
    "University of California, Irvine, CA, 2000.",
]
for ref in references:
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(9)
    p.paragraph_format.line_spacing = 1.5
    pf = p.paragraph_format
    pf.left_indent = Inches(0.35)
    pf.first_line_indent = Inches(-0.35)
    r = p.add_run(ref)
    set_run_font(r, 12)

# ---------------------------------------------------------------------------
enable_update_fields_on_open()
doc.save(OUT_PATH)
print("Saved:", OUT_PATH)
print("Size:", os.path.getsize(OUT_PATH))
