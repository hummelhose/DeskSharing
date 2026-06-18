package io.github.hummelhose.desksharing.ui.view;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.hummelhose.desksharing.application.service.OfficeService;
import io.github.hummelhose.desksharing.application.service.ResourceService;
import io.github.hummelhose.desksharing.application.service.RoomService;
import io.github.hummelhose.desksharing.domain.model.Office;
import io.github.hummelhose.desksharing.domain.model.Resource;
import io.github.hummelhose.desksharing.domain.model.ResourceType;
import io.github.hummelhose.desksharing.domain.model.Room;
import io.github.hummelhose.desksharing.infrastructure.security.AdminAccessService;
import io.github.hummelhose.desksharing.ui.layout.MainLayout;
import io.github.hummelhose.desksharing.ui.layout.ViewFrame;
import jakarta.annotation.security.PermitAll;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Route(value = "admin/floorplan-editor", layout = MainLayout.class)
@PageTitle("Plan-Editor")
@PermitAll
public class AdminFloorplanEditorView extends VerticalLayout implements BeforeEnterObserver {

    private static final int DEFAULT_OFFICE_WIDTH = 1800;
    private static final int DEFAULT_OFFICE_HEIGHT = 1100;
    private static final int MIN_OFFICE_WIDTH = 900;
    private static final int MIN_OFFICE_HEIGHT = 600;

    private static final int GRID_SIZE = 20;

    private static final int DEFAULT_ROOM_WIDTH = 280;
    private static final int DEFAULT_ROOM_HEIGHT = 180;
    private static final int MIN_ROOM_WIDTH = 160;
    private static final int MIN_ROOM_HEIGHT = 110;

    private static final int DEFAULT_DESK_WIDTH = 108;
    private static final int DEFAULT_DESK_HEIGHT = 74;
    private static final int MIN_DESK_WIDTH = 60;
    private static final int MIN_DESK_HEIGHT = 40;

    private final OfficeService officeService;
    private final RoomService roomService;
    private final ResourceService resourceService;
    private final AdminAccessService adminAccessService;

    private final ComboBox<Office> officeField = new ComboBox<>("Büro / Standort");
    private final Div editorCanvas = new Div();

    private final Button selectionModeButton = new Button("Auswahl");
    private final Button roomModeButton = new Button("Raum");
    private final Button deskModeButton = new Button("Tisch");
    private final Button gridModeButton = new Button("Raster: AN");

    private final Button zoom75Button = new Button("75%");
    private final Button zoom100Button = new Button("100%");
    private final Button zoom125Button = new Button("125%");
    private final Button zoom150Button = new Button("150%");

    private EditorTool currentTool = EditorTool.SELECT;
    private boolean snapToGridEnabled = true;
    private int currentZoomPercent = 100;

    private enum EditorTool {
        SELECT,
        ROOM,
        DESK
    }

