package io.github.hummelhose.desksharing.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.hummelhose.desksharing.application.service.ReservationService;
import io.github.hummelhose.desksharing.domain.model.Reservation;
import io.github.hummelhose.desksharing.domain.model.ReservationStatus;
import io.github.hummelhose.desksharing.infrastructure.security.AdminAccessService;
import io.github.hummelhose.desksharing.ui.layout.MainLayout;
import io.github.hummelhose.desksharing.ui.layout.ViewFrame;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Route(value = "admin/reservations", layout = MainLayout.class)
@PageTitle("Buchungen verwalten")
@PermitAll
public class AdminReservationView extends VerticalLayout implements BeforeEnterObserver {

    private final ReservationService reservationService;
    private final AdminAccessService adminAccessService;

    private final Grid<Reservation> reservationGrid = new Grid<>(Reservation.class, false);
    private final Div statsContainer = new Div();

    private final TextField searchField = new TextField();
    private final ComboBox<ReservationStatus> statusFilter = new ComboBox<>("Status");

    private final Button allFilterButton = new Button("Alle");
    private final Button runningNowFilterButton = new Button("Läuft gerade");
    private final Button upcomingFilterButton = new Button("Kommend");
    private final Button todayFilterButton = new Button("Heute");
    private final Button pastFilterButton = new Button("Vergangen");
    private final Button cancelledFilterButton = new Button("Storniert");

    private List<Reservation> allReservations = List.of();
    private QuickFilter activeQuickFilter = QuickFilter.ALL;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private enum QuickFilter {
        ALL,
        RUNNING_NOW,
        UPCOMING,
        TODAY,
        PAST,
        CANCELLED
    }

