#!/usr/bin/env python3
"""Regression checks for coffin retirement, replacing the removed lid choreography."""
from pathlib import Path
import json,unittest
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
class CoffinRetirementAssets(unittest.TestCase):
 def test_old_shape_textures_and_loot_are_absent(self):
  self.assertFalse(list((ASSETS/'models/sa2_02515').glob('chao_coffin*.obj')))
  self.assertFalse(list((ASSETS/'models/block').glob('chao_coffin*.json')))
  self.assertFalse(list((ASSETS/'textures/block/sa2_02515/chao_coffin').glob('*.png')))
  self.assertFalse((ASSETS/'textures/item/chao_coffin.png').exists())
  self.assertFalse((ROOT/'src/main/resources/data/chao_garden/loot_table/blocks/chao_coffin.json').exists())
 def test_compatibility_resources_are_neutral_and_static(self):
  state=json.loads((ASSETS/'blockstates/chao_coffin.json').read_text())
  self.assertEqual(state,{'variants':{'':{'model':'chao_garden:block/retired_toy_marker'}}})
  marker=json.loads((ASSETS/'models/block/retired_toy_marker.json').read_text())
  self.assertEqual(marker['elements'][0]['from'],[4,0,4]);self.assertEqual(marker['elements'][0]['to'],[12,1,12])
  item=json.loads((ASSETS/'models/item/chao_coffin.json').read_text())
  self.assertEqual(item['textures']['layer0'],'minecraft:item/paper')
 def test_no_recipe_or_tag_reintroduces_the_retired_item(self):
  data=ROOT/'src/main/resources/data'
  for folder in ('recipe','tags'):
   for path in data.glob('**/'+folder+'/**/*.json'):
    self.assertNotIn('chao_garden:chao_coffin',path.read_text(),str(path))
if __name__=='__main__':unittest.main(verbosity=2)
