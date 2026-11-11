#!/usr/bin/env python3
"""Source-level journal checks, not a Minecraft compilation or visual test."""
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/com/chaogarden'
SOURCE = (JAVA / 'item/GardenGuideItem.java').read_text()
PAGE = re.compile(r'page\(\s*((?:"(?:[^"\\]|\\.)*"\s*(?:\+\s*)?)+)\)', re.S)
PAGES = [''.join(json.loads(s) for s in re.findall(r'"(?:[^"\\]|\\.)*"', m.group(1)))
         for m in PAGE.finditer(SOURCE)]
SECTIONS = dict(p.split('\n\n', 1) for p in PAGES)
EXPECTED_TITLES = '''A Keeper's Journal|First care|Care supplies|The Analyzer|Training|Learning to move|Learning to swim|Learning to fly|Growth and alignment|Evolution|Adult forms|Breeding|Inheritance|Special eggs|Animal Genes|Reincarnation|Chaos Chao|Safe transport|Minecraft adaptations|Pack makers|Fruit orchards|Learning gardening|Watering trees|Planted-tree life|Black Market|Rare egg discovery|Gem eggs|Other market gifts|Your gardens|Garden registration|Garden boundaries|Building a race|Races and karate|Chao companions|Companion flight and water|Nether companions|End companions|Robot legacy|A robot's next life|Companion recovery|Choosing a gift|Curious hands|Personalities|Portable adventure fruit|Kindergarten travel|Meet the staff|Doctor care|Grade care|Classroom|Learned skills|Wardrobe|Handling eggs|Garden friends|Contest awards|Expanded garden gifts|Birthday parties|Optional garden rules|Official comparison (OP)'''.split('|')


class JournalContentTest(unittest.TestCase):
    def test_section_identity_and_order_preserved(self):
        self.assertEqual([p.split('\n\n', 1)[0] for p in PAGES], EXPECTED_TITLES)
        self.assertEqual(len(SECTIONS), 58)

    def test_sections_are_short_and_paragraphed(self):
        for title, body in SECTIONS.items():
            with self.subTest(title=title):
                self.assertTrue(body.strip())
                self.assertIn('\n\n', body)
                self.assertLessEqual(len(body.split()), 100)
                self.assertLessEqual(max(map(len, body.split('\n\n'))), 410)

    def test_navigation_contract_unchanged(self):
        screen = (JAVA / 'client/AdventureGuideScreen.java').read_text()
        self.assertIn('Search chapters and sections', screen)
        self.assertIn('chapter and section names', SECTIONS["A Keeper's Journal"])
        self.assertIn('stack.set(DataComponents.WRITTEN_BOOK_CONTENT, getDefaultInstance()', SOURCE)

    def test_growth_and_evolution_teach_mechanics(self):
        self.assertIn('Eating whole fruit speeds growth', SECTIONS['Growth and alignment'])
        self.assertIn('POINTS', SECTIONS['Evolution'])
        self.assertIn('Stamina gives Normal', SECTIONS['Evolution'])
        self.assertIn('tied ability', SECTIONS['Evolution'])

    def test_robot_and_breeding_contradictions_removed(self):
        self.assertNotIn('including robot construction', SOURCE)
        self.assertNotIn('Any two', SECTIONS['Breeding'])
        self.assertIn('never allows Omochao breeding', SECTIONS['Breeding'])
        self.assertIn('five race wins', SECTIONS['Robot legacy'])
        self.assertIn('above 50', SECTIONS["A robot's next life"])
        self.assertIn('Omochao grades improve through rebuilding', SECTIONS['Grade care'])
        self.assertIn('current progress resets', SECTIONS['Companion recovery'])
        self.assertNotIn('keeps its genes, name and progress', SECTIONS['Companion recovery'])

    def test_points_and_levels_are_explicit(self):
        self.assertIn('135 points', SECTIONS['Learning to fly'])
        self.assertIn('level 99', SECTIONS['Learning to fly'])
        self.assertIn('Swim level 70', SECTIONS['Companion flight and water'])
        self.assertIn('level 99 adds water breathing', SECTIONS['Companion flight and water'])
        for title in ('Learning to move', 'Learning to swim'):
            self.assertIn('POINTS', SECTIONS[title])

    def test_help_is_actionable(self):
        self.assertIn('1.', SECTIONS['First care'])
        self.assertIn('No response?', SECTIONS['Choosing a gift'])
        self.assertIn('Registration and release are separate steps', SECTIONS['Garden registration'])
        self.assertIn('must train it again', SECTIONS['Grade care'])
        self.assertIn('enrollment happens at the Teacher', SECTIONS['Classroom'])
        self.assertIn('not every visible part', SECTIONS['Animal Genes'])

    def test_doctor_routes_match_screen_modes(self):
        screen = (JAVA / 'client/KindergartenScreen.java').read_text()
        chart, home = screen.split('if(mode.equals("chart")){', 1)[1].split('}else{', 1)
        home = home.split('case "TEACHER"', 1)[0]
        self.assertNotIn('"SEE THE DOCTOR"', chart)
        self.assertIn('"SEE THE DOCTOR"', home)
        self.assertIn('"MEDICAL CHART",()->view("chart")', home)
        self.assertIn('"STATS"', chart)
        self.assertIn('if(chart==0)', chart)
        self.assertIn('"UP"', chart)
        self.assertIn('from the list and choose SEE THE DOCTOR on the main menu', SECTIONS['Doctor care'])
        self.assertIn('open MEDICAL CHART, then STATS', SECTIONS['Grade care'])
        self.assertLess(SECTIONS['Grade care'].index('MEDICAL CHART'), SECTIONS['Grade care'].index('UP'))

    def test_analyzer_links_use_real_tabs(self):
        inspection = (JAVA / 'client/ChaoInspectionScreen.java').read_text()
        for tab in ('STATS', 'GENES', 'PARTS', 'MIND', 'CARE'):
            self.assertIn('"' + tab + '"', inspection)
            self.assertIn(tab, SECTIONS['The Analyzer'])
        self.assertIn('Favorite Fruit', inspection)
        self.assertIn('Favorite Fruit', SECTIONS['The Analyzer'])


if __name__ == '__main__':
    unittest.main(verbosity=2)