    public AdminReservationView(ReservationService reservationService,
                                AdminAccessService adminAccessService) {
        this.reservationService = reservationService;
        this.adminAccessService = adminAccessService;

        configureStatsContainer();
        configureSearchField();
        configureStatusFilter();
        configureQuickFilterButtons();
        configureGrid();

        ViewFrame headerFrame = createHeaderFrame();
        ViewFrame gridFrame = createGridFrame();

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle()
                .set("background", "linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%)")
                .set("gap", "1rem");

        add(headerFrame, statsContainer, gridFrame);

        refreshGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!adminAccessService.isCurrentUserAdmin()) {
            event.forwardTo(DashboardView.class);
        }
    }

    private ViewFrame createHeaderFrame() {
        Span eyebrow = new Span("ADMIN");
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

        H1 title = new H1("Buchungen verwalten");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.65rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.045em")
                .set("line-height", "1.05")
                .set("color", "#0f172a");

        Paragraph subtitle = new Paragraph("Alle Sitzplatzbuchungen im Überblick. Prüfe laufende, kommende und stornierte Buchungen zentral an einer Stelle.");
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

    private ViewFrame createGridFrame() {
        H3 gridTitle = createSectionTitle("Reservierungen");
        Paragraph gridText = createSectionText("Suche nach Benutzer, E-Mail, Büro, Raum, Sitzplatz, Titel oder Status.");

        HorizontalLayout toolbar = new HorizontalLayout(searchField, statusFilter);
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(Alignment.END);
        toolbar.getStyle()
                .set("gap", "0.75rem")
                .set("flex-wrap", "wrap");

        HorizontalLayout quickFilterBar = new HorizontalLayout(
                allFilterButton,
                runningNowFilterButton,
                upcomingFilterButton,
                todayFilterButton,
                pastFilterButton,
                cancelledFilterButton
        );
        quickFilterBar.setPadding(false);
        quickFilterBar.setSpacing(true);
        quickFilterBar.getStyle()
                .set("gap", "0.55rem")
                .set("flex-wrap", "wrap");

        ViewFrame frame = new ViewFrame(gridTitle, gridText, toolbar, quickFilterBar, reservationGrid);
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

    private void configureStatsContainer() {
        statsContainer.setWidthFull();
        statsContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(210px, 1fr))")
                .set("gap", "1rem");
    }

    private void configureSearchField() {
        searchField.setLabel("Suche");
        searchField.setPlaceholder("Benutzer, Büro, Raum oder Sitzplatz suchen");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidth("420px");
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        styleInputField(searchField);

        searchField.addValueChangeListener(event -> applyFilter());
    }

    private void configureStatusFilter() {
        statusFilter.setItems(ReservationStatus.values());
        statusFilter.setItemLabelGenerator(this::getReservationStatusDisplayName);
        statusFilter.setPlaceholder("Alle Status");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("220px");
        styleInputField(statusFilter);

        statusFilter.addValueChangeListener(event -> applyFilter());
    }

    private void configureQuickFilterButtons() {
        allFilterButton.addClickListener(event -> setQuickFilter(QuickFilter.ALL));
        runningNowFilterButton.addClickListener(event -> setQuickFilter(QuickFilter.RUNNING_NOW));
        upcomingFilterButton.addClickListener(event -> setQuickFilter(QuickFilter.UPCOMING));
        todayFilterButton.addClickListener(event -> setQuickFilter(QuickFilter.TODAY));
        pastFilterButton.addClickListener(event -> setQuickFilter(QuickFilter.PAST));
        cancelledFilterButton.addClickListener(event -> setQuickFilter(QuickFilter.CANCELLED));

        updateQuickFilterButtonStyles();
    }

    private void setQuickFilter(QuickFilter quickFilter) {
        activeQuickFilter = quickFilter;
        updateQuickFilterButtonStyles();
        applyFilter();
    }

    private void updateQuickFilterButtonStyles() {
        styleQuickFilterButton(allFilterButton, activeQuickFilter == QuickFilter.ALL);
        styleQuickFilterButton(runningNowFilterButton, activeQuickFilter == QuickFilter.RUNNING_NOW);
        styleQuickFilterButton(upcomingFilterButton, activeQuickFilter == QuickFilter.UPCOMING);
        styleQuickFilterButton(todayFilterButton, activeQuickFilter == QuickFilter.TODAY);
        styleQuickFilterButton(pastFilterButton, activeQuickFilter == QuickFilter.PAST);
        styleQuickFilterButton(cancelledFilterButton, activeQuickFilter == QuickFilter.CANCELLED);
    }

    private void configureGrid() {
        reservationGrid.addComponentColumn(this::createUserCell)
                .setHeader("Benutzer")
                .setAutoWidth(true)
                .setFlexGrow(1);

        reservationGrid.addComponentColumn(this::createSeatCell)
                .setHeader("Sitzplatz")
                .setAutoWidth(true)
                .setFlexGrow(1);

        reservationGrid.addComponentColumn(this::createTimeCell)
                .setHeader("Zeitraum")
                .setAutoWidth(true)
                .setFlexGrow(0);

        reservationGrid.addComponentColumn(this::createStatusBadge)
                .setHeader("Status")
                .setAutoWidth(true)
                .setFlexGrow(0);

        reservationGrid.addComponentColumn(this::createActionButtons)
                .setHeader("Aktion")
                .setAutoWidth(true)
                .setFlexGrow(0);

        reservationGrid.setWidthFull();
        reservationGrid.setAllRowsVisible(true);
        reservationGrid.setEmptyStateText("Keine Buchungen gefunden.");

        reservationGrid.getStyle()
                .set("border-radius", "18px")
                .set("overflow", "hidden")
                .set("border", "1px solid #e2e8f0")
                .set("box-shadow", "0 10px 24px rgba(15, 23, 42, 0.04)")
                .set("--vaadin-grid-cell-padding", "0.7rem 0.85rem")
                .set("--vaadin-grid-header-background", "#f8fafc")
                .set("--vaadin-grid-header-cell-color", "#334155");
    }

    private Div createUserCell(Reservation reservation) {
        String displayName = getUserName(reservation);
        String email = getUserEmail(reservation);

        Div avatar = new Div();
        avatar.setText(getInitial(displayName));
        avatar.getStyle()
                .set("width", "42px")
                .set("height", "42px")
                .set("border-radius", "14px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-weight", "850")
                .set("color", "#1d4ed8")
                .set("background", "#dbeafe")
                .set("border", "1px solid #bfdbfe")
                .set("flex-shrink", "0");

        Span name = new Span(displayName);
        name.getStyle()
                .set("font-weight", "850")
                .set("color", "#0f172a")
                .set("font-size", "0.95rem");

        Span mail = new Span(email);
        mail.getStyle()
                .set("font-size", "0.84rem")
                .set("font-weight", "600")
                .set("color", "#64748b");

        Div textWrapper = new Div(name, mail);
        textWrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.18rem");

        Div wrapper = new Div(avatar, textWrapper);
        wrapper.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "0.75rem");

        return wrapper;
    }

    private Div createSeatCell(Reservation reservation) {
        Span title = new Span(getResourceName(reservation));
        title.getStyle()
                .set("font-weight", "850")
                .set("color", "#0f172a")
                .set("font-size", "0.95rem");

        Span subtitle = new Span(getRoomName(reservation) + " · " + getOfficeName(reservation));
        subtitle.getStyle()
                .set("font-size", "0.84rem")
                .set("font-weight", "600")
                .set("color", "#64748b");

        Span bookingTitle = new Span(getReservationTitle(reservation));
        bookingTitle.getStyle()
                .set("font-size", "0.8rem")
                .set("font-weight", "650")
                .set("color", "#475569");

        Div wrapper = new Div(title, subtitle, bookingTitle);
        wrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.18rem");

        return wrapper;
    }

    private Div createTimeCell(Reservation reservation) {
        Span start = new Span("Start: " + formatDateTime(reservation.getStartDateTime()));
        start.getStyle()
                .set("font-size", "0.86rem")
                .set("font-weight", "800")
                .set("color", "#0f172a");

        Span end = new Span("Ende: " + formatDateTime(reservation.getEndDateTime()));
        end.getStyle()
                .set("font-size", "0.84rem")
                .set("font-weight", "650")
                .set("color", "#64748b");

        Div wrapper = new Div(start, end);
        wrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.18rem");

        return wrapper;
    }

    private Span createStatusBadge(Reservation reservation) {
        ReservationDisplayStatus displayStatus = getReservationDisplayStatus(reservation);

        Span badge = new Span(displayStatus.label);
        badge.getStyle()
                .set("padding", "0.36rem 0.76rem")
                .set("border-radius", "999px")
                .set("font-size", "0.78rem")
                .set("font-weight", "850")
                .set("letter-spacing", "0.02em")
                .set("display", "inline-block")
                .set("background", displayStatus.background)
                .set("color", displayStatus.color)
                .set("border", "1px solid " + displayStatus.border);

        return badge;
    }

    private Component createActionButtons(Reservation reservation) {
        Button detailsButton = new Button("Details");
        detailsButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        detailsButton.getStyle()
                .set("border-radius", "12px")
                .set("font-weight", "800")
                .set("background", "#ffffff")
                .set("color", "#334155")
                .set("border", "1px solid #dbe2ea");

        detailsButton.addClickListener(event -> openReservationDetailsDialog(reservation));

        HorizontalLayout actions = new HorizontalLayout(detailsButton);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setAlignItems(Alignment.CENTER);
        actions.getStyle()
                .set("gap", "0.5rem")
                .set("flex-wrap", "wrap");

        if (canCancelReservation(reservation)) {
            Button cancelButton = new Button("Stornieren");
            cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            cancelButton.getStyle()
                    .set("border-radius", "12px")
                    .set("font-weight", "800");

            cancelButton.addClickListener(event -> openCancelConfirmationDialog(reservation));
            actions.add(cancelButton);
        }

        return actions;
    }

    private void openReservationDetailsDialog(Reservation reservation) {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Buchungsdetails");
        Paragraph text = createDialogText("Hier siehst du alle Informationen zu dieser Sitzplatzbuchung.");

        Div detailsBox = createReservationDetailsBox(reservation, false);

        Button closeButton = new Button("Schließen", event -> dialog.close());
        styleSecondaryButton(closeButton);

        HorizontalLayout actions = new HorizontalLayout(closeButton);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setWidthFull();
        actions.getStyle()
                .set("justify-content", "flex-end")
                .set("gap", "0.75rem")
                .set("flex-wrap", "wrap");

        if (canCancelReservation(reservation)) {
            Button cancelButton = new Button("Buchung stornieren");
            styleDangerButton(cancelButton);
            cancelButton.addClickListener(event -> {
                dialog.close();
                openCancelConfirmationDialog(reservation);
            });

            actions.addComponentAsFirst(cancelButton);
        }

        VerticalLayout content = createDialogContent(title, text, detailsBox, actions);
        dialog.add(content);
        dialog.open();
    }

    private void openCancelConfirmationDialog(Reservation reservation) {
        Dialog dialog = createStyledDialog();

        H3 title = createDialogTitle("Buchung wirklich stornieren?");

        Paragraph warningText = new Paragraph("Diese Buchung wird nicht gelöscht, sondern sauber auf STORNIERT gesetzt. Die Historie bleibt erhalten.");
        warningText.getStyle()
                .set("margin", "0")
                .set("color", "#7f1d1d")
                .set("font-size", "0.95rem")
                .set("font-weight", "700")
                .set("line-height", "1.55");

        Div warningBox = new Div(warningText);
        warningBox.getStyle()
                .set("padding", "1rem")
                .set("border-radius", "18px")
                .set("background", "#fef2f2")
                .set("border", "1px solid #fecaca")
                .set("box-shadow", "inset 0 0 0 1px rgba(255,255,255,0.7)");

        Div detailsBox = createReservationDetailsBox(reservation, true);

        Button cancelButton = new Button("Abbrechen", event -> dialog.close());
        styleSecondaryButton(cancelButton);

        Button confirmButton = new Button("Jetzt stornieren");
        styleDangerButton(confirmButton);

        confirmButton.addClickListener(event -> {
            dialog.close();
            cancelReservation(reservation);
        });

        HorizontalLayout actions = new HorizontalLayout(cancelButton, confirmButton);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setWidthFull();
        actions.getStyle()
                .set("justify-content", "flex-end")
                .set("gap", "0.75rem")
                .set("flex-wrap", "wrap");

        VerticalLayout content = createDialogContent(title, warningBox, detailsBox, actions);
        dialog.add(content);
        dialog.open();
    }

    private void cancelReservation(Reservation reservation) {
        reservationService.cancelReservation(reservation.getId())
                .ifPresentOrElse(cancelledReservation -> {
                    refreshGrid();
                    showResultDialog(
                            "Buchung storniert",
                            "Die Buchung wurde erfolgreich storniert und bleibt in der Historie sichtbar.",
                            true
                    );
                }, () -> showResultDialog(
                        "Stornierung fehlgeschlagen",
                        "Die Buchung konnte nicht storniert werden. Bitte lade die Seite neu und versuche es erneut.",
                        false
                ));
    }

    private Div createReservationDetailsBox(Reservation reservation, boolean dangerStyle) {
        Div detailsBox = new Div(
                createDetailRow("Status", getReservationDisplayStatus(reservation).label),
                createDetailRow("Benutzer", getUserName(reservation)),
                createDetailRow("E-Mail", getUserEmail(reservation)),
                createDetailRow("Sitzplatz", getResourceName(reservation)),
                createDetailRow("Raum", getRoomName(reservation)),
                createDetailRow("Büro", getOfficeName(reservation)),
                createDetailRow("Start", formatDateTime(reservation.getStartDateTime())),
                createDetailRow("Ende", formatDateTime(reservation.getEndDateTime())),
                createDetailRow("Titel", getReservationTitle(reservation)),
                createDetailRow("Notiz", getReservationNotes(reservation))
        );

        detailsBox.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(240px, 1fr))")
                .set("gap", "0.75rem")
                .set("padding", "1rem")
                .set("border-radius", "20px")
                .set("background", dangerStyle ? "#fff7ed" : "#f8fafc")
                .set("border", dangerStyle ? "1px solid #fed7aa" : "1px solid #e2e8f0");

        return detailsBox;
    }

    private Div createDetailRow(String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "0.74rem")
                .set("font-weight", "850")
                .set("letter-spacing", "0.04em")
                .set("text-transform", "uppercase")
                .set("color", "#64748b");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "0.94rem")
                .set("font-weight", "800")
                .set("color", "#0f172a")
                .set("line-height", "1.35")
                .set("word-break", "break-word");

        Div row = new Div(labelSpan, valueSpan);
        row.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.25rem")
                .set("padding", "0.75rem")
                .set("border-radius", "16px")
                .set("background", "#ffffff")
                .set("border", "1px solid rgba(226, 232, 240, 0.9)");

        return row;
    }

    private void showResultDialog(String titleText, String messageText, boolean success) {
        Dialog dialog = createStyledDialog();
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        H3 title = createDialogTitle(titleText);
        title.getStyle()
                .set("color", success ? "#166534" : "#7f1d1d");

        Paragraph message = new Paragraph(messageText);
        message.getStyle()
                .set("margin", "0")
                .set("font-size", "0.98rem")
                .set("line-height", "1.55")
                .set("font-weight", "650")
                .set("color", success ? "#166534" : "#991b1b");

        Div messageBox = new Div(message);
        messageBox.getStyle()
                .set("padding", "1rem")
                .set("border-radius", "18px")
                .set("background", success ? "#f0fdf4" : "#fef2f2")
                .set("border", success ? "1px solid #bbf7d0" : "1px solid #fecaca");

        Button closeButton = new Button("Schließen", event -> dialog.close());

        if (success) {
            stylePrimaryButton(closeButton);
        } else {
            styleDangerButton(closeButton);
        }

        HorizontalLayout actions = new HorizontalLayout(closeButton);
        actions.setPadding(false);
        actions.setWidthFull();
        actions.getStyle()
                .set("justify-content", "flex-end");

        VerticalLayout content = createDialogContent(title, messageBox, actions);
        content.setWidth("540px");

        dialog.add(content);
        dialog.open();
    }

    private void refreshGrid() {
        allReservations = reservationService.getAllReservations().stream()
                .sorted(Comparator.comparing(Reservation::getStartDateTime).reversed())
                .toList();

        applyFilter();
    }

    private void applyFilter() {
        ReservationStatus selectedStatus = statusFilter.getValue();
        String query = searchField.getValue();

        List<Reservation> filteredReservations = allReservations.stream()
                .filter(reservation -> selectedStatus == null || reservation.getStatus() == selectedStatus)
                .filter(this::matchesQuickFilter)
                .filter(reservation -> matchesSearch(reservation, query))
                .toList();

        reservationGrid.setItems(filteredReservations);
        refreshStats(allReservations, filteredReservations);
    }

    private boolean matchesQuickFilter(Reservation reservation) {
        LocalDateTime now = LocalDateTime.now();

        return switch (activeQuickFilter) {
            case ALL -> true;
            case RUNNING_NOW -> isRunningNow(reservation, now);
            case UPCOMING -> isUpcoming(reservation, now);
            case TODAY -> isToday(reservation);
            case PAST -> reservation.getEndDateTime().isBefore(now) || reservation.getEndDateTime().isEqual(now);
            case CANCELLED -> reservation.getStatus() == ReservationStatus.CANCELLED;
        };
    }

    private boolean matchesSearch(Reservation reservation, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase();

        String userName = getUserName(reservation).toLowerCase();
        String email = getUserEmail(reservation).toLowerCase();
        String resourceName = getResourceName(reservation).toLowerCase();
        String roomName = getRoomName(reservation).toLowerCase();
        String officeName = getOfficeName(reservation).toLowerCase();
        String status = getReservationDisplayStatus(reservation).label.toLowerCase();
        String technicalStatus = reservation.getStatus() != null ? reservation.getStatus().name().toLowerCase() : "";
        String title = getReservationTitle(reservation).toLowerCase();
        String notes = getReservationNotes(reservation).toLowerCase();

        return userName.contains(normalizedQuery)
                || email.contains(normalizedQuery)
                || resourceName.contains(normalizedQuery)
                || roomName.contains(normalizedQuery)
                || officeName.contains(normalizedQuery)
                || status.contains(normalizedQuery)
                || technicalStatus.contains(normalizedQuery)
                || title.contains(normalizedQuery)
                || notes.contains(normalizedQuery);
    }

    private void refreshStats(List<Reservation> reservations, List<Reservation> filteredReservations) {
        statsContainer.removeAll();

        LocalDateTime now = LocalDateTime.now();

        long total = reservations.size();

        long active = reservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE)
                .count();

        long runningNow = reservations.stream()
                .filter(reservation -> isRunningNow(reservation, now))
                .count();

        long future = reservations.stream()
                .filter(reservation -> isUpcoming(reservation, now))
                .count();

        long today = reservations.stream()
                .filter(this::isToday)
                .count();

        long cancelled = reservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CANCELLED)
                .count();

        long hits = filteredReservations.size();

        statsContainer.add(
                createStatCard("Alle Buchungen", String.valueOf(total), "Gesamte Reservierungshistorie", VaadinIcon.CALENDAR, "#2563eb"),
                createStatCard("Aktiv", String.valueOf(active), "Aktive Buchungen im System", VaadinIcon.CHECK_CIRCLE, "#16a34a"),
                createStatCard("Läuft gerade", String.valueOf(runningNow), "Aktuell belegte Sitzplätze", VaadinIcon.CLOCK, "#dc2626"),
                createStatCard("Kommend", String.valueOf(future), "Noch bevorstehende Buchungen", VaadinIcon.CALENDAR_CLOCK, "#f59e0b"),
                createStatCard("Heute", String.valueOf(today), "Buchungen mit Bezug zu heute", VaadinIcon.CALENDAR, "#0f766e"),
                createStatCard("Storniert", String.valueOf(cancelled), "Stornierte Reservierungen", VaadinIcon.CLOSE_CIRCLE, "#64748b"),
                createStatCard("Treffer", String.valueOf(hits), "Aktuell sichtbare Einträge", VaadinIcon.SEARCH, "#7c3aed")
        );
    }

    private Div createStatCard(String title,
                               String value,
                               String description,
                               VaadinIcon icon,
                               String accentColor) {
        Icon cardIcon = icon.create();
        cardIcon.getStyle()
                .set("width", "1.22rem")
                .set("height", "1.22rem")
                .set("color", accentColor);

        Div iconBubble = new Div(cardIcon);
        iconBubble.getStyle()
                .set("width", "42px")
                .set("height", "42px")
                .set("border-radius", "14px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background", "#f8fafc")
                .set("border", "1px solid #e2e8f0");

        Span titleLabel = new Span(title);
        titleLabel.getStyle()
                .set("font-size", "0.95rem")
                .set("font-weight", "850")
                .set("color", "#334155");

        Div valueLabel = new Div();
        valueLabel.setText(value);
        valueLabel.getStyle()
                .set("font-size", "2.2rem")
                .set("font-weight", "850")
                .set("line-height", "1")
                .set("letter-spacing", "-0.055em")
                .set("color", accentColor);

        Paragraph descriptionLabel = new Paragraph(description);
        descriptionLabel.getStyle()
                .set("margin", "0")
                .set("font-size", "0.88rem")
                .set("line-height", "1.45")
                .set("color", "#64748b");

        Div card = new Div(iconBubble, titleLabel, valueLabel, descriptionLabel);
        card.getStyle()
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.7rem")
                .set("min-height", "160px")
                .set("background", "rgba(255, 255, 255, 0.96)")
                .set("border", "1px solid rgba(226, 232, 240, 0.92)")
                .set("border-radius", "24px")
                .set("padding", "1.35rem")
                .set("box-shadow", "0 18px 45px rgba(15, 23, 42, 0.08)");

        addHoverEffect(card);

        return card;
    }

    private ReservationDisplayStatus getReservationDisplayStatus(Reservation reservation) {
        LocalDateTime now = LocalDateTime.now();

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return new ReservationDisplayStatus("Storniert", "#f1f5f9", "#475569", "#cbd5e1");
        }

        if (isRunningNow(reservation, now)) {
            return new ReservationDisplayStatus("Läuft gerade", "#fee2e2", "#991b1b", "#fecaca");
        }

        if (isUpcoming(reservation, now)) {
            return new ReservationDisplayStatus("Kommend", "#fffbeb", "#92400e", "#fde68a");
        }

        if (reservation.getStatus() == ReservationStatus.ACTIVE
                && (reservation.getEndDateTime().isBefore(now) || reservation.getEndDateTime().isEqual(now))) {
            return new ReservationDisplayStatus("Vergangen", "#f8fafc", "#64748b", "#e2e8f0");
        }

        return new ReservationDisplayStatus("Aktiv", "#dcfce7", "#166534", "#bbf7d0");
    }

    private boolean isRunningNow(Reservation reservation, LocalDateTime now) {
        return reservation.getStatus() == ReservationStatus.ACTIVE
                && !reservation.getStartDateTime().isAfter(now)
                && reservation.getEndDateTime().isAfter(now);
    }

    private boolean isUpcoming(Reservation reservation, LocalDateTime now) {
        return reservation.getStatus() == ReservationStatus.ACTIVE
                && reservation.getStartDateTime().isAfter(now);
    }

    private boolean isToday(Reservation reservation) {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        return reservation.getStartDateTime().isBefore(dayEnd)
                && reservation.getEndDateTime().isAfter(dayStart);
    }

    private boolean canCancelReservation(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.ACTIVE
                && reservation.getEndDateTime().isAfter(LocalDateTime.now());
    }

    private String getReservationStatusDisplayName(ReservationStatus status) {
        if (status == ReservationStatus.ACTIVE) {
            return "Aktiv";
        }

        if (status == ReservationStatus.CANCELLED) {
            return "Storniert";
        }

        return status.name();
    }

    private String getUserName(Reservation reservation) {
        if (reservation.getAppUser() == null
                || reservation.getAppUser().getDisplayName() == null
                || reservation.getAppUser().getDisplayName().isBlank()) {
            return "Unbekannter Benutzer";
        }

        return reservation.getAppUser().getDisplayName();
    }

    private String getUserEmail(Reservation reservation) {
        if (reservation.getAppUser() == null
                || reservation.getAppUser().getEmail() == null
                || reservation.getAppUser().getEmail().isBlank()) {
            return "Keine E-Mail";
        }

        return reservation.getAppUser().getEmail();
    }

    private String getResourceName(Reservation reservation) {
        if (reservation.getResource() == null
                || reservation.getResource().getName() == null
                || reservation.getResource().getName().isBlank()) {
            return "Unbekannter Sitzplatz";
        }

        return reservation.getResource().getName();
    }

    private String getRoomName(Reservation reservation) {
        if (reservation.getResource() == null
                || reservation.getResource().getRoom() == null
                || reservation.getResource().getRoom().getName() == null
                || reservation.getResource().getRoom().getName().isBlank()) {
            return "Ohne Raum";
        }

        return reservation.getResource().getRoom().getName();
    }

    private String getOfficeName(Reservation reservation) {
        if (reservation.getResource() == null
                || reservation.getResource().getRoom() == null
                || reservation.getResource().getRoom().getOffice() == null
                || reservation.getResource().getRoom().getOffice().getName() == null
                || reservation.getResource().getRoom().getOffice().getName().isBlank()) {
            return "Ohne Büro";
        }

        return reservation.getResource().getRoom().getOffice().getName();
    }

    private String getReservationTitle(Reservation reservation) {
        if (reservation.getTitle() == null || reservation.getTitle().isBlank()) {
            return "Sitzplatzbuchung";
        }

        return reservation.getTitle();
    }

    private String getReservationNotes(Reservation reservation) {
        if (reservation.getNotes() == null || reservation.getNotes().isBlank()) {
            return "Keine Notiz";
        }

        return reservation.getNotes();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }

        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private String getInitial(String text) {
        if (text == null || text.isBlank()) {
            return "?";
        }

        return text.substring(0, 1).toUpperCase();
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
        content.setWidth("760px");
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

    private void styleQuickFilterButton(Button button, boolean active) {
        if (active) {
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            button.getStyle()
                    .set("background", "linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)")
                    .set("color", "#ffffff")
                    .set("border", "none")
                    .set("border-radius", "999px")
                    .set("font-weight", "850")
                    .set("padding", "0.55rem 0.9rem")
                    .set("box-shadow", "0 12px 24px rgba(37, 99, 235, 0.18)");
        } else {
            button.getThemeNames().remove("primary");
            button.getStyle()
                    .set("background", "#ffffff")
                    .set("color", "#334155")
                    .set("border", "1px solid #dbe2ea")
                    .set("border-radius", "999px")
                    .set("font-weight", "800")
                    .set("padding", "0.55rem 0.9rem")
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

    private void styleDangerButton(Button button) {
        button.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        button.getStyle()
                .set("border-radius", "14px")
                .set("font-weight", "850")
                .set("padding", "0.62rem 1rem")
                .set("box-shadow", "0 12px 24px rgba(220, 38, 38, 0.18)");
    }

    private void addHoverEffect(Div card) {
        card.getStyle()
                .set("transition", "transform 150ms ease, box-shadow 150ms ease");

        card.getElement().executeJs("""
                const card = this;
                const originalBoxShadow = card.style.boxShadow;

                card.addEventListener('mouseenter', () => {
                    card.style.transform = 'translateY(-3px)';
                    card.style.boxShadow = '0 24px 52px rgba(15, 23, 42, 0.14)';
                });

                card.addEventListener('mouseleave', () => {
                    card.style.transform = 'translateY(0)';
                    card.style.boxShadow = originalBoxShadow;
                });
                """);
    }

    private record ReservationDisplayStatus(String label, String background, String color, String border) {
    }
}