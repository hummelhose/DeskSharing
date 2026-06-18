package io.github.hummelhose.desksharing.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.hummelhose.desksharing.application.service.OfficeService;
import io.github.hummelhose.desksharing.application.service.ReservationService;
import io.github.hummelhose.desksharing.application.service.ResourceService;
import io.github.hummelhose.desksharing.application.service.RoomService;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import io.github.hummelhose.desksharing.domain.model.Office;
import io.github.hummelhose.desksharing.domain.model.Reservation;
import io.github.hummelhose.desksharing.domain.model.ReservationStatus;
import io.github.hummelhose.desksharing.domain.model.Resource;
import io.github.hummelhose.desksharing.domain.model.ResourceType;
import io.github.hummelhose.desksharing.domain.model.Room;
import io.github.hummelhose.desksharing.infrastructure.security.CurrentUserService;
import io.github.hummelhose.desksharing.ui.layout.MainLayout;
import io.github.hummelhose.desksharing.ui.layout.ViewFrame;
import jakarta.annotation.security.PermitAll;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Route(value = "floorplan", layout = MainLayout.class)
@PageTitle("Sitzplatz buchen")
@PermitAll
public class FloorplanView extends VerticalLayout {

    private final OfficeService officeService;
    private final RoomService roomService;
    private final ResourceService resourceService;
    private final ReservationService reservationService;
    private final CurrentUserService currentUserService;

    private final ComboBox<Office> officeField = new ComboBox<>("Büro / Standort");
    private final Div floorplanCanvas = new Div();
    private final Div myReservationsContainer = new Div();

    private final Button zoom75Button = new Button("75%");
    private final Button zoom100Button = new Button("100%");
    private final Button zoom125Button = new Button("125%");
    private final Button zoom150Button = new Button("150%");

    private int currentZoomPercent = 100;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private enum DeskDisplayState {
        FREE,
        RESERVED_LATER_TODAY,
        OCCUPIED_NOW,
        NOT_BOOKABLE
    }

    public FloorplanView(OfficeService officeService,
                         RoomService roomService,
                         ResourceService resourceService,
                         ReservationService reservationService,
                         CurrentUserService currentUserService) {
        this.officeService = officeService;
        this.roomService = roomService;
        this.resourceService = resourceService;
        this.reservationService = reservationService;
        this.currentUserService = currentUserService;

        configureOfficeField();
        configureZoomButtons();
        configureCanvas();
        configureMyReservationsContainer();

        ViewFrame headerFrame = createHeaderFrame();
        ViewFrame myReservationsFrame = createMyReservationsFrame();
        ViewFrame floorplanFrame = createFloorplanFrame();

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle()
                .set("background", "linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%)")
                .set("gap", "1rem");

        add(headerFrame, myReservationsFrame, floorplanFrame);

        renderOfficeFloorplan(null);
        refreshMyReservations();
        configureAutoRefresh();
    }

