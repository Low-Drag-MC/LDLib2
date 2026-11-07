## v2.2.46
* Added compact counts to item slots past three digits, with the full count in the tooltip
* Fixed fields held back by `@ConditionalSynced` never being saved, and array fields re-sending after their elements changed
* Fixed a saved editor layout without one of the editor's panes crashing the next layout load, and docked views going missing when a layout rearranged the panes
* Fixed elements with an opacity below 1 or an overflow clip sometimes rendering black (thanks @QiuYe-123)
* Fixed SDF rects cutting off their anti-aliased edge and applying its alpha twice
* Fixed compact numbers, such as fluid slot amounts, never showing their decimal
* Fixed text fields keeping their old placeholder after it changed
* Fixed uitest clients left running when the game never exits
