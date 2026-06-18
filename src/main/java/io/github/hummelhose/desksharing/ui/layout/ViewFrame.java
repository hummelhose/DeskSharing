package io.github.hummelhose.desksharing.ui.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class ViewFrame extends VerticalLayout {

    public ViewFrame(Component... components) {
        add(components);

        setSpacing(true);
        setPadding(false);
        setWidthFull();

        getStyle()
                .set("box-sizing", "border-box")
                .set("background", "rgba(255, 255, 255, 0.96)")
                .set("border-radius", "24px")
                .set("border", "1px solid rgba(226, 232, 240, 0.92)")
                .set("box-shadow", "0 18px 45px rgba(15, 23, 42, 0.08)")
                .set("padding", "1.75rem")
                .set("gap", "1rem")
                .set("backdrop-filter", "blur(10px)");
    }
}