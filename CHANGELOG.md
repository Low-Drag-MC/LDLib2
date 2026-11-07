## v26.3.2.46
* Added compact counts to item slots past three digits, with the full count in the tooltip
* Fixed fields held back by `@ConditionalSynced` never being saved, and array fields re-sending after their elements changed
* Fixed a saved editor layout without one of the editor's panes crashing the next layout load, and docked views going missing when a layout rearranged the panes
* Fixed scenes making the game rebuild its chunk section buffers every frame while it draws the world by multi-draw indirect
* Fixed SDF rects cutting off their anti-aliased edge
* Fixed compact numbers, such as fluid slot amounts, never showing their decimal
* Fixed text fields keeping their old placeholder after it changed
* Fixed `LocalizationUtils.exist` answering false for every key, so enum constants never picked up their translations
* Fixed uitest clients left running when the game never exits
