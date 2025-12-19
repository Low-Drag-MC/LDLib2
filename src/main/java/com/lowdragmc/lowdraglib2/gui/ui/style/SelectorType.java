package com.lowdragmc.lowdraglib2.gui.ui.style;

/**
 * The SelectorType enum defines types of LSS-style selectors that can be used
 * to identify elements in a UI or styled structure.
 *
 * The following types of selectors are defined:
 *
 * - CLASS: Represents a class selector, used to select elements based on their class names.
 * - ID: Represents an ID selector, used to select a specific element based on its unique identifier.
 * - ELEMENT: Represents an element selector, used to select HTML-like element tags directly.
 * - UNIVERSAL: Represents a universal selector, used to select all elements in a given context.
 */
public enum SelectorType {NOT, CLASS, ID, ELEMENT, UNIVERSAL}