    public AdminFloorplanEditorView(OfficeService officeService,
                                    RoomService roomService,
                                    ResourceService resourceService,
                                    AdminAccessService adminAccessService) {
        this.officeService = officeService;
        this.roomService = roomService;
        this.resourceService = resourceService;
        this.adminAccessService = adminAccessService;

        configureOfficeField();
        configureToolButtons();
        configureCanvas();

        ViewFrame headerFrame = createHeaderFrame();
        ViewFrame officeFrame = createOfficeFrame();
        ViewFrame editorFrame = createEditorFrame();

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle()
                .set("background", "linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%)")
                .set("gap", "1rem");

        add(headerFrame, officeFrame, editorFrame);

        refreshOfficeSelector(null);
        renderOfficeCanvas(null);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!adminAccessService.isCurrentUserAdmin()) {
            event.forwardTo(DashboardView.class);
        }
    }

    private ViewFrame createHeaderFrame() {
        Span eyebrow = new Span("PLAN-EDITOR");
        eyebrow.getStyle()
                .set("display", "inline-flex")
                .set("width", "fit-content")
                .set("padding", "0.3rem 0.62rem")
                .set("border-radius", "999px")
                .set("font-size", "0.68rem")
                .set("font-weight", "850")
                .set("letter-spacing", "0.08em")
                .set("color", "#1d4ed8")
                .set("background", "#dbeafe")
                .set("border", "1px solid #bfdbfe");

        H1 title = new H1("Plan-Editor");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.65rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.045em")
                .set("line-height", "1.05")
                .set("color", "#0f172a");

        Paragraph subtitle = new Paragraph("Büro auswählen, Räume einzeichnen und Tische direkt im Plan platzieren.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "#64748b")
                .set("font-size", "0.92rem")
                .set("line-height", "1.45");

        ViewFrame frame = new ViewFrame(eyebrow, title, subtitle);
        frame.getStyle()
                .set("padding", "1.15rem 1.35rem")
                .set("gap", "0.55rem");

        return frame;
    }

    private ViewFrame createOfficeFrame() {
        H3 title = createSectionTitle("Büro / Standort");
        Paragraph text = createSectionText("Wähle ein Büro aus oder lege einen neuen Standort an.");

        Button createOfficeButton = new Button("Büro anlegen", event -> openCreateOfficeDialog());
        stylePrimaryButton(createOfficeButton);

        Button editOfficeButton = new Button("Büro bearbeiten", event -> openEditOfficeDialog());
        styleSecondaryButton(editOfficeButton);

        Button deleteOfficeButton = new Button("Büro löschen", event -> openDeleteOfficeDialog());
        styleDangerButton(deleteOfficeButton);

        HorizontalLayout officeActions = new HorizontalLayout(
                officeField,
                createOfficeButton,
                editOfficeButton,
                deleteOfficeButton
        );
        officeActions.setWidthFull();
        officeActions.setPadding(false);
        officeActions.setSpacing(true);
        officeActions.setAlignItems(Alignment.END);
        officeActions.getStyle()
                .set("gap", "0.75rem")
                .set("flex-wrap", "wrap");

        ViewFrame frame = new ViewFrame(title, text, officeActions);
        frame.getStyle()
                .set("padding", "1.35rem 1.5rem");

        return frame;
    }

    private ViewFrame createEditorFrame() {
        H3 title = createSectionTitle("Büroplan");
        Paragraph text = createSectionText("Wähle ein Werkzeug aus. Räume, Tische und die Bürofläche selbst kannst du mit der Maus bearbeiten.");

        HorizontalLayout canvasTools = new HorizontalLayout(
                selectionModeButton,
                roomModeButton,
                deskModeButton,
                gridModeButton
        );
        canvasTools.setPadding(false);
        canvasTools.setSpacing(true);
        canvasTools.getStyle()
                .set("gap", "0.75rem")
                .set("flex-wrap", "wrap");

        Span zoomLabel = new Span("Zoom");
        zoomLabel.getStyle()
                .set("font-size", "0.82rem")
                .set("font-weight", "850")
                .set("color", "#475569")
                .set("align-self", "center");

        HorizontalLayout zoomTools = new HorizontalLayout(
                zoomLabel,
                zoom75Button,
                zoom100Button,
                zoom125Button,
                zoom150Button
        );
        zoomTools.setPadding(false);
        zoomTools.setSpacing(true);
        zoomTools.getStyle()
                .set("gap", "0.45rem")
                .set("flex-wrap", "wrap");

        Div toolbarWrapper = new Div(canvasTools, zoomTools);
        toolbarWrapper.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("gap", "1rem")
                .set("flex-wrap", "wrap")
                .set("width", "100%");

        ViewFrame frame = new ViewFrame(title, text, toolbarWrapper, editorCanvas);
        frame.getStyle()
                .set("padding", "1.35rem 1.5rem");

        return frame;
    }

    private H3 createSectionTitle(String text) {
        H3 title = new H3(text);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.28rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.025em")
                .set("color", "#0f172a");
        return title;
    }

    private Paragraph createSectionText(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.getStyle()
                .set("margin", "0")
                .set("color", "#64748b")
                .set("font-size", "0.94rem")
                .set("line-height", "1.5");
        return paragraph;
    }

    private void configureOfficeField() {
        officeField.setWidth("360px");
        officeField.setPlaceholder("Büro / Standort auswählen");
        officeField.setItemLabelGenerator(office -> {
            String label = office.getName();

            if (!office.isActive()) {
                label += " (inaktiv)";
            }

            return label;
        });

        officeField.addValueChangeListener(event -> {
            currentTool = EditorTool.SELECT;
            updateToolButtonStyles();
            renderOfficeCanvas(event.getValue());
        });

        styleInputField(officeField);
    }

    private void configureToolButtons() {
        selectionModeButton.addClickListener(event -> {
            currentTool = EditorTool.SELECT;
            updateToolButtonStyles();
            renderOfficeCanvas(officeField.getValue());
        });

        roomModeButton.addClickListener(event -> {
            if (officeField.getValue() == null) {
                Notification.show("Bitte zuerst ein Büro auswählen.");
                return;
            }

            currentTool = EditorTool.ROOM;
            updateToolButtonStyles();
            renderOfficeCanvas(officeField.getValue());

            Notification.show("Raum-Werkzeug aktiv. Klicke auf die Bürofläche.");
        });

        deskModeButton.addClickListener(event -> {
            if (officeField.getValue() == null) {
                Notification.show("Bitte zuerst ein Büro auswählen.");
                return;
            }

            currentTool = EditorTool.DESK;
            updateToolButtonStyles();
            renderOfficeCanvas(officeField.getValue());

            Notification.show("Tisch-Werkzeug aktiv. Klicke in einen Raum.");
        });

        gridModeButton.addClickListener(event -> {
            snapToGridEnabled = !snapToGridEnabled;
            updateToolButtonStyles();
            renderOfficeCanvas(officeField.getValue());
        });

        zoom75Button.addClickListener(event -> changeZoom(75));
        zoom100Button.addClickListener(event -> changeZoom(100));
        zoom125Button.addClickListener(event -> changeZoom(125));
        zoom150Button.addClickListener(event -> changeZoom(150));

        updateToolButtonStyles();
    }

    private void changeZoom(int zoomPercent) {
        currentZoomPercent = zoomPercent;
        updateToolButtonStyles();
        renderOfficeCanvas(officeField.getValue());
    }

    private void updateToolButtonStyles() {
        styleToolButton(selectionModeButton, currentTool == EditorTool.SELECT);
        styleToolButton(roomModeButton, currentTool == EditorTool.ROOM);
        styleToolButton(deskModeButton, currentTool == EditorTool.DESK);

        gridModeButton.setText(snapToGridEnabled ? "Raster: AN" : "Raster: AUS");
        styleToolButton(gridModeButton, snapToGridEnabled);

        styleZoomButton(zoom75Button, currentZoomPercent == 75);
        styleZoomButton(zoom100Button, currentZoomPercent == 100);
        styleZoomButton(zoom125Button, currentZoomPercent == 125);
        styleZoomButton(zoom150Button, currentZoomPercent == 150);
    }

    private void configureCanvas() {
        editorCanvas.setWidthFull();
        editorCanvas.getStyle()
                .set("min-height", "620px")
                .set("overflow", "auto");
    }

    private int snapIfEnabled(int value) {
        if (!snapToGridEnabled) {
            return value;
        }

        return Math.round(value / (float) GRID_SIZE) * GRID_SIZE;
    }

    private double getZoomScale() {
        return currentZoomPercent / 100.0;
    }

    private String getOfficeCanvasBackground() {
        if (!snapToGridEnabled) {
            return "linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%)";
        }

        return "repeating-linear-gradient(to right, rgba(37, 99, 235, 0.10) 0 1px, transparent 1px " + GRID_SIZE + "px), " +
                "repeating-linear-gradient(to bottom, rgba(37, 99, 235, 0.10) 0 1px, transparent 1px " + GRID_SIZE + "px), " +
                "linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%)";
    }

    private void renderOfficeCanvas(Office office) {
        editorCanvas.removeAll();

        if (office == null) {
            Div placeholder = new Div();
            placeholder.setText("Bitte wähle zuerst ein Büro aus oder lege ein neues Büro an.");
            placeholder.getStyle()
                    .set("padding", "1.15rem")
                    .set("border-radius", "18px")
                    .set("background", "#f8fafc")
                    .set("border", "1px dashed #cbd5e1")
                    .set("color", "#64748b")
                    .set("font-weight", "700");

            editorCanvas.add(placeholder);
            return;
        }

        int width = office.getLayoutWidth() != null ? office.getLayoutWidth() : DEFAULT_OFFICE_WIDTH;
        int height = office.getLayoutHeight() != null ? office.getLayoutHeight() : DEFAULT_OFFICE_HEIGHT;

        Div canvasWrapper = new Div();
        canvasWrapper.getStyle()
                .set("width", "fit-content")
                .set("min-width", "100%")
                .set("padding", "0.25rem")
                .set("box-sizing", "border-box");

        ResizableOfficeCanvas officeArea = new ResizableOfficeCanvas(office);
        officeArea.getStyle()
                .set("position", "relative")
                .set("box-sizing", "border-box")
                .set("width", width + "px")
                .set("height", height + "px")
                .set("zoom", currentZoomPercent + "%")
                .set("background", getOfficeCanvasBackground())
                .set("border", "2px solid #0f172a")
                .set("border-radius", "24px")
                .set("box-shadow", "inset 0 0 0 1px #eef2f7, 0 18px 45px rgba(15, 23, 42, 0.10)")
                .set("overflow", "hidden")
                .set("cursor", currentTool == EditorTool.ROOM ? "crosshair" : "default");

        officeArea.add(createOfficeTitleBadge(office));
        officeArea.add(createOfficeResizeHandle());

        if (currentTool == EditorTool.ROOM) {
            officeArea.add(createCanvasHint("Raum-Werkzeug aktiv: Klicke auf die Fläche, um einen Raum zu erstellen.", "62px"));
        }

        if (currentTool == EditorTool.DESK) {
            officeArea.add(createCanvasHint("Tisch-Werkzeug aktiv: Klicke in einen Raum, um einen Tisch zu erstellen.", "62px"));
        }

        List<Room> rooms = roomService.getRoomsByOfficeId(office.getId());

        for (Room room : rooms) {
            officeArea.add(createRoomComponent(room));
        }

        officeArea.getElement()
                .addEventListener("click", this::handleOfficeCanvasClick)
                .addEventData("event.offsetX")
                .addEventData("event.offsetY");

        officeArea.enableResize();

        canvasWrapper.add(officeArea);
        editorCanvas.add(canvasWrapper);
    }

    private Span createOfficeTitleBadge(Office office) {
        Span badge = new Span(office.getName());
        badge.getStyle()
                .set("position", "absolute")
                .set("top", "16px")
                .set("left", "16px")
                .set("padding", "0.5rem 0.85rem")
                .set("border-radius", "999px")
                .set("font-size", "0.82rem")
                .set("font-weight", "850")
                .set("background", "#0f172a")
                .set("color", "#ffffff")
                .set("box-shadow", "0 10px 24px rgba(15, 23, 42, 0.18)")
                .set("z-index", "5")
                .set("pointer-events", "none");
        return badge;
    }

    private Div createOfficeResizeHandle() {
        Div resizeHandle = new Div();
        resizeHandle.addClassName("office-resize-handle");

        resizeHandle.getStyle()
                .set("position", "absolute")
                .set("right", "12px")
                .set("bottom", "12px")
                .set("width", "22px")
                .set("height", "22px")
                .set("border-radius", "8px")
                .set("background", "#0f172a")
                .set("border", "2px solid #ffffff")
                .set("box-shadow", "0 10px 22px rgba(15, 23, 42, 0.28)")
                .set("cursor", "nwse-resize")
                .set("z-index", "50");

        resizeHandle.getElement().setProperty("title", "Bürogröße ändern");

        return resizeHandle;
    }

    private Span createCanvasHint(String text, String top) {
        Span hint = new Span(text);

        hint.getStyle()
                .set("position", "absolute")
                .set("left", "16px")
                .set("top", top)
                .set("padding", "0.5rem 0.85rem")
                .set("border-radius", "999px")
                .set("font-size", "0.8rem")
                .set("font-weight", "800")
                .set("background", "#dbeafe")
                .set("color", "#1d4ed8")
                .set("border", "1px solid #bfdbfe")
                .set("z-index", "5")
                .set("pointer-events", "none");

        return hint;
    }

    private void handleOfficeCanvasClick(DomEvent event) {
        if (currentTool != EditorTool.ROOM) {
            return;
        }

        Office selectedOffice = officeField.getValue();

        if (selectedOffice == null) {
            Notification.show("Bitte zuerst ein Büro auswählen.");
            return;
        }

        JsonNode eventData = event.getEventData();

        int offsetX = (int) Math.round(eventData.get("event.offsetX").asDouble());
        int offsetY = (int) Math.round(eventData.get("event.offsetY").asDouble());

        int officeWidth = selectedOffice.getLayoutWidth() != null ? selectedOffice.getLayoutWidth() : DEFAULT_OFFICE_WIDTH;
        int officeHeight = selectedOffice.getLayoutHeight() != null ? selectedOffice.getLayoutHeight() : DEFAULT_OFFICE_HEIGHT;

        int x = snapIfEnabled(Math.max(0, Math.min(offsetX, officeWidth - DEFAULT_ROOM_WIDTH)));
        int y = snapIfEnabled(Math.max(0, Math.min(offsetY, officeHeight - DEFAULT_ROOM_HEIGHT)));

        openCreateRoomDialogAt(x, y);
    }

    private Div createRoomComponent(Room room) {
        DraggableResizableRoomBox roomBox = new DraggableResizableRoomBox(room);

        int x = room.getPosX() != null ? room.getPosX() : 20;
        int y = room.getPosY() != null ? room.getPosY() : 20;
        int width = room.getLayoutWidth() != null ? room.getLayoutWidth() : DEFAULT_ROOM_WIDTH;
        int height = room.getLayoutHeight() != null ? room.getLayoutHeight() : DEFAULT_ROOM_HEIGHT;

        Div roomHeader = new Div();
        roomHeader.addClassName("room-header");
        roomHeader.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("right", "0")
                .set("height", "42px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("gap", "0.5rem")
                .set("padding", "0.45rem 0.55rem")
                .set("box-sizing", "border-box")
                .set("background", "rgba(15, 23, 42, 0.92)")
                .set("color", "#ffffff")
                .set("border-radius", "18px 18px 0 0")
                .set("z-index", "12");

        Span roomTitle = new Span(room.getName() != null && !room.getName().isBlank()
                ? room.getName()
                : "Unbenannter Raum");
        roomTitle.getStyle()
                .set("font-size", "0.86rem")
                .set("font-weight", "850")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        HorizontalLayout roomActions = new HorizontalLayout(
                createEditRoomButton(room),
                createDeleteRoomButton(room)
        );
        roomActions.setPadding(false);
        roomActions.setSpacing(false);
        roomActions.getStyle().set("gap", "0.35rem");

        roomHeader.add(roomTitle, roomActions);

        Span roomHint = new Span(currentTool == EditorTool.DESK ? "klicken = Tisch erstellen" : "ziehen · unten rechts skalieren");
        roomHint.getStyle()
                .set("position", "absolute")
                .set("left", "12px")
                .set("bottom", "12px")
                .set("font-size", "0.72rem")
                .set("font-weight", "750")
                .set("color", "#64748b")
                .set("background", "rgba(248, 250, 252, 0.88)")
                .set("border", "1px solid #e2e8f0")
                .set("padding", "0.28rem 0.55rem")
                .set("border-radius", "999px")
                .set("z-index", "8")
                .set("pointer-events", "none");

        Div resizeHandle = new Div();
        resizeHandle.addClassName("room-resize-handle");
        resizeHandle.getStyle()
                .set("position", "absolute")
                .set("right", "8px")
                .set("bottom", "8px")
                .set("width", "18px")
                .set("height", "18px")
                .set("border-radius", "6px")
                .set("background", "#2563eb")
                .set("box-shadow", "0 8px 16px rgba(37, 99, 235, 0.25)")
                .set("cursor", currentTool == EditorTool.SELECT ? "nwse-resize" : "default")
                .set("z-index", "12");

        roomBox.getStyle()
                .set("position", "absolute")
                .set("box-sizing", "border-box")
                .set("left", x + "px")
                .set("top", y + "px")
                .set("width", width + "px")
                .set("height", height + "px")
                .set("background", "rgba(255, 255, 255, 0.96)")
                .set("border", room.isActive() ? "2px solid #2563eb" : "2px solid #94a3b8")
                .set("border-radius", "20px")
                .set("box-shadow", "0 18px 38px rgba(15, 23, 42, 0.12)")
                .set("overflow", "hidden")
                .set("cursor", getRoomCursor())
                .set("user-select", "none")
                .set("z-index", "2");

        if (!room.isActive()) {
            roomBox.getStyle()
                    .set("opacity", "0.75")
                    .set("background", "#f1f5f9");
        }

        roomBox.getElement().setProperty(
                "title",
                "Raum: " + room.getName()
                        + "\nX: " + x
                        + "\nY: " + y
                        + "\nBreite: " + width
                        + "\nHöhe: " + height
        );

        roomBox.add(roomHeader);

        List<Resource> desks = resourceService.getAllResources().stream()
                .filter(resource -> resource.getResourceType() == ResourceType.DESK)
                .filter(resource -> resource.getRoom() != null)
                .filter(resource -> resource.getRoom().getId().equals(room.getId()))
                .toList();

        for (Resource desk : desks) {
            roomBox.add(createDeskComponent(desk));
        }

        roomBox.add(roomHint, resizeHandle);
        roomBox.enableInteractions();

        return roomBox;
    }

    private String getRoomCursor() {
        if (currentTool == EditorTool.DESK) {
            return "crosshair";
        }

        if (currentTool == EditorTool.SELECT) {
            return "grab";
        }

        return "default";
    }

    private Div createDeskComponent(Resource desk) {
        DraggableResizableDeskBox deskBox = new DraggableResizableDeskBox(desk);
        deskBox.addClassName("desk-box");

        Span deskName = new Span(desk.getName() != null && !desk.getName().isBlank()
                ? desk.getName()
                : "Tisch");
        deskName.getStyle()
                .set("font-weight", "850")
                .set("line-height", "1.1")
                .set("max-width", "82px")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        Span status = new Span(getDeskAdminStatusText(desk));
        status.getStyle()
                .set("font-size", "0.68rem")
                .set("font-weight", "750")
                .set("opacity", "0.88");

        VerticalLayout content = new VerticalLayout(deskName, status);
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(Alignment.CENTER);
        content.getStyle()
                .set("gap", "0.15rem")
                .set("width", "100%")
                .set("pointer-events", "none");

        Button editButton = createEditDeskButton(desk);
        Button deleteButton = createDeleteDeskButton(desk);

        Div resizeHandle = new Div();
        resizeHandle.addClassName("desk-resize-handle");
        resizeHandle.getStyle()
                .set("position", "absolute")
                .set("right", "5px")
                .set("bottom", "5px")
                .set("width", "13px")
                .set("height", "13px")
                .set("border-radius", "5px")
                .set("background", "#2563eb")
                .set("box-shadow", "0 6px 12px rgba(37, 99, 235, 0.25)")
                .set("cursor", currentTool == EditorTool.SELECT ? "nwse-resize" : "default")
                .set("z-index", "20");

        int x = desk.getPosX() != null ? desk.getPosX() : 0;
        int y = desk.getPosY() != null ? desk.getPosY() : 48;
        int width = desk.getWidth() != null ? desk.getWidth() : DEFAULT_DESK_WIDTH;
        int height = desk.getHeight() != null ? desk.getHeight() : DEFAULT_DESK_HEIGHT;

        deskBox.getStyle()
                .set("position", "absolute")
                .set("box-sizing", "border-box")
                .set("left", x + "px")
                .set("top", y + "px")
                .set("width", width + "px")
                .set("height", height + "px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("text-align", "center")
                .set("padding", "0.45rem")
                .set("font-size", "0.82rem")
                .set("font-weight", "700")
                .set("border-radius", "18px")
                .set("box-shadow", "0 12px 28px rgba(15, 23, 42, 0.10)")
                .set("cursor", currentTool == EditorTool.SELECT ? "grab" : "default")
                .set("user-select", "none")
                .set("z-index", "6");

        applyAdminDeskStyle(deskBox, desk);

        deskBox.getElement().setProperty(
                "title",
                "Tisch: " + desk.getName()
                        + "\nX: " + x
                        + "\nY: " + y
                        + "\nBreite: " + width
                        + "\nHöhe: " + height
        );

        deskBox.add(content, editButton, deleteButton, resizeHandle);
        deskBox.enableInteractions();

        return deskBox;
    }

    private String getDeskAdminStatusText(Resource desk) {
        if (!desk.isActive()) {
            return "inaktiv";
        }

        if (!desk.isBookable()) {
            return "gesperrt";
        }

        return "buchbar";
    }

    private void applyAdminDeskStyle(Div deskBox, Resource desk) {
        if (!desk.isActive()) {
            deskBox.getStyle()
                    .set("color", "#334155")
                    .set("background", "linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%)")
                    .set("border", "2px solid #64748b")
                    .set("opacity", "0.95");
            return;
        }

        if (!desk.isBookable()) {
            deskBox.getStyle()
                    .set("color", "#78350f")
                    .set("background", "linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)")
                    .set("border", "2px solid #f59e0b");
            return;
        }

        deskBox.getStyle()
                .set("color", "#172554")
                .set("background", "linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%)")
                .set("border", "2px solid #2563eb");
    }

    private Button createEditRoomButton(Room room) {
        Button button = new Button("✎");
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.getStyle()
                .set("width", "26px")
                .set("height", "26px")
                .set("min-width", "26px")
                .set("padding", "0")
                .set("border-radius", "999px")
                .set("font-size", "0.9rem")
                .set("font-weight", "850")
                .set("background", "#ffffff")
                .set("color", "#2563eb")
                .set("border", "1px solid #bfdbfe")
                .set("cursor", "pointer");

        button.getElement().setProperty("title", "Raum bearbeiten");
        stopPointerAndClickPropagation(button);
        button.addClickListener(event -> openEditRoomDialog(room));

        return button;
    }

    private Button createDeleteRoomButton(Room room) {
        Button button = new Button("×");
        button.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        button.getStyle()
                .set("width", "26px")
                .set("height", "26px")
                .set("min-width", "26px")
                .set("padding", "0")
                .set("border-radius", "999px")
                .set("font-size", "1rem")
                .set("font-weight", "850")
                .set("cursor", "pointer");

        button.getElement().setProperty("title", "Raum löschen");
        stopPointerAndClickPropagation(button);
        button.addClickListener(event -> openDeleteRoomDialog(room));

        return button;
    }

    private Button createEditDeskButton(Resource desk) {
        Button button = new Button("✎");
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.getStyle()
                .set("position", "absolute")
                .set("top", "-10px")
                .set("left", "-10px")
                .set("width", "25px")
                .set("height", "25px")
                .set("min-width", "25px")
                .set("padding", "0")
                .set("border-radius", "999px")
                .set("font-size", "0.9rem")
                .set("font-weight", "850")
                .set("background", "#ffffff")
                .set("color", "#2563eb")
                .set("border", "1px solid #bfdbfe")
                .set("box-shadow", "0 8px 16px rgba(37, 99, 235, 0.22)")
                .set("cursor", "pointer")
                .set("z-index", "30");

        button.getElement().setProperty("title", "Tisch bearbeiten");
        stopPointerAndClickPropagation(button);
        button.addClickListener(event -> openEditDeskDialog(desk));

        return button;
    }

    private Button createDeleteDeskButton(Resource desk) {
        Button button = new Button("×");
        button.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        button.getStyle()
                .set("position", "absolute")
                .set("top", "-10px")
                .set("right", "-10px")
                .set("width", "25px")
                .set("height", "25px")
                .set("min-width", "25px")
                .set("padding", "0")
                .set("border-radius", "999px")
                .set("font-size", "1rem")
                .set("font-weight", "850")
                .set("box-shadow", "0 8px 16px rgba(220, 38, 38, 0.25)")
                .set("cursor", "pointer")
                .set("z-index", "30");

        button.getElement().setProperty("title", "Tisch löschen");
        stopPointerAndClickPropagation(button);
        button.addClickListener(event -> openDeleteDeskDialog(desk));

        return button;
    }

    private String getNextRoomNameForSelectedOffice() {
        Office selectedOffice = officeField.getValue();

        if (selectedOffice == null) {
            return "Raum 1";
        }

        List<String> existingNames = roomService.getRoomsByOfficeId(selectedOffice.getId()).stream()
                .map(room -> room.getName() != null ? room.getName() : "")
                .toList();

        return getNextNumberedName("Raum", existingNames);
    }

    private String getNextDeskNameForRoom(Room room) {
        if (room == null || room.getId() == null) {
            return "Tisch 1";
        }

        List<String> existingNames = resourceService.getAllResources().stream()
                .filter(resource -> resource.getResourceType() == ResourceType.DESK)
                .filter(resource -> resource.getRoom() != null)
                .filter(resource -> resource.getRoom().getId().equals(room.getId()))
                .map(resource -> resource.getName() != null ? resource.getName() : "")
                .toList();

        return getNextNumberedName("Tisch", existingNames);
    }

    private String getNextNumberedName(String prefix, List<String> existingNames) {
        int counter = 1;

        while (existingNames.contains(prefix + " " + counter)) {
            counter++;
        }

        return prefix + " " + counter;
    }

    private void openCreateOfficeDialog() {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Büro anlegen");
        Paragraph text = createDialogText("Lege einen neuen Standort oder Bürokomplex an.");

        TextField nameField = new TextField("Büroname");
        nameField.setWidthFull();
        nameField.setPlaceholder("z. B. Büro Duisburg");

        TextArea descriptionField = new TextArea("Beschreibung");
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("100px");
        descriptionField.setPlaceholder("Optional");

        Checkbox activeField = new Checkbox("Büro ist aktiv");
        activeField.setValue(true);

        styleInputField(nameField);
        styleInputField(descriptionField);
        styleCheckbox(activeField);

        FormLayout formLayout = new FormLayout(nameField, descriptionField, activeField);
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        formLayout.setColspan(nameField, 2);
        formLayout.setColspan(descriptionField, 2);
        formLayout.setColspan(activeField, 2);

        Button saveButton = new Button("Anlegen");
        stylePrimaryButton(saveButton);

        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        styleSecondaryButton(cancelButton);

        saveButton.addClickListener(event -> {
            String name = nameField.getValue().trim();

            if (name.isBlank()) {
                Notification.show("Bitte einen Büronamen eingeben.");
                return;
            }

            Office createdOffice = officeService.createOffice(
                    name,
                    descriptionField.getValue().trim(),
                    activeField.getValue(),
                    DEFAULT_OFFICE_WIDTH,
                    DEFAULT_OFFICE_HEIGHT
            );

            dialog.close();
            Notification.show("Büro wurde angelegt.");
            refreshOfficeSelector(createdOffice);
        });

        HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        VerticalLayout content = createDialogContent(title, text, formLayout, actions);
        dialog.add(content);
        dialog.open();
    }

    private void openEditOfficeDialog() {
        Office selectedOffice = officeField.getValue();

        if (selectedOffice == null) {
            Notification.show("Bitte zuerst ein Büro auswählen.");
            return;
        }

        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Büro bearbeiten");
        Paragraph text = createDialogText("Name, Beschreibung und Status des Büros bearbeiten.");

        TextField nameField = new TextField("Büroname");
        nameField.setWidthFull();
        nameField.setValue(selectedOffice.getName() != null ? selectedOffice.getName() : "");

        TextArea descriptionField = new TextArea("Beschreibung");
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("100px");
        descriptionField.setValue(selectedOffice.getDescription() != null ? selectedOffice.getDescription() : "");

        Checkbox activeField = new Checkbox("Büro ist aktiv");
        activeField.setValue(selectedOffice.isActive());

        styleInputField(nameField);
        styleInputField(descriptionField);
        styleCheckbox(activeField);

        FormLayout formLayout = new FormLayout(nameField, descriptionField, activeField);
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        formLayout.setColspan(nameField, 2);
        formLayout.setColspan(descriptionField, 2);
        formLayout.setColspan(activeField, 2);

        Button saveButton = new Button("Speichern");
        stylePrimaryButton(saveButton);

        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        styleSecondaryButton(cancelButton);

        saveButton.addClickListener(event -> {
            String name = nameField.getValue().trim();

            if (name.isBlank()) {
                Notification.show("Bitte einen Büronamen eingeben.");
                return;
            }

            officeService.updateOffice(
                    selectedOffice.getId(),
                    name,
                    descriptionField.getValue().trim(),
                    activeField.getValue(),
                    selectedOffice.getLayoutWidth() != null ? selectedOffice.getLayoutWidth() : DEFAULT_OFFICE_WIDTH,
                    selectedOffice.getLayoutHeight() != null ? selectedOffice.getLayoutHeight() : DEFAULT_OFFICE_HEIGHT
            ).ifPresentOrElse(updatedOffice -> {
                dialog.close();
                Notification.show("Büro wurde aktualisiert.");
                refreshOfficeSelector(updatedOffice);
                renderOfficeCanvas(updatedOffice);
            }, () -> Notification.show("Büro konnte nicht aktualisiert werden."));
        });

        HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        VerticalLayout content = createDialogContent(title, text, formLayout, actions);
        dialog.add(content);
        dialog.open();
    }

    private void openDeleteOfficeDialog() {
        Office selectedOffice = officeField.getValue();

        if (selectedOffice == null) {
            Notification.show("Bitte zuerst ein Büro auswählen.");
            return;
        }

        openDeleteConfirmationDialog(
                "Büro löschen",
                "Möchtest du das Büro \"" + selectedOffice.getName() + "\" wirklich löschen? " +
                        "Alle Räume, Tische und Buchungen in diesem Büro werden ebenfalls gelöscht.",
                "Büro löschen",
                () -> deleteSelectedOffice(selectedOffice)
        );
    }

    private void deleteSelectedOffice(Office office) {
        boolean deleted = officeService.deleteOffice(office.getId());

        if (deleted) {
            Notification.show("Büro wurde gelöscht.");
            refreshOfficeSelector(null);
            renderOfficeCanvas(null);
        } else {
            Notification.show("Büro konnte nicht gelöscht werden.");
        }
    }

    private void openCreateRoomDialogAt(int posX, int posY) {
        Office selectedOffice = officeField.getValue();

        if (selectedOffice == null) {
            Notification.show("Bitte zuerst ein Büro auswählen.");
            return;
        }

        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Raum anlegen");
        Paragraph text = createDialogText("Lege den Raum an dieser Position im Büroplan an.");

        TextField nameField = new TextField("Raumname");
        nameField.setWidthFull();
        nameField.setPlaceholder("z. B. Raum 1");
        nameField.setValue(getNextRoomNameForSelectedOffice());

        TextArea descriptionField = new TextArea("Beschreibung");
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("100px");
        descriptionField.setPlaceholder("Optional");

        Checkbox activeField = new Checkbox("Raum ist aktiv");
        activeField.setValue(true);

        styleInputField(nameField);
        styleInputField(descriptionField);
        styleCheckbox(activeField);

        FormLayout formLayout = new FormLayout(nameField, descriptionField, activeField);
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        formLayout.setColspan(nameField, 2);
        formLayout.setColspan(descriptionField, 2);
        formLayout.setColspan(activeField, 2);

        Button saveButton = new Button("Raum anlegen");
        stylePrimaryButton(saveButton);

        Button cancelButton = new Button("Abbrechen", event -> {
            dialog.close();
            currentTool = EditorTool.SELECT;
            updateToolButtonStyles();
            renderSelectedOffice();
        });
        styleSecondaryButton(cancelButton);

        saveButton.addClickListener(event -> {
            String name = nameField.getValue().trim();

            if (name.isBlank()) {
                Notification.show("Bitte einen Raumnamen eingeben.");
                return;
            }

            roomService.createRoom(
                    selectedOffice.getId(),
                    name,
                    descriptionField.getValue().trim(),
                    activeField.getValue(),
                    posX,
                    posY,
                    DEFAULT_ROOM_WIDTH,
                    DEFAULT_ROOM_HEIGHT
            ).ifPresentOrElse(createdRoom -> {
                dialog.close();
                Notification.show("Raum wurde erstellt.");
                currentTool = EditorTool.SELECT;
                updateToolButtonStyles();
                renderSelectedOffice();
            }, () -> Notification.show("Raum konnte nicht erstellt werden."));
        });

        HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        VerticalLayout content = createDialogContent(title, text, formLayout, actions);
        dialog.add(content);
        dialog.open();
    }

    private void openEditRoomDialog(Room room) {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Raum bearbeiten");
        Paragraph text = createDialogText("Name, Beschreibung und Status des Raums bearbeiten.");

        TextField nameField = new TextField("Raumname");
        nameField.setWidthFull();
        nameField.setValue(room.getName() != null ? room.getName() : "");

        TextArea descriptionField = new TextArea("Beschreibung");
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("100px");
        descriptionField.setValue(room.getDescription() != null ? room.getDescription() : "");

        Checkbox activeField = new Checkbox("Raum ist aktiv");
        activeField.setValue(room.isActive());

        styleInputField(nameField);
        styleInputField(descriptionField);
        styleCheckbox(activeField);

        FormLayout formLayout = new FormLayout(nameField, descriptionField, activeField);
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        formLayout.setColspan(nameField, 2);
        formLayout.setColspan(descriptionField, 2);
        formLayout.setColspan(activeField, 2);

        Button saveButton = new Button("Speichern");
        stylePrimaryButton(saveButton);

        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        styleSecondaryButton(cancelButton);

        saveButton.addClickListener(event -> {
            Office selectedOffice = officeField.getValue();

            if (selectedOffice == null) {
                Notification.show("Bitte zuerst ein Büro auswählen.");
                return;
            }

            String name = nameField.getValue().trim();

            if (name.isBlank()) {
                Notification.show("Bitte einen Raumnamen eingeben.");
                return;
            }

            int roomPosX = room.getPosX() != null ? room.getPosX() : 20;
            int roomPosY = room.getPosY() != null ? room.getPosY() : 20;
            int roomWidth = room.getLayoutWidth() != null ? room.getLayoutWidth() : DEFAULT_ROOM_WIDTH;
            int roomHeight = room.getLayoutHeight() != null ? room.getLayoutHeight() : DEFAULT_ROOM_HEIGHT;

            roomService.updateRoom(
                    room.getId(),
                    selectedOffice.getId(),
                    name,
                    descriptionField.getValue().trim(),
                    activeField.getValue(),
                    roomPosX,
                    roomPosY,
                    roomWidth,
                    roomHeight
            ).ifPresentOrElse(updatedRoom -> {
                dialog.close();
                Notification.show("Raum wurde aktualisiert.");
                renderSelectedOffice();
            }, () -> Notification.show("Raum konnte nicht aktualisiert werden."));
        });

        HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        VerticalLayout content = createDialogContent(title, text, formLayout, actions);
        dialog.add(content);
        dialog.open();
    }

    private void openDeleteRoomDialog(Room room) {
        openDeleteConfirmationDialog(
                "Raum löschen",
                "Möchtest du den Raum \"" + room.getName() + "\" wirklich löschen? " +
                        "Alle Tische und Buchungen in diesem Raum werden ebenfalls gelöscht.",
                "Raum löschen",
                () -> deleteRoom(room)
        );
    }

    private void deleteRoom(Room room) {
        boolean deleted = roomService.deleteRoom(room.getId());

        if (deleted) {
            Notification.show("Raum wurde gelöscht.");
            renderSelectedOffice();
        } else {
            Notification.show("Raum konnte nicht gelöscht werden.");
        }
    }

    private void openCreateDeskDialogAt(Room room, int posX, int posY) {
        if (room == null) {
            Notification.show("Der Tisch kann keinem Raum zugeordnet werden.");
            return;
        }

        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Tisch anlegen");
        Paragraph text = createDialogText("Lege den Tisch an dieser Position im Raum \"" + room.getName() + "\" an.");

        TextField nameField = new TextField("Tischname");
        nameField.setWidthFull();
        nameField.setPlaceholder("z. B. Tisch 1");
        nameField.setValue(getNextDeskNameForRoom(room));

        TextArea descriptionField = new TextArea("Beschreibung");
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("100px");
        descriptionField.setPlaceholder("Optional");

        Checkbox activeField = new Checkbox("Tisch ist aktiv");
        activeField.setValue(true);

        Checkbox bookableField = new Checkbox("Tisch ist buchbar");
        bookableField.setValue(true);

        styleInputField(nameField);
        styleInputField(descriptionField);
        styleCheckbox(activeField);
        styleCheckbox(bookableField);

        FormLayout formLayout = new FormLayout(nameField, descriptionField, activeField, bookableField);
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        formLayout.setColspan(nameField, 2);
        formLayout.setColspan(descriptionField, 2);

        Button saveButton = new Button("Tisch anlegen");
        stylePrimaryButton(saveButton);

        Button cancelButton = new Button("Abbrechen", event -> {
            dialog.close();
            currentTool = EditorTool.SELECT;
            updateToolButtonStyles();
            renderSelectedOffice();
        });
        styleSecondaryButton(cancelButton);

        saveButton.addClickListener(event -> {
            String name = nameField.getValue().trim();

            if (name.isBlank()) {
                Notification.show("Bitte einen Tischnamen eingeben.");
                return;
            }

            boolean created = resourceService.createResource(
                    name,
                    descriptionField.getValue().trim(),
                    ResourceType.DESK,
                    room.getId(),
                    activeField.getValue(),
                    bookableField.getValue(),
                    posX,
                    posY,
                    DEFAULT_DESK_WIDTH,
                    DEFAULT_DESK_HEIGHT
            ).isPresent();

            if (created) {
                dialog.close();
                Notification.show("Tisch wurde erstellt.");
                currentTool = EditorTool.SELECT;
                updateToolButtonStyles();
                renderSelectedOffice();
            } else {
                Notification.show("Tisch konnte nicht erstellt werden.");
            }
        });

        HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        VerticalLayout content = createDialogContent(title, text, formLayout, actions);
        dialog.add(content);
        dialog.open();
    }

    private void openEditDeskDialog(Resource desk) {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Tisch bearbeiten");
        Paragraph text = createDialogText("Name, Beschreibung und Status des Tisches bearbeiten.");

        TextField nameField = new TextField("Tischname");
        nameField.setWidthFull();
        nameField.setValue(desk.getName() != null ? desk.getName() : "");

        TextArea descriptionField = new TextArea("Beschreibung");
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("100px");
        descriptionField.setValue(desk.getDescription() != null ? desk.getDescription() : "");

        Checkbox activeField = new Checkbox("Tisch ist aktiv");
        activeField.setValue(desk.isActive());

        Checkbox bookableField = new Checkbox("Tisch ist buchbar");
        bookableField.setValue(desk.isBookable());

        styleInputField(nameField);
        styleInputField(descriptionField);
        styleCheckbox(activeField);
        styleCheckbox(bookableField);

        FormLayout formLayout = new FormLayout(nameField, descriptionField, activeField, bookableField);
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        formLayout.setColspan(nameField, 2);
        formLayout.setColspan(descriptionField, 2);

        Button saveButton = new Button("Speichern");
        stylePrimaryButton(saveButton);

        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        styleSecondaryButton(cancelButton);

        saveButton.addClickListener(event -> {
            String name = nameField.getValue().trim();

            if (name.isBlank()) {
                Notification.show("Bitte einen Tischnamen eingeben.");
                return;
            }

            Room room = desk.getRoom();

            if (room == null) {
                Notification.show("Der Tisch ist keinem Raum zugeordnet.");
                return;
            }

            resourceService.updateResource(
                    desk.getId(),
                    name,
                    descriptionField.getValue().trim(),
                    ResourceType.DESK,
                    room.getId(),
                    activeField.getValue(),
                    bookableField.getValue(),
                    desk.getPosX() != null ? desk.getPosX() : 0,
                    desk.getPosY() != null ? desk.getPosY() : 48,
                    desk.getWidth() != null ? desk.getWidth() : DEFAULT_DESK_WIDTH,
                    desk.getHeight() != null ? desk.getHeight() : DEFAULT_DESK_HEIGHT
            ).ifPresentOrElse(updatedDesk -> {
                dialog.close();
                Notification.show("Tisch wurde aktualisiert.");
                renderSelectedOffice();
            }, () -> Notification.show("Tisch konnte nicht aktualisiert werden."));
        });

        HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        VerticalLayout content = createDialogContent(title, text, formLayout, actions);
        dialog.add(content);
        dialog.open();
    }

    private void openDeleteDeskDialog(Resource desk) {
        openDeleteConfirmationDialog(
                "Tisch löschen",
                "Möchtest du den Tisch \"" + desk.getName() + "\" wirklich löschen? " +
                        "Vorhandene Buchungen für diesen Tisch werden ebenfalls gelöscht.",
                "Tisch löschen",
                () -> deleteDesk(desk)
        );
    }

    private void deleteDesk(Resource desk) {
        boolean deleted = resourceService.deleteResource(desk.getId());

        if (deleted) {
            Notification.show("Tisch wurde gelöscht.");
            renderSelectedOffice();
        } else {
            Notification.show("Tisch konnte nicht gelöscht werden.");
        }
    }

    private void openDeleteConfirmationDialog(String titleText,
                                              String messageText,
                                              String confirmButtonText,
                                              Runnable onConfirm) {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle(titleText);

        Paragraph message = createDialogText(messageText);
        message.getStyle()
                .set("margin", "0")
                .set("color", "#7f1d1d")
                .set("font-weight", "650")
                .set("line-height", "1.55");

        Div warningBox = new Div(message);
        warningBox.getStyle()
                .set("padding", "1rem")
                .set("border-radius", "18px")
                .set("background", "#fef2f2")
                .set("border", "1px solid #fecaca")
                .set("box-shadow", "inset 0 0 0 1px rgba(255,255,255,0.7)");

        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        styleSecondaryButton(cancelButton);

        Button confirmButton = new Button(confirmButtonText);
        confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        confirmButton.getStyle()
                .set("border-radius", "14px")
                .set("font-weight", "850")
                .set("padding", "0.62rem 1rem")
                .set("box-shadow", "0 12px 24px rgba(220, 38, 38, 0.18)");

        confirmButton.addClickListener(event -> {
            dialog.close();
            onConfirm.run();
        });

        HorizontalLayout actions = new HorizontalLayout(cancelButton, confirmButton);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setWidthFull();
        actions.getStyle()
                .set("justify-content", "flex-end")
                .set("gap", "0.75rem")
                .set("flex-wrap", "wrap");

        VerticalLayout content = createDialogContent(title, warningBox, actions);
        dialog.add(content);
        dialog.open();
    }

    private void refreshOfficeSelector(Office officeToSelect) {
        List<Office> offices = officeService.getAllOffices();
        officeField.setItems(offices);

        if (officeToSelect == null || officeToSelect.getId() == null) {
            officeField.clear();
            return;
        }

        offices.stream()
                .filter(office -> office.getId().equals(officeToSelect.getId()))
                .findFirst()
                .ifPresent(officeField::setValue);
    }

    private void renderSelectedOffice() {
        renderOfficeCanvas(officeField.getValue());
    }

    private class ResizableOfficeCanvas extends Div {

        private final Office office;

        private ResizableOfficeCanvas(Office office) {
            this.office = office;
        }

        private void enableResize() {
            getElement().executeJs("""
                    const officeArea = this;
                    const minOfficeWidth = $0;
                    const minOfficeHeight = $1;
                    const snapToGrid = $2;
                    const gridSize = $3;
                    const zoomScale = $4;

                    const snap = (value) => {
                        if (!snapToGrid) {
                            return Math.round(value);
                        }

                        return Math.round(value / gridSize) * gridSize;
                    };

                    if (officeArea.__officeResizeInitialized) {
                        return;
                    }

                    officeArea.__officeResizeInitialized = true;
                    officeArea.style.touchAction = 'none';

                    const resizeHandle = officeArea.querySelector('.office-resize-handle');

                    if (!resizeHandle) {
                        return;
                    }

                    resizeHandle.addEventListener('pointerdown', function(event) {
                        if (event.button !== 0) {
                            return;
                        }

                        event.preventDefault();
                        event.stopPropagation();

                        const startX = event.clientX;
                        const startY = event.clientY;
                        const startWidth = officeArea.offsetWidth;
                        const startHeight = officeArea.offsetHeight;

                        resizeHandle.setPointerCapture(event.pointerId);

                        const move = function(moveEvent) {
                            const deltaX = (moveEvent.clientX - startX) / zoomScale;
                            const deltaY = (moveEvent.clientY - startY) / zoomScale;

                            const newWidth = Math.max(minOfficeWidth, snap(startWidth + deltaX));
                            const newHeight = Math.max(minOfficeHeight, snap(startHeight + deltaY));

                            officeArea.style.width = newWidth + 'px';
                            officeArea.style.height = newHeight + 'px';
                        };

                        const up = function(upEvent) {
                            resizeHandle.removeEventListener('pointermove', move);
                            resizeHandle.removeEventListener('pointerup', up);
                            resizeHandle.removeEventListener('pointercancel', up);

                            const finalWidth = Math.round(officeArea.offsetWidth);
                            const finalHeight = Math.round(officeArea.offsetHeight);

                            officeArea.$server.saveOfficeSize(finalWidth, finalHeight);
                        };

                        resizeHandle.addEventListener('pointermove', move);
                        resizeHandle.addEventListener('pointerup', up);
                        resizeHandle.addEventListener('pointercancel', up);
                    });
                    """, MIN_OFFICE_WIDTH, MIN_OFFICE_HEIGHT, snapToGridEnabled, GRID_SIZE, getZoomScale());
        }

        @ClientCallable
        public void saveOfficeSize(int width, int height) {
            officeService.updateOffice(
                    office.getId(),
                    office.getName(),
                    office.getDescription(),
                    office.isActive(),
                    Math.max(snapIfEnabled(width), MIN_OFFICE_WIDTH),
                    Math.max(snapIfEnabled(height), MIN_OFFICE_HEIGHT)
            ).ifPresentOrElse(updatedOffice -> {
                Notification.show("Bürogröße gespeichert.");
                refreshOfficeSelector(updatedOffice);
                renderOfficeCanvas(updatedOffice);
            }, () -> Notification.show("Bürogröße konnte nicht gespeichert werden."));
        }
    }

    private class DraggableResizableRoomBox extends Div {

        private final Room room;

        private DraggableResizableRoomBox(Room room) {
            this.room = room;
        }

        private void enableInteractions() {
            getElement().executeJs("""
                    const roomBox = this;
                    const selectionMode = $0;
                    const deskToolMode = $1;
                    const minRoomWidth = $2;
                    const minRoomHeight = $3;
                    const defaultDeskWidth = $4;
                    const defaultDeskHeight = $5;
                    const snapToGrid = $6;
                    const gridSize = $7;
                    const zoomScale = $8;

                    const snap = (value) => {
                        if (!snapToGrid) {
                            return Math.round(value);
                        }

                        return Math.round(value / gridSize) * gridSize;
                    };

                    if (roomBox.__roomEditorInitialized) {
                        return;
                    }

                    roomBox.__roomEditorInitialized = true;
                    roomBox.style.touchAction = 'none';

                    const getParentBounds = () => {
                        const parent = roomBox.parentElement;
                        return {
                            parent,
                            rect: parent.getBoundingClientRect()
                        };
                    };

                    const saveLayout = () => {
                        const finalX = parseInt(roomBox.style.left, 10) || 0;
                        const finalY = parseInt(roomBox.style.top, 10) || 0;
                        const finalWidth = Math.round(roomBox.offsetWidth);
                        const finalHeight = Math.round(roomBox.offsetHeight);

                        roomBox.$server.saveLayout(finalX, finalY, finalWidth, finalHeight);
                    };

                    roomBox.addEventListener('click', function(event) {
                        event.stopPropagation();

                        if (!deskToolMode) {
                            return;
                        }

                        if (
                            event.target.closest('vaadin-button') ||
                            event.target.closest('.room-header') ||
                            event.target.closest('.room-resize-handle') ||
                            event.target.closest('.desk-resize-handle') ||
                            event.target.closest('.desk-box')
                        ) {
                            return;
                        }

                        const rect = roomBox.getBoundingClientRect();
                        const rawX = ((event.clientX - rect.left) / zoomScale) - (defaultDeskWidth / 2);
                        const rawY = ((event.clientY - rect.top) / zoomScale) - (defaultDeskHeight / 2);

                        const maxX = roomBox.clientWidth - defaultDeskWidth;
                        const maxY = roomBox.clientHeight - defaultDeskHeight;

                        const newX = Math.max(0, Math.min(maxX, snap(rawX)));
                        const newY = Math.max(44, Math.min(maxY, snap(rawY)));

                        roomBox.$server.createDeskAt(Math.round(newX), Math.round(newY));
                    });

                    if (selectionMode) {
                        roomBox.addEventListener('pointerdown', function(event) {
                            if (event.button !== 0) {
                                return;
                            }

                            if (
                                event.target.closest('vaadin-button') ||
                                event.target.closest('.room-resize-handle') ||
                                event.target.closest('.desk-box')
                            ) {
                                return;
                            }

                            event.preventDefault();
                            event.stopPropagation();

                            const { parent, rect } = getParentBounds();
                            const boxRect = roomBox.getBoundingClientRect();

                            const shiftX = (event.clientX - boxRect.left) / zoomScale;
                            const shiftY = (event.clientY - boxRect.top) / zoomScale;

                            roomBox.setPointerCapture(event.pointerId);
                            roomBox.style.cursor = 'grabbing';
                            roomBox.style.zIndex = '20';

                            const move = function(moveEvent) {
                                const rawX = ((moveEvent.clientX - rect.left) / zoomScale) - shiftX;
                                const rawY = ((moveEvent.clientY - rect.top) / zoomScale) - shiftY;

                                const maxX = parent.clientWidth - roomBox.offsetWidth;
                                const maxY = parent.clientHeight - roomBox.offsetHeight;

                                const newX = Math.max(0, Math.min(maxX, snap(rawX)));
                                const newY = Math.max(0, Math.min(maxY, snap(rawY)));

                                roomBox.style.left = newX + 'px';
                                roomBox.style.top = newY + 'px';
                            };

                            const up = function(upEvent) {
                                roomBox.removeEventListener('pointermove', move);
                                roomBox.removeEventListener('pointerup', up);
                                roomBox.removeEventListener('pointercancel', up);

                                roomBox.style.cursor = 'grab';
                                roomBox.style.zIndex = '2';

                                saveLayout();
                            };

                            roomBox.addEventListener('pointermove', move);
                            roomBox.addEventListener('pointerup', up);
                            roomBox.addEventListener('pointercancel', up);
                        });

                        const resizeHandle = roomBox.querySelector('.room-resize-handle');

                        if (resizeHandle && !resizeHandle.__resizeInitialized) {
                            resizeHandle.__resizeInitialized = true;

                            resizeHandle.addEventListener('pointerdown', function(event) {
                                if (event.button !== 0) {
                                    return;
                                }

                                event.preventDefault();
                                event.stopPropagation();

                                const { parent } = getParentBounds();
                                const startX = event.clientX;
                                const startY = event.clientY;
                                const startWidth = roomBox.offsetWidth;
                                const startHeight = roomBox.offsetHeight;
                                const startLeft = parseInt(roomBox.style.left, 10) || 0;
                                const startTop = parseInt(roomBox.style.top, 10) || 0;

                                resizeHandle.setPointerCapture(event.pointerId);
                                roomBox.style.zIndex = '20';

                                const move = function(moveEvent) {
                                    const deltaX = (moveEvent.clientX - startX) / zoomScale;
                                    const deltaY = (moveEvent.clientY - startY) / zoomScale;

                                    const maxWidth = parent.clientWidth - startLeft;
                                    const maxHeight = parent.clientHeight - startTop;

                                    const newWidth = Math.max(
                                        minRoomWidth,
                                        Math.min(maxWidth, snap(startWidth + deltaX))
                                    );

                                    const newHeight = Math.max(
                                        minRoomHeight,
                                        Math.min(maxHeight, snap(startHeight + deltaY))
                                    );

                                    roomBox.style.width = newWidth + 'px';
                                    roomBox.style.height = newHeight + 'px';
                                };

                                const up = function(upEvent) {
                                    resizeHandle.removeEventListener('pointermove', move);
                                    resizeHandle.removeEventListener('pointerup', up);
                                    resizeHandle.removeEventListener('pointercancel', up);

                                    roomBox.style.zIndex = '2';

                                    saveLayout();
                                };

                                resizeHandle.addEventListener('pointermove', move);
                                resizeHandle.addEventListener('pointerup', up);
                                resizeHandle.addEventListener('pointercancel', up);
                            });
                        }
                    }
                    """,
                    currentTool == EditorTool.SELECT,
                    currentTool == EditorTool.DESK,
                    MIN_ROOM_WIDTH,
                    MIN_ROOM_HEIGHT,
                    DEFAULT_DESK_WIDTH,
                    DEFAULT_DESK_HEIGHT,
                    snapToGridEnabled,
                    GRID_SIZE,
                    getZoomScale()
            );
        }

        @ClientCallable
        public void saveLayout(int posX, int posY, int width, int height) {
            boolean saved = roomService.updateRoomLayout(
                    room.getId(),
                    snapIfEnabled(posX),
                    snapIfEnabled(posY),
                    Math.max(snapIfEnabled(width), MIN_ROOM_WIDTH),
                    Math.max(snapIfEnabled(height), MIN_ROOM_HEIGHT)
            ).isPresent();

            if (saved) {
                Notification.show("Raum gespeichert.");
                renderSelectedOffice();
            } else {
                Notification.show("Raum konnte nicht gespeichert werden.");
            }
        }

        @ClientCallable
        public void createDeskAt(int posX, int posY) {
            openCreateDeskDialogAt(room, snapIfEnabled(posX), snapIfEnabled(posY));
        }
    }

    private class DraggableResizableDeskBox extends Div {

        private final Resource desk;

        private DraggableResizableDeskBox(Resource desk) {
            this.desk = desk;
        }

        private void enableInteractions() {
            getElement().executeJs("""
                    const deskBox = this;
                    const selectionMode = $0;
                    const minDeskWidth = $1;
                    const minDeskHeight = $2;
                    const snapToGrid = $3;
                    const gridSize = $4;
                    const zoomScale = $5;

                    const snap = (value) => {
                        if (!snapToGrid) {
                            return Math.round(value);
                        }

                        return Math.round(value / gridSize) * gridSize;
                    };

                    if (deskBox.__deskEditorInitialized) {
                        return;
                    }

                    deskBox.__deskEditorInitialized = true;
                    deskBox.style.touchAction = 'none';

                    const getParentBounds = () => {
                        const parent = deskBox.parentElement;
                        return {
                            parent,
                            rect: parent.getBoundingClientRect()
                        };
                    };

                    const saveLayout = () => {
                        const finalX = parseInt(deskBox.style.left, 10) || 0;
                        const finalY = parseInt(deskBox.style.top, 10) || 0;
                        const finalWidth = Math.round(deskBox.offsetWidth);
                        const finalHeight = Math.round(deskBox.offsetHeight);

                        deskBox.$server.saveLayout(finalX, finalY, finalWidth, finalHeight);
                    };

                    deskBox.addEventListener('click', function(event) {
                        event.stopPropagation();
                    });

                    if (selectionMode) {
                        deskBox.addEventListener('pointerdown', function(event) {
                            if (event.button !== 0) {
                                return;
                            }

                            if (
                                event.target.closest('vaadin-button') ||
                                event.target.closest('.desk-resize-handle')
                            ) {
                                return;
                            }

                            event.preventDefault();
                            event.stopPropagation();

                            const { parent, rect } = getParentBounds();
                            const boxRect = deskBox.getBoundingClientRect();

                            const shiftX = (event.clientX - boxRect.left) / zoomScale;
                            const shiftY = (event.clientY - boxRect.top) / zoomScale;

                            deskBox.setPointerCapture(event.pointerId);
                            deskBox.style.cursor = 'grabbing';
                            deskBox.style.zIndex = '30';

                            const move = function(moveEvent) {
                                const rawX = ((moveEvent.clientX - rect.left) / zoomScale) - shiftX;
                                const rawY = ((moveEvent.clientY - rect.top) / zoomScale) - shiftY;

                                const maxX = parent.clientWidth - deskBox.offsetWidth;
                                const maxY = parent.clientHeight - deskBox.offsetHeight;

                                const newX = Math.max(0, Math.min(maxX, snap(rawX)));
                                const newY = Math.max(44, Math.min(maxY, snap(rawY)));

                                deskBox.style.left = newX + 'px';
                                deskBox.style.top = newY + 'px';
                            };

                            const up = function(upEvent) {
                                deskBox.removeEventListener('pointermove', move);
                                deskBox.removeEventListener('pointerup', up);
                                deskBox.removeEventListener('pointercancel', up);

                                deskBox.style.cursor = 'grab';
                                deskBox.style.zIndex = '6';

                                saveLayout();
                            };

                            deskBox.addEventListener('pointermove', move);
                            deskBox.addEventListener('pointerup', up);
                            deskBox.addEventListener('pointercancel', up);
                        });

                        const resizeHandle = deskBox.querySelector('.desk-resize-handle');

                        if (resizeHandle && !resizeHandle.__resizeInitialized) {
                            resizeHandle.__resizeInitialized = true;

                            resizeHandle.addEventListener('pointerdown', function(event) {
                                if (event.button !== 0) {
                                    return;
                                }

                                event.preventDefault();
                                event.stopPropagation();

                                const { parent } = getParentBounds();
                                const startX = event.clientX;
                                const startY = event.clientY;
                                const startWidth = deskBox.offsetWidth;
                                const startHeight = deskBox.offsetHeight;
                                const startLeft = parseInt(deskBox.style.left, 10) || 0;
                                const startTop = parseInt(deskBox.style.top, 10) || 0;

                                resizeHandle.setPointerCapture(event.pointerId);
                                deskBox.style.zIndex = '30';

                                const move = function(moveEvent) {
                                    const deltaX = (moveEvent.clientX - startX) / zoomScale;
                                    const deltaY = (moveEvent.clientY - startY) / zoomScale;

                                    const maxWidth = parent.clientWidth - startLeft;
                                    const maxHeight = parent.clientHeight - startTop;

                                    const newWidth = Math.max(
                                        minDeskWidth,
                                        Math.min(maxWidth, snap(startWidth + deltaX))
                                    );

                                    const newHeight = Math.max(
                                        minDeskHeight,
                                        Math.min(maxHeight, snap(startHeight + deltaY))
                                    );

                                    deskBox.style.width = newWidth + 'px';
                                    deskBox.style.height = newHeight + 'px';
                                };

                                const up = function(upEvent) {
                                    resizeHandle.removeEventListener('pointermove', move);
                                    resizeHandle.removeEventListener('pointerup', up);
                                    resizeHandle.removeEventListener('pointercancel', up);

                                    deskBox.style.zIndex = '6';

                                    saveLayout();
                                };

                                resizeHandle.addEventListener('pointermove', move);
                                resizeHandle.addEventListener('pointerup', up);
                                resizeHandle.addEventListener('pointercancel', up);
                            });
                        }
                    }
                    """,
                    currentTool == EditorTool.SELECT,
                    MIN_DESK_WIDTH,
                    MIN_DESK_HEIGHT,
                    snapToGridEnabled,
                    GRID_SIZE,
                    getZoomScale()
            );
        }

        @ClientCallable
        public void saveLayout(int posX, int posY, int width, int height) {
            boolean saved = resourceService.updateResourceLayout(
                    desk.getId(),
                    snapIfEnabled(posX),
                    snapIfEnabled(posY),
                    Math.max(snapIfEnabled(width), MIN_DESK_WIDTH),
                    Math.max(snapIfEnabled(height), MIN_DESK_HEIGHT)
            ).isPresent();

            if (saved) {
                Notification.show("Tisch gespeichert.");
                renderSelectedOffice();
            } else {
                Notification.show("Tisch konnte nicht gespeichert werden.");
            }
        }
    }

    private void stopPointerAndClickPropagation(Button button) {
        button.getElement().executeJs("""
                const button = this;

                button.addEventListener('pointerdown', function(event) {
                    event.stopPropagation();
                });

                button.addEventListener('click', function(event) {
                    event.stopPropagation();
                });
                """);
    }

    private Dialog createStyledDialog() {
        Dialog dialog = new Dialog();
        dialog.setDraggable(false);
        dialog.setResizable(false);
        return dialog;
    }

    private H3 createDialogTitle(String text) {
        H3 title = new H3(text);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.45rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.03em")
                .set("color", "#0f172a");
        return title;
    }

    private Paragraph createDialogText(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.getStyle()
                .set("margin", "0")
                .set("color", "#64748b")
                .set("font-size", "0.95rem")
                .set("line-height", "1.55");
        return paragraph;
    }

    private VerticalLayout createDialogContent(Component... components) {
        VerticalLayout content = new VerticalLayout(components);
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidth("680px");
        content.setMaxWidth("92vw");
        content.getStyle()
                .set("padding", "0.4rem")
                .set("gap", "1rem")
                .set("font-family", "Inter, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif");
        return content;
    }

    private void styleInputField(Component field) {
        field.getElement().getThemeList().add("small");

        field.getElement().getStyle()
                .set("font-family", "Inter, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif")
                .set("--vaadin-input-field-background", "#f8fafc")
                .set("--vaadin-input-field-border-width", "1px")
                .set("--vaadin-input-field-border-color", "#dbe2ea")
                .set("--vaadin-input-field-border-radius", "14px")
                .set("--vaadin-input-field-hover-border-color", "#bfdbfe")
                .set("--vaadin-input-field-focus-border-color", "#2563eb")
                .set("--vaadin-input-field-value-color", "#0f172a")
                .set("--vaadin-input-field-label-color", "#475569")
                .set("--vaadin-input-field-placeholder-color", "#94a3b8");
    }

    private void styleCheckbox(Checkbox checkbox) {
        checkbox.getStyle()
                .set("color", "#334155")
                .set("font-weight", "700");
    }

    private void styleToolButton(Button button, boolean active) {
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        if (active) {
            button.getStyle()
                    .set("background", "linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)")
                    .set("color", "#ffffff")
                    .set("border", "none")
                    .set("border-radius", "14px")
                    .set("font-weight", "850")
                    .set("padding", "0.62rem 1rem")
                    .set("box-shadow", "0 12px 24px rgba(37, 99, 235, 0.18)");
        } else {
            button.getStyle()
                    .set("background", "#ffffff")
                    .set("color", "#334155")
                    .set("border", "1px solid #dbe2ea")
                    .set("border-radius", "14px")
                    .set("font-weight", "800")
                    .set("padding", "0.62rem 1rem")
                    .set("box-shadow", "0 8px 18px rgba(15, 23, 42, 0.04)");
        }
    }

    private void styleZoomButton(Button button, boolean active) {
        if (active) {
            button.getStyle()
                    .set("background", "#0f172a")
                    .set("color", "#ffffff")
                    .set("border", "1px solid #0f172a")
                    .set("border-radius", "12px")
                    .set("font-weight", "850")
                    .set("padding", "0.5rem 0.75rem")
                    .set("box-shadow", "0 10px 20px rgba(15, 23, 42, 0.12)");
        } else {
            button.getStyle()
                    .set("background", "#ffffff")
                    .set("color", "#334155")
                    .set("border", "1px solid #dbe2ea")
                    .set("border-radius", "12px")
                    .set("font-weight", "800")
                    .set("padding", "0.5rem 0.75rem")
                    .set("box-shadow", "0 8px 18px rgba(15, 23, 42, 0.04)");
        }
    }

    private void stylePrimaryButton(Button button) {
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.getStyle()
                .set("background", "linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)")
                .set("border", "none")
                .set("border-radius", "14px")
                .set("font-weight", "850")
                .set("padding", "0.62rem 1rem")
                .set("box-shadow", "0 12px 24px rgba(37, 99, 235, 0.18)");
    }

    private void styleDangerButton(Button button) {
        button.addThemeVariants(ButtonVariant.LUMO_ERROR);
        button.getStyle()
                .set("border-radius", "14px")
                .set("font-weight", "850")
                .set("padding", "0.62rem 1rem");
    }

    private void styleSecondaryButton(Button button) {
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        button.getStyle()
                .set("background", "#ffffff")
                .set("color", "#334155")
                .set("border", "1px solid #dbe2ea")
                .set("border-radius", "14px")
                .set("font-weight", "750")
                .set("padding", "0.62rem 0.95rem")
                .set("box-shadow", "0 8px 18px rgba(15, 23, 42, 0.04)");
    }
}