    private ViewFrame createHeaderFrame() {
        Span eyebrow = new Span("BUCHUNG");
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

        H1 title = new H1("Sitzplatz buchen");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.65rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.045em")
                .set("line-height", "1.05")
                .set("color", "#0f172a");

        Paragraph subtitle = new Paragraph("Wähle ein Büro aus, klicke auf einen freien Tisch und buche deinen Arbeitsplatz.");
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

    private ViewFrame createMyReservationsFrame() {
        H3 sectionTitle = createSectionTitle("Meine Reservierungen");
        Paragraph sectionText = createSectionText("Hier siehst du deine aktuellen und kommenden Sitzplatzbuchungen.");

        ViewFrame frame = new ViewFrame(sectionTitle, sectionText, myReservationsContainer);
        frame.getStyle()
                .set("padding", "1.2rem 1.4rem")
                .set("gap", "0.85rem");

        return frame;
    }

    private ViewFrame createFloorplanFrame() {
        H3 sectionTitle = createSectionTitle("Sitzplan");
        Paragraph sectionText = createSectionText("Wähle ein Büro aus. Grün ist frei, Rot ist aktuell belegt, Orange ist später reserviert.");

        HorizontalLayout zoomTools = createZoomTools();

        HorizontalLayout topRow = new HorizontalLayout(officeField, createLegend(), zoomTools);
        topRow.setWidthFull();
        topRow.setAlignItems(Alignment.END);
        topRow.getStyle()
                .set("gap", "1rem")
                .set("flex-wrap", "wrap")
                .set("justify-content", "space-between");

        ViewFrame frame = new ViewFrame(sectionTitle, sectionText, topRow, floorplanCanvas);
        frame.getStyle()
                .set("padding", "1.35rem 1.5rem")
                .set("gap", "1rem");

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

    private Div createLegend() {
        Div legend = new Div(
                createLegendItem("#16a34a", "frei"),
                createLegendItem("#f59e0b", "später"),
                createLegendItem("#dc2626", "belegt"),
                createLegendItem("#64748b", "gesperrt")
        );

        legend.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "0.6rem")
                .set("flex-wrap", "wrap")
                .set("padding", "0.75rem 0.95rem")
                .set("border-radius", "18px")
                .set("background", "#f8fafc")
                .set("border", "1px solid #e2e8f0")
                .set("width", "fit-content");

        return legend;
    }

    private HorizontalLayout createLegendItem(String color, String text) {
        Div dot = new Div();
        dot.getStyle()
                .set("width", "12px")
                .set("height", "12px")
                .set("border-radius", "999px")
                .set("background", color)
                .set("flex-shrink", "0");

        Span label = new Span(text);
        label.getStyle()
                .set("color", "#475569")
                .set("font-size", "0.86rem")
                .set("font-weight", "700");

        HorizontalLayout item = new HorizontalLayout(dot, label);
        item.setPadding(false);
        item.setSpacing(true);
        item.setAlignItems(Alignment.CENTER);
        item.getStyle().set("gap", "0.42rem");
        return item;
    }

    private HorizontalLayout createZoomTools() {
        Span zoomLabel = new Span("Zoom");
        zoomLabel.getStyle()
                .set("font-size", "0.82rem")
                .set("font-weight", "850")
                .set("color", "#475569")
                .set("align-self", "center");

        HorizontalLayout layout = new HorizontalLayout(
                zoomLabel,
                zoom75Button,
                zoom100Button,
                zoom125Button,
                zoom150Button
        );
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.getStyle()
                .set("gap", "0.45rem")
                .set("flex-wrap", "wrap")
                .set("align-items", "center");

        return layout;
    }

    private void configureOfficeField() {
        officeField.setWidth("360px");
        officeField.setItems(officeService.getAllActiveOffices());
        officeField.setPlaceholder("Büro / Standort auswählen");
        officeField.setItemLabelGenerator(Office::getName);
        styleInputField(officeField);

        officeField.addValueChangeListener(event -> renderOfficeFloorplan(event.getValue()));
    }

    private void configureZoomButtons() {
        zoom75Button.addClickListener(event -> changeZoom(75));
        zoom100Button.addClickListener(event -> changeZoom(100));
        zoom125Button.addClickListener(event -> changeZoom(125));
        zoom150Button.addClickListener(event -> changeZoom(150));

        updateZoomButtonStyles();
    }

    private void changeZoom(int zoomPercent) {
        currentZoomPercent = zoomPercent;
        updateZoomButtonStyles();
        renderSelectedOffice();
    }

    private void updateZoomButtonStyles() {
        styleZoomButton(zoom75Button, currentZoomPercent == 75);
        styleZoomButton(zoom100Button, currentZoomPercent == 100);
        styleZoomButton(zoom125Button, currentZoomPercent == 125);
        styleZoomButton(zoom150Button, currentZoomPercent == 150);
    }

    private void configureCanvas() {
        floorplanCanvas.setWidthFull();
        floorplanCanvas.getStyle()
                .set("min-height", "620px")
                .set("overflow", "auto")
                .set("padding-bottom", "0.25rem");
    }

    private void configureMyReservationsContainer() {
        myReservationsContainer.setWidthFull();
        myReservationsContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(280px, 1fr))")
                .set("gap", "0.75rem");
    }

    private void renderOfficeFloorplan(Office office) {
        floorplanCanvas.removeAll();

        if (office == null) {
            Div placeholder = new Div();
            placeholder.setText("Bitte wähle zuerst ein Büro aus.");
            placeholder.getStyle()
                    .set("padding", "1.15rem")
                    .set("border-radius", "18px")
                    .set("background", "#f8fafc")
                    .set("border", "1px dashed #cbd5e1")
                    .set("color", "#64748b")
                    .set("font-weight", "700");

            floorplanCanvas.add(placeholder);
            return;
        }

        int width = office.getLayoutWidth() != null ? office.getLayoutWidth() : 1800;
        int height = office.getLayoutHeight() != null ? office.getLayoutHeight() : 1100;

        Div canvasWrapper = new Div();
        canvasWrapper.getStyle()
                .set("width", "fit-content")
                .set("min-width", "100%")
                .set("padding", "0.25rem")
                .set("box-sizing", "border-box");

        Div officeArea = new Div();
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
                .set("overflow", "hidden");

        officeArea.add(createOfficeTitleBadge(office));

        List<Room> rooms = roomService.getActiveRoomsByOfficeId(office.getId());

        if (rooms.isEmpty()) {
            officeArea.add(createEmptyOfficeHint());
        }

        for (Room room : rooms) {
            officeArea.add(createRoomComponent(room));
        }

        canvasWrapper.add(officeArea);
        floorplanCanvas.add(canvasWrapper);
    }

