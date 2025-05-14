package com.lowdragmc.lowdraglib.gui.ui.style;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.style.value.TextWrap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true, fluent = true)
public class TextStyle extends Style {
    @Getter
    @Setter
    private Horizontal textAlignHorizontal = Horizontal.LEFT;
    @Getter @Setter
    private Vertical textAlignVertical = Vertical.TOP;
    @Getter @Setter
    private TextWrap textWrap = TextWrap.NONE;
    @Getter @Setter
    private float fontSize = 9;
    @Getter @Setter
    private float lineSpacing = 1;
    @Getter @Setter
    private int textColor = -1;
    @Getter @Setter
    private boolean textShadow = true;

    public TextStyle(UIElement holder) {
        super(holder);
    }
}