    private String getOfficeCanvasBackground() {
        return "repeating-linear-gradient(to right, rgba(37, 99, 235, 0.07) 0 1px, transparent 1px 20px), " +
                "repeating-linear-gradient(to bottom, rgba(37, 99, 235, 0.07) 0 1px, transparent 1px 20px), " +
                "linear-gradient(180deg, #fbfdff 0%, #f8fafc 100%)";
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

    private Div createEmptyOfficeHint() {
        Div hint = new Div();
        hint.setText("In diesem Büro sind noch keine aktiven Räume angelegt.");
        hint.getStyle()
                .set("position", "absolute")
                .set("top", "68px")
                .set("left", "16px")
                .set("padding", "0.8rem 1rem")
                .set("border-radius", "16px")
                .set("background", "#f8fafc")
                .set("border", "1px dashed #cbd5e1")
                .set("color", "#64748b")
                .set("font-weight", "700")
                .set("z-index", "5");
        return hint;
    }

    private Div createRoomComponent(Room room) {
        Div roomBox = new Div();

        int x = room.getPosX() != null ? room.getPosX() : 20;
        int y = room.getPosY() != null ? room.getPosY() : 20;
        int width = room.getLayoutWidth() != null ? room.getLayoutWidth() : 280;
        int height = room.getLayoutHeight() != null ? room.getLayoutHeight() : 180;

        Div roomHeader = new Div();
        roomHeader.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("right", "0")
                .set("height", "42px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("padding", "0.45rem 0.65rem")
                .set("box-sizing", "border-box")
                .set("background", "rgba(15, 23, 42, 0.92)")
                .set("color", "#ffffff")
                .set("border-radius", "18px 18px 0 0")
                .set("z-index", "8");

        Span roomTitle = new Span(room.getName() != null && !room.getName().isBlank()
                ? room.getName()
                : "Unbenannter Raum");
        roomTitle.getStyle()
                .set("font-size", "0.86rem")
                .set("font-weight", "850")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        roomHeader.add(roomTitle);

        roomBox.getStyle()
                .set("position", "absolute")
                .set("box-sizing", "border-box")
                .set("left", x + "px")
                .set("top", y + "px")
                .set("width", width + "px")
                .set("height", height + "px")
                .set("background", "rgba(255, 255, 255, 0.96)")
                .set("border", "2px solid #2563eb")
                .set("border-radius", "20px")
                .set("box-shadow", "0 18px 38px rgba(15, 23, 42, 0.12)")
                .set("overflow", "hidden")
                .set("user-select", "none")
                .set("z-index", "2");

        roomBox.add(roomHeader);

        List<Resource> desks = resourceService.getAllResources().stream()
                .filter(resource -> resource.getResourceType() == ResourceType.DESK)
                .filter(Resource::isActive)
                .filter(resource -> resource.getRoom() != null)
                .filter(resource -> resource.getRoom().getId().equals(room.getId()))
                .toList();

        for (Resource desk : desks) {
            roomBox.add(createDeskComponent(desk));
        }

        return roomBox;
    }

    private Div createDeskComponent(Resource desk) {
        Optional<Reservation> currentReservation = getCurrentReservationForDesk(desk);
        Optional<Reservation> nextReservationToday = currentReservation.isEmpty() && desk.isBookable()
                ? getNextReservationTodayForDesk(desk)
                : Optional.empty();

        DeskDisplayState state = getDeskDisplayState(desk, currentReservation, nextReservationToday);

        Div deskBox = new Div();

        Span name = new Span(desk.getName());
        name.getStyle()
                .set("font-weight", "850")
                .set("line-height", "1.1")
                .set("max-width", "100%")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(Alignment.CENTER);
        content.getStyle()
                .set("gap", "0.15rem")
                .set("width", "100%");

        content.add(name);

        if (state == DeskDisplayState.OCCUPIED_NOW) {
            Reservation reservation = currentReservation.get();

            Span bookedBy = new Span(reservation.getAppUser().getDisplayName());
            bookedBy.getStyle()
                    .set("font-size", "0.72rem")
                    .set("font-weight", "800")
                    .set("line-height", "1.05")
                    .set("max-width", "100%")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis")
                    .set("white-space", "nowrap");

            Span until = new Span("bis " + reservation.getEndDateTime().format(TIME_FORMATTER));
            until.getStyle()
                    .set("font-size", "0.68rem")
                    .set("font-weight", "700")
                    .set("opacity", "0.92");

            content.add(bookedBy, until);
        } else if (state == DeskDisplayState.RESERVED_LATER_TODAY) {
            Reservation reservation = nextReservationToday.get();

            Span from = new Span("ab " + reservation.getStartDateTime().format(TIME_FORMATTER));
            from.getStyle()
                    .set("font-size", "0.72rem")
                    .set("font-weight", "850")
                    .set("line-height", "1.05");

            Span bookedBy = new Span(reservation.getAppUser().getDisplayName());
            bookedBy.getStyle()
                    .set("font-size", "0.68rem")
                    .set("font-weight", "750")
                    .set("max-width", "100%")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis")
                    .set("white-space", "nowrap");

            content.add(from, bookedBy);
        } else if (state == DeskDisplayState.NOT_BOOKABLE) {
            Span notBookableLabel = new Span("gesperrt");
            notBookableLabel.getStyle()
                    .set("font-size", "0.7rem")
                    .set("font-weight", "850")
                    .set("line-height", "1.05");

            content.add(notBookableLabel);
        } else {
            Span freeLabel = new Span("frei");
            freeLabel.getStyle()
                    .set("font-size", "0.72rem")
                    .set("font-weight", "800")
                    .set("opacity", "0.92");

            content.add(freeLabel);
        }

        int x = desk.getPosX() != null ? desk.getPosX() : 0;
        int y = desk.getPosY() != null ? desk.getPosY() : 48;
        int width = desk.getWidth() != null ? desk.getWidth() : 108;
        int height = desk.getHeight() != null ? desk.getHeight() : 74;

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
                .set("cursor", state == DeskDisplayState.NOT_BOOKABLE ? "not-allowed" : "pointer")
                .set("user-select", "none")
                .set("z-index", "10")
                .set("transition", "transform 140ms ease, box-shadow 140ms ease");

        deskBox.getElement().executeJs("""
                const el = this;
                el.addEventListener('mouseenter', () => {
                    el.style.transform = 'translateY(-2px)';
                    el.style.boxShadow = '0 18px 36px rgba(15, 23, 42, 0.14)';
                });
                el.addEventListener('mouseleave', () => {
                    el.style.transform = 'translateY(0)';
                    el.style.boxShadow = '0 12px 28px rgba(15, 23, 42, 0.10)';
                });
                """);

        applyDeskStateStyle(deskBox, state);

        deskBox.getElement().setProperty(
                "title",
                buildDeskTooltip(desk, state, currentReservation, nextReservationToday)
        );

        deskBox.addClickListener(event -> handleDeskClick(desk, currentReservation, nextReservationToday));

        deskBox.add(content);
        return deskBox;
    }

    private DeskDisplayState getDeskDisplayState(Resource desk,
                                                 Optional<Reservation> currentReservation,
                                                 Optional<Reservation> nextReservationToday) {
        if (currentReservation.isPresent()) {
            return DeskDisplayState.OCCUPIED_NOW;
        }

        if (!desk.isBookable()) {
            return DeskDisplayState.NOT_BOOKABLE;
        }

        if (nextReservationToday.isPresent()) {
            return DeskDisplayState.RESERVED_LATER_TODAY;
        }

        return DeskDisplayState.FREE;
    }

    private void applyDeskStateStyle(Div deskBox, DeskDisplayState state) {
        if (state == DeskDisplayState.OCCUPIED_NOW) {
            deskBox.getStyle()
                    .set("color", "#ffffff")
                    .set("background", "linear-gradient(135deg, #ef4444 0%, #dc2626 100%)")
                    .set("border", "2px solid #b91c1c");
            return;
        }

        if (state == DeskDisplayState.RESERVED_LATER_TODAY) {
            deskBox.getStyle()
                    .set("color", "#78350f")
                    .set("background", "linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)")
                    .set("border", "2px solid #f59e0b");
            return;
        }

        if (state == DeskDisplayState.NOT_BOOKABLE) {
            deskBox.getStyle()
                    .set("color", "#334155")
                    .set("background", "linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%)")
                    .set("border", "2px solid #64748b")
                    .set("opacity", "0.95");
            return;
        }

        deskBox.getStyle()
                .set("color", "#065f46")
                .set("background", "linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%)")
                .set("border", "2px solid #16a34a");
    }

    private void handleDeskClick(Resource desk,
                                 Optional<Reservation> currentReservation,
                                 Optional<Reservation> nextReservationToday) {
        if (currentReservation.isPresent()) {
            Reservation reservation = currentReservation.get();
            AppUser currentUser = currentUserService.getOrCreateCurrentUser();

            boolean ownReservation = reservation.getAppUser().getId().equals(currentUser.getId());

            if (ownReservation) {
                openOwnReservationDialog(reservation);
            } else {
                openOccupiedInfoDialog(reservation);
            }

            return;
        }

        if (!desk.isBookable()) {
            openNotBookableDialog(desk);
            return;
        }

        if (nextReservationToday.isPresent()) {
            Reservation reservation = nextReservationToday.get();
            AppUser currentUser = currentUserService.getOrCreateCurrentUser();

            boolean ownReservation = reservation.getAppUser().getId().equals(currentUser.getId());

            if (ownReservation) {
                openOwnFutureReservationDialog(reservation, desk);
            } else {
                openBookingDialog(desk, nextReservationToday);
            }

            return;
        }

        openBookingDialog(desk, Optional.empty());
    }

    private void openNotBookableDialog(Resource desk) {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Sitzplatz nicht buchbar");
        Paragraph text = createDialogText(
                desk.getName() + " ist aktuell nicht buchbar. Bitte wähle einen anderen Sitzplatz aus."
        );

        Button closeButton = new Button("Schließen", event -> dialog.close());
        stylePrimaryButton(closeButton);

        VerticalLayout content = createDialogContent(title, text, closeButton);
        dialog.add(content);
        dialog.open();
    }

    private void openBookingDialog(Resource desk, Optional<Reservation> nextReservationToday) {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Sitzplatz buchen");
        Paragraph text = createDialogText(
                desk.getName()
                        + " · "
                        + desk.getRoom().getName()
                        + " · "
                        + desk.getRoom().getOffice().getName()
        );

        Div quickBox = new Div();
        quickBox.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(150px, 1fr))")
                .set("gap", "0.65rem")
                .set("width", "100%");

        LocalDate today = LocalDate.now();
        LocalTime nextStartTime = getNextRoundedTime();
        LocalTime defaultEndTime = nextStartTime.plusHours(1);
        LocalDate defaultEndDate = defaultEndTime.isAfter(nextStartTime) ? today : today.plusDays(1);

        DatePicker startDateField = new DatePicker("Startdatum");
        startDateField.setWidthFull();
        startDateField.setValue(today);

        TimePicker startTimeField = new TimePicker("Startzeit");
        startTimeField.setWidthFull();
        startTimeField.setStep(Duration.ofMinutes(15));
        startTimeField.setValue(nextStartTime);

        DatePicker endDateField = new DatePicker("Enddatum");
        endDateField.setWidthFull();
        endDateField.setValue(defaultEndDate);

        TimePicker endTimeField = new TimePicker("Endzeit");
        endTimeField.setWidthFull();
        endTimeField.setStep(Duration.ofMinutes(15));
        endTimeField.setValue(defaultEndTime);

        quickBox.add(
                createQuickBookingButton("1 Stunde", "ab nächster Viertelstunde", startDateField, startTimeField, endDateField, endTimeField, "ONE_HOUR"),
                createQuickBookingButton("2 Stunden", "ab nächster Viertelstunde", startDateField, startTimeField, endDateField, endTimeField, "TWO_HOURS"),
                createQuickBookingButton("Bis Feierabend", "bis 17:00 Uhr", startDateField, startTimeField, endDateField, endTimeField, "UNTIL_END_OF_DAY"),
                createQuickBookingButton("Morgen Vormittag", "08:00 - 12:00 Uhr", startDateField, startTimeField, endDateField, endTimeField, "TOMORROW_MORNING")
        );

        VerticalLayout infoBoxWrapper = new VerticalLayout();
        infoBoxWrapper.setPadding(false);
        infoBoxWrapper.setSpacing(false);

        if (nextReservationToday.isPresent()) {
            Reservation nextReservation = nextReservationToday.get();

            Div infoBox = new Div();
            infoBox.setText(
                    "Hinweis: Dieser Sitzplatz ist heute ab "
                            + nextReservation.getStartDateTime().format(TIME_FORMATTER)
                            + " von "
                            + nextReservation.getAppUser().getDisplayName()
                            + " reserviert."
            );
            infoBox.getStyle()
                    .set("padding", "0.9rem 1rem")
                    .set("border-radius", "16px")
                    .set("background", "#fffbeb")
                    .set("border", "1px solid #fde68a")
                    .set("color", "#92400e")
                    .set("font-size", "0.9rem")
                    .set("font-weight", "650");

            infoBoxWrapper.add(infoBox);
        }

        TextField titleField = new TextField("Titel");
        titleField.setWidthFull();
        titleField.setValue("Sitzplatzbuchung");

        TextArea notesField = new TextArea("Notiz");
        notesField.setWidthFull();
        notesField.setMinHeight("90px");
        notesField.setPlaceholder("Optional");

        styleInputField(startDateField);
        styleInputField(startTimeField);
        styleInputField(endDateField);
        styleInputField(endTimeField);
        styleInputField(titleField);
        styleInputField(notesField);

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.add(startDateField, startTimeField, endDateField, endTimeField, titleField, notesField);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        formLayout.setColspan(titleField, 2);
        formLayout.setColspan(notesField, 2);

        Button bookButton = new Button("Sitzplatz buchen");
        stylePrimaryButton(bookButton);

        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        styleSecondaryButton(cancelButton);

        HorizontalLayout actions = new HorizontalLayout(bookButton, cancelButton);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.getStyle()
                .set("gap", "0.75rem")
                .set("flex-wrap", "wrap");

        bookButton.addClickListener(event -> {
            if (startDateField.getValue() == null || startTimeField.getValue() == null ||
                    endDateField.getValue() == null || endTimeField.getValue() == null) {
                showBookingErrorNotification("Bitte Start und Ende vollständig ausfüllen.");
                return;
            }

            LocalDateTime startDateTime = LocalDateTime.of(startDateField.getValue(), startTimeField.getValue());
            LocalDateTime endDateTime = LocalDateTime.of(endDateField.getValue(), endTimeField.getValue());

            AppUser currentUser = currentUserService.getOrCreateCurrentUser();

            ReservationService.ReservationCreationResult result = reservationService.createReservationWithResult(
                    currentUser.getId(),
                    desk.getId(),
                    startDateTime,
                    endDateTime,
                    titleField.getValue(),
                    notesField.getValue()
            );

            if (result.success()) {
                dialog.close();
                Notification.show(result.message());
                refreshAfterReservationChange();
            } else {
                showBookingErrorNotification(result.message());
            }
        });

        Span quickTitle = new Span("Schnellwahl");
        quickTitle.getStyle()
                .set("font-size", "0.9rem")
                .set("font-weight", "850")
                .set("color", "#0f172a");

        VerticalLayout content = createDialogContent(title, text, infoBoxWrapper, quickTitle, quickBox, formLayout, actions);
        dialog.add(content);
        dialog.open();
    }

    private Div createQuickBookingButton(String title,
                                         String subtitle,
                                         DatePicker startDateField,
                                         TimePicker startTimeField,
                                         DatePicker endDateField,
                                         TimePicker endTimeField,
                                         String type) {
        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "0.9rem")
                .set("font-weight", "850")
                .set("color", "#0f172a");

        Span subtitleSpan = new Span(subtitle);
        subtitleSpan.getStyle()
                .set("font-size", "0.76rem")
                .set("font-weight", "650")
                .set("color", "#64748b");

        VerticalLayout content = new VerticalLayout(titleSpan, subtitleSpan);
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle()
                .set("gap", "0.15rem")
                .set("align-items", "flex-start")
                .set("pointer-events", "none");

        Div quickButton = new Div(content);
        quickButton.getStyle()
                .set("height", "auto")
                .set("padding", "0.8rem 0.9rem")
                .set("background", "#ffffff")
                .set("border", "1px solid #dbe2ea")
                .set("border-radius", "16px")
                .set("box-shadow", "0 8px 18px rgba(15, 23, 42, 0.04)")
                .set("cursor", "pointer")
                .set("user-select", "none")
                .set("transition", "transform 140ms ease, box-shadow 140ms ease");

        quickButton.getElement().executeJs("""
            const el = this;

            el.addEventListener('mouseenter', () => {
                el.style.transform = 'translateY(-2px)';
                el.style.boxShadow = '0 14px 28px rgba(15, 23, 42, 0.10)';
            });

            el.addEventListener('mouseleave', () => {
                el.style.transform = 'translateY(0)';
                el.style.boxShadow = '0 8px 18px rgba(15, 23, 42, 0.04)';
            });
            """);

        quickButton.addClickListener(event ->
                applyQuickBookingRange(type, startDateField, startTimeField, endDateField, endTimeField)
        );

        return quickButton;
    }

    private void applyQuickBookingRange(String type,
                                        DatePicker startDateField,
                                        TimePicker startTimeField,
                                        DatePicker endDateField,
                                        TimePicker endTimeField) {
        LocalDateTime nowRounded = LocalDateTime.of(LocalDate.now(), getNextRoundedTime());
        LocalDateTime start;
        LocalDateTime end;

        switch (type) {
            case "ONE_HOUR" -> {
                start = nowRounded;
                end = start.plusHours(1);
            }
            case "TWO_HOURS" -> {
                start = nowRounded;
                end = start.plusHours(2);
            }
            case "UNTIL_END_OF_DAY" -> {
                start = nowRounded;
                end = LocalDateTime.of(LocalDate.now(), LocalTime.of(17, 0));

                if (!end.isAfter(start)) {
                    end = start.plusHours(1);
                }
            }
            case "TOMORROW_MORNING" -> {
                start = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(8, 0));
                end = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(12, 0));
            }
            default -> {
                start = nowRounded;
                end = start.plusHours(1);
            }
        }

        startDateField.setValue(start.toLocalDate());
        startTimeField.setValue(start.toLocalTime());
        endDateField.setValue(end.toLocalDate());
        endTimeField.setValue(end.toLocalTime());
    }

    private void openOwnFutureReservationDialog(Reservation reservation, Resource desk) {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Deine spätere Buchung");
        Paragraph text = createDialogText(
                reservation.getResource().getName()
                        + " ist heute ab "
                        + reservation.getStartDateTime().format(TIME_FORMATTER)
                        + " von dir reserviert."
        );

        Button cancelReservationButton = new Button("Reservierung stornieren");
        cancelReservationButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        cancelReservationButton.getStyle()
                .set("border-radius", "14px")
                .set("font-weight", "850")
                .set("padding", "0.62rem 1rem");

        Button bookAnotherButton = new Button("Weitere Buchung");
        stylePrimaryButton(bookAnotherButton);

        Button closeButton = new Button("Schließen", event -> dialog.close());
        styleSecondaryButton(closeButton);

        cancelReservationButton.addClickListener(event -> {
            boolean cancelled = reservationService.cancelReservation(reservation.getId()).isPresent();

            if (cancelled) {
                dialog.close();
                Notification.show("Reservierung wurde storniert.");
                refreshAfterReservationChange();
            } else {
                showBookingErrorNotification("Reservierung konnte nicht storniert werden.");
            }
        });

        bookAnotherButton.addClickListener(event -> {
            dialog.close();
            openBookingDialog(desk, Optional.of(reservation));
        });

        HorizontalLayout actions = new HorizontalLayout(cancelReservationButton, bookAnotherButton, closeButton);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.getStyle().set("flex-wrap", "wrap");

        VerticalLayout content = createDialogContent(title, text, actions);
        dialog.add(content);
        dialog.open();
    }

    private void openOwnReservationDialog(Reservation reservation) {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Deine Buchung");
        Paragraph text = createDialogText(
                reservation.getResource().getName()
                        + " ist aktuell von dir gebucht bis "
                        + reservation.getEndDateTime().format(DATE_TIME_FORMATTER)
                        + "."
        );

        Button cancelReservationButton = new Button("Reservierung stornieren");
        cancelReservationButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        cancelReservationButton.getStyle()
                .set("border-radius", "14px")
                .set("font-weight", "850")
                .set("padding", "0.62rem 1rem");

        Button closeButton = new Button("Schließen", event -> dialog.close());
        styleSecondaryButton(closeButton);

        cancelReservationButton.addClickListener(event -> {
            boolean cancelled = reservationService.cancelReservation(reservation.getId()).isPresent();

            if (cancelled) {
                dialog.close();
                Notification.show("Reservierung wurde storniert.");
                refreshAfterReservationChange();
            } else {
                showBookingErrorNotification("Reservierung konnte nicht storniert werden.");
            }
        });

        HorizontalLayout actions = new HorizontalLayout(cancelReservationButton, closeButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        VerticalLayout content = createDialogContent(title, text, actions);
        dialog.add(content);
        dialog.open();
    }

    private void openOccupiedInfoDialog(Reservation reservation) {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Sitzplatz ist belegt");
        Paragraph text = createDialogText(
                reservation.getResource().getName()
                        + " ist aktuell gebucht von "
                        + reservation.getAppUser().getDisplayName()
                        + " bis "
                        + reservation.getEndDateTime().format(DATE_TIME_FORMATTER)
                        + "."
        );

        Button closeButton = new Button("Schließen", event -> dialog.close());
        stylePrimaryButton(closeButton);

        VerticalLayout content = createDialogContent(title, text, closeButton);
        dialog.add(content);
        dialog.open();
    }

    private void refreshMyReservations() {
        myReservationsContainer.removeAll();

        AppUser currentUser = currentUserService.getOrCreateCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> reservations = reservationService.getReservationsByAppUserId(currentUser.getId()).stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE)
                .filter(reservation -> reservation.getEndDateTime().isAfter(now))
                .sorted(Comparator.comparing(Reservation::getStartDateTime))
                .toList();

        if (reservations.isEmpty()) {
            Div emptyState = new Div();
            emptyState.setText("Du hast aktuell keine aktiven oder zukünftigen Sitzplatzbuchungen.");
            emptyState.getStyle()
                    .set("padding", "1rem")
                    .set("border-radius", "16px")
                    .set("background", "#f8fafc")
                    .set("border", "1px solid #e2e8f0")
                    .set("color", "#64748b")
                    .set("font-weight", "650");

            myReservationsContainer.add(emptyState);
            return;
        }

        for (Reservation reservation : reservations) {
            myReservationsContainer.add(createMyReservationCard(reservation));
        }
    }

    private Div createMyReservationCard(Reservation reservation) {
        Div card = new Div();
        card.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("gap", "1rem")
                .set("padding", "1rem 1.1rem")
                .set("border-radius", "18px")
                .set("background", "#ffffff")
                .set("border", "1px solid #e2e8f0")
                .set("box-shadow", "0 10px 24px rgba(15, 23, 42, 0.04)");

        Div info = new Div();
        info.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.25rem");

        String officeName = reservation.getResource().getRoom().getOffice() != null
                ? reservation.getResource().getRoom().getOffice().getName()
                : "Ohne Büro";

        Span title = new Span(
                reservation.getResource().getName()
                        + " · "
                        + reservation.getResource().getRoom().getName()
                        + " · "
                        + officeName
        );
        title.getStyle()
                .set("font-weight", "850")
                .set("color", "#0f172a");

        Span time = new Span(
                reservation.getStartDateTime().format(DATE_TIME_FORMATTER)
                        + " - "
                        + reservation.getEndDateTime().format(DATE_TIME_FORMATTER)
        );
        time.getStyle()
                .set("font-size", "0.9rem")
                .set("color", "#64748b");

        Span reservationTitle = new Span(reservation.getTitle() != null && !reservation.getTitle().isBlank()
                ? reservation.getTitle()
                : "Sitzplatzbuchung");
        reservationTitle.getStyle()
                .set("font-size", "0.86rem")
                .set("color", "#475569");

        info.add(title, time, reservationTitle);

        Button cancelButton = new Button("Stornieren");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        cancelButton.getStyle()
                .set("border-radius", "12px")
                .set("font-weight", "750");

        cancelButton.addClickListener(event -> cancelReservationFromList(reservation));

        card.add(info, cancelButton);
        return card;
    }

    private void cancelReservationFromList(Reservation reservation) {
        boolean cancelled = reservationService.cancelReservation(reservation.getId()).isPresent();

        if (cancelled) {
            Notification.show("Reservierung wurde storniert.");
            refreshAfterReservationChange();
        } else {
            showBookingErrorNotification("Reservierung konnte nicht storniert werden.");
        }
    }

    private Optional<Reservation> getCurrentReservationForDesk(Resource desk) {
        return reservationService.getCurrentReservationForResource(
                desk.getId(),
                LocalDateTime.now()
        );
    }

    private Optional<Reservation> getNextReservationTodayForDesk(Resource desk) {
        return reservationService.getNextReservationTodayForResource(
                desk.getId(),
                LocalDateTime.now()
        );
    }

    private String buildDeskTooltip(Resource desk,
                                    DeskDisplayState state,
                                    Optional<Reservation> currentReservation,
                                    Optional<Reservation> nextReservationToday) {
        String officeName = desk.getRoom().getOffice() != null
                ? desk.getRoom().getOffice().getName()
                : "Ohne Büro";

        if (state == DeskDisplayState.NOT_BOOKABLE) {
            return "Tisch: " + desk.getName()
                    + "\nBüro: " + officeName
                    + "\nRaum: " + desk.getRoom().getName()
                    + "\nStatus: Nicht buchbar";
        }

        if (currentReservation.isPresent()) {
            Reservation reservation = currentReservation.get();

            return "Tisch: " + desk.getName()
                    + "\nBüro: " + officeName
                    + "\nRaum: " + desk.getRoom().getName()
                    + "\nStatus: Jetzt belegt"
                    + "\nBenutzer: " + reservation.getAppUser().getDisplayName()
                    + "\nBis: " + reservation.getEndDateTime().format(DATE_TIME_FORMATTER);
        }

        if (nextReservationToday.isPresent()) {
            Reservation reservation = nextReservationToday.get();

            return "Tisch: " + desk.getName()
                    + "\nBüro: " + officeName
                    + "\nRaum: " + desk.getRoom().getName()
                    + "\nStatus: Heute später reserviert"
                    + "\nBenutzer: " + reservation.getAppUser().getDisplayName()
                    + "\nAb: " + reservation.getStartDateTime().format(DATE_TIME_FORMATTER)
                    + "\nBis: " + reservation.getEndDateTime().format(DATE_TIME_FORMATTER);
        }

        return "Tisch: " + desk.getName()
                + "\nBüro: " + officeName
                + "\nRaum: " + desk.getRoom().getName()
                + "\nStatus: Frei";
    }

    private void refreshAfterReservationChange() {
        renderSelectedOffice();
        refreshMyReservations();
    }

    private void renderSelectedOffice() {
        renderOfficeFloorplan(officeField.getValue());
    }

    private void configureAutoRefresh() {
        UI ui = UI.getCurrent();

        if (ui == null) {
            return;
        }

        ui.setPollInterval(10_000);

        ui.addPollListener(event -> {
            renderSelectedOffice();
            refreshMyReservations();
        });
    }

    private LocalTime getNextRoundedTime() {
        LocalTime now = LocalTime.now().plusMinutes(15);
        int minute = now.getMinute();

        int roundedMinute = ((minute + 14) / 15) * 15;

        if (roundedMinute >= 60) {
            return now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        }

        return now.withMinute(roundedMinute).withSecond(0).withNano(0);
    }

    private void showBookingErrorNotification(String message) {
        Dialog errorDialog = createStyledDialog();
        errorDialog.setCloseOnEsc(true);
        errorDialog.setCloseOnOutsideClick(true);

        H3 title = new H3("Buchung nicht möglich");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.35rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.03em")
                .set("color", "#7f1d1d");

        Paragraph text = new Paragraph(message);
        text.getStyle()
                .set("margin", "0")
                .set("font-size", "0.98rem")
                .set("line-height", "1.55")
                .set("font-weight", "650")
                .set("color", "#991b1b");

        Div warningBox = new Div(text);
        warningBox.getStyle()
                .set("padding", "1rem")
                .set("border-radius", "18px")
                .set("background", "#fef2f2")
                .set("border", "1px solid #fecaca")
                .set("box-shadow", "inset 0 0 0 1px rgba(255,255,255,0.7)");

        Button closeButton = new Button("Schließen", event -> errorDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        closeButton.getStyle()
                .set("width", "fit-content")
                .set("border-radius", "14px")
                .set("font-weight", "850")
                .set("padding", "0.62rem 1rem");

        HorizontalLayout actions = new HorizontalLayout(closeButton);
        actions.setPadding(false);
        actions.setSpacing(false);
        actions.setWidthFull();
        actions.getStyle()
                .set("justify-content", "flex-end");

        VerticalLayout content = new VerticalLayout(title, warningBox, actions);
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidth("520px");
        content.setMaxWidth("90vw");
        content.getStyle()
                .set("padding", "0.4rem")
                .set("gap", "1rem")
                .set("font-family", "Inter, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif");

        errorDialog.add(content);
        errorDialog.open();
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
        content.setWidth("720px");
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
}