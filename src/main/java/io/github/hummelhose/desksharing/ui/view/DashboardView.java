package io.github.hummelhose.desksharing.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.hummelhose.desksharing.application.service.OfficeService;
import io.github.hummelhose.desksharing.application.service.ReservationService;
import io.github.hummelhose.desksharing.application.service.ResourceService;
import io.github.hummelhose.desksharing.application.service.RoomService;
import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import io.github.hummelhose.desksharing.domain.model.ReservationStatus;
import io.github.hummelhose.desksharing.domain.model.ResourceType;
import io.github.hummelhose.desksharing.infrastructure.security.CurrentUserService;
import io.github.hummelhose.desksharing.ui.layout.MainLayout;
import io.github.hummelhose.desksharing.ui.layout.ViewFrame;
import io.github.hummelhose.desksharing.ui.view.AdminReservationView;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDateTime;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Startseite")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView(OfficeService officeService,
                         RoomService roomService,
                         ResourceService resourceService,
                         ReservationService reservationService,
                         CurrentUserService currentUserService) {

        AppUser currentUser = currentUserService.getOrCreateCurrentUser();
        boolean admin = currentUser.getRole() == AppRole.ADMIN;
        LocalDateTime now = LocalDateTime.now();

        long officeCount = officeService.getAllOffices().size();

        long activeOfficeCount = officeService.getAllActiveOffices().size();

        long roomCount = roomService.getAllRooms().size();

        long deskCount = resourceService.getAllResources().stream()
                .filter(resource -> resource.getResourceType() == ResourceType.DESK)
                .count();

        long bookableDeskCount = resourceService.getAllResources().stream()
                .filter(resource -> resource.getResourceType() == ResourceType.DESK)
                .filter(resource -> resource.isActive() && resource.isBookable())
                .count();

        long occupiedNowCount = reservationService.getAllReservations().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE)
                .filter(reservation -> reservation.getResource().getResourceType() == ResourceType.DESK)
                .filter(reservation -> !reservation.getStartDateTime().isAfter(now))
                .filter(reservation -> reservation.getEndDateTime().isAfter(now))
                .count();

        long freeNowCount = Math.max(bookableDeskCount - occupiedNowCount, 0);

        long upcomingReservationCount = reservationService.getAllReservations().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE)
                .filter(reservation -> reservation.getResource().getResourceType() == ResourceType.DESK)
                .filter(reservation -> reservation.getEndDateTime().isAfter(now))
                .count();

        long myUpcomingReservations = reservationService.getAllReservations().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE)
                .filter(reservation -> reservation.getResource().getResourceType() == ResourceType.DESK)
                .filter(reservation -> reservation.getAppUser().getId().equals(currentUser.getId()))
                .filter(reservation -> reservation.getEndDateTime().isAfter(now))
                .count();

        int utilizationPercent = bookableDeskCount == 0
                ? 0
                : Math.round((occupiedNowCount * 100f) / bookableDeskCount);

        ViewFrame headerFrame = createHeaderFrame(admin, currentUser);

        Div statsGrid = admin
                ? createAdminStatsGrid(
                officeCount,
                activeOfficeCount,
                roomCount,
                deskCount,
                freeNowCount,
                occupiedNowCount,
                upcomingReservationCount
        )
                : createUserStatsGrid(
                myUpcomingReservations,
                freeNowCount,
                occupiedNowCount,
                utilizationPercent
        );

        ViewFrame occupancyFrame = createOccupancyFrame(
                freeNowCount,
                occupiedNowCount,
                bookableDeskCount,
                utilizationPercent
        );

        ViewFrame actionFrame = admin
                ? createAdminActionFrame()
                : createUserActionFrame();

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle()
                .set("background", "linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%)")
                .set("gap", "1rem");

        add(headerFrame, statsGrid, occupancyFrame, actionFrame);
    }

    private ViewFrame createHeaderFrame(boolean admin, AppUser currentUser) {
        Span eyebrow = new Span(admin ? "ADMIN-ÜBERSICHT" : "ÜBERSICHT");
        eyebrow.getStyle()
                .set("display", "inline-flex")
                .set("width", "fit-content")
                .set("padding", "0.38rem 0.72rem")
                .set("border-radius", "999px")
                .set("font-size", "0.72rem")
                .set("font-weight", "850")
                .set("letter-spacing", "0.08em")
                .set("color", "#1d4ed8")
                .set("background", "#dbeafe")
                .set("border", "1px solid #bfdbfe");

        H1 title = new H1(admin ? "Admin-Startseite" : "Meine Startseite");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2.25rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.045em")
                .set("line-height", "1.05")
                .set("color", "#0f172a");

        Paragraph subtitle = new Paragraph(admin
                ? "Verwalte Büros, Räume, Tische und Benutzer zentral. Behalte Belegung und kommende Buchungen im Blick."
                : "Willkommen zurück, " + currentUser.getDisplayName() + ". Buche deinen Sitzplatz direkt im Sitzplan und verwalte deine Reservierungen.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("max-width", "860px")
                .set("color", "#475569")
                .set("font-size", "1rem")
                .set("line-height", "1.65");

        ViewFrame frame = new ViewFrame(eyebrow, title, subtitle);
        frame.getStyle()
                .set("padding", "1.35rem 1.5rem")
                .set("gap", "0.6rem");

        return frame;
    }

    private Div createAdminStatsGrid(long officeCount,
                                     long activeOfficeCount,
                                     long roomCount,
                                     long deskCount,
                                     long freeNowCount,
                                     long occupiedNowCount,
                                     long upcomingReservationCount) {
        Div grid = new Div(
                createStatCard(
                        "Büros",
                        String.valueOf(officeCount),
                        activeOfficeCount + " aktive Büros / Standorte",
                        VaadinIcon.OFFICE,
                        "#2563eb"
                ),
                createStatCard(
                        "Räume",
                        String.valueOf(roomCount),
                        "Angelegte Räume in allen Büros",
                        VaadinIcon.GRID_BIG,
                        "#7c3aed"
                ),
                createStatCard(
                        "Tische",
                        String.valueOf(deskCount),
                        "Alle angelegten Tische",
                        VaadinIcon.CUBE,
                        "#0f766e"
                ),
                createStatCard(
                        "Frei jetzt",
                        String.valueOf(freeNowCount),
                        "Aktuell freie buchbare Tische",
                        VaadinIcon.CHECK_CIRCLE,
                        "#16a34a"
                ),
                createStatCard(
                        "Jetzt belegt",
                        String.valueOf(occupiedNowCount),
                        "Aktuell laufende Buchungen",
                        VaadinIcon.CLOCK,
                        "#dc2626"
                ),
                createStatCard(
                        "Kommend",
                        String.valueOf(upcomingReservationCount),
                        "Aktive und zukünftige Buchungen",
                        VaadinIcon.CALENDAR,
                        "#f59e0b"
                )
        );

        styleStatsGrid(grid);
        return grid;
    }

    private Div createUserStatsGrid(long myUpcomingReservations,
                                    long freeNowCount,
                                    long occupiedNowCount,
                                    int utilizationPercent) {
        Div grid = new Div(
                createStatCard(
                        "Meine Buchungen",
                        String.valueOf(myUpcomingReservations),
                        "Aktive und zukünftige Reservierungen",
                        VaadinIcon.CALENDAR,
                        "#2563eb"
                ),
                createStatCard(
                        "Frei jetzt",
                        String.valueOf(freeNowCount),
                        "Aktuell freie buchbare Tische",
                        VaadinIcon.CHECK_CIRCLE,
                        "#16a34a"
                ),
                createStatCard(
                        "Belegt jetzt",
                        String.valueOf(occupiedNowCount),
                        "Aktuell belegte Tische",
                        VaadinIcon.CLOCK,
                        "#dc2626"
                ),
                createStatCard(
                        "Auslastung",
                        utilizationPercent + "%",
                        "Aktuelle Tischbelegung",
                        VaadinIcon.CHART,
                        "#f59e0b"
                )
        );

        styleStatsGrid(grid);
        return grid;
    }

    private void styleStatsGrid(Div grid) {
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(220px, 1fr))")
                .set("gap", "1rem")
                .set("width", "100%");
    }

    private ViewFrame createOccupancyFrame(long freeNowCount,
                                           long occupiedNowCount,
                                           long bookableDeskCount,
                                           int utilizationPercent) {
        H3 title = createSectionTitle("Aktuelle Belegung");

        Paragraph text = new Paragraph(
                bookableDeskCount == 0
                        ? "Aktuell sind noch keine buchbaren Tische vorhanden."
                        : occupiedNowCount + " von " + bookableDeskCount + " buchbaren Tischen sind aktuell belegt."
        );
        text.getStyle()
                .set("margin", "0")
                .set("color", "#64748b")
                .set("line-height", "1.55");

        Div progressOuter = new Div();
        progressOuter.getStyle()
                .set("width", "100%")
                .set("height", "16px")
                .set("border-radius", "999px")
                .set("background", "#e2e8f0")
                .set("overflow", "hidden")
                .set("box-shadow", "inset 0 0 0 1px rgba(148, 163, 184, 0.18)");

        Div progressInner = new Div();
        progressInner.getStyle()
                .set("height", "100%")
                .set("width", utilizationPercent + "%")
                .set("border-radius", "999px")
                .set("background", getUtilizationColor(utilizationPercent))
                .set("transition", "width 180ms ease");

        progressOuter.add(progressInner);

        Div statusRow = new Div(
                createMiniStatus("Frei", String.valueOf(freeNowCount), "#16a34a"),
                createMiniStatus("Belegt", String.valueOf(occupiedNowCount), "#dc2626"),
                createMiniStatus("Auslastung", utilizationPercent + "%", "#2563eb")
        );
        statusRow.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(160px, 1fr))")
                .set("gap", "0.75rem")
                .set("width", "100%");

        ViewFrame frame = new ViewFrame(title, text, progressOuter, statusRow);
        frame.getStyle()
                .set("padding", "1.35rem 1.5rem")
                .set("gap", "1rem");

        return frame;
    }

    private Div createMiniStatus(String label, String value, String color) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "0.78rem")
                .set("font-weight", "800")
                .set("color", "#64748b")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.04em");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "1.3rem")
                .set("font-weight", "850")
                .set("color", color)
                .set("line-height", "1");

        Div box = new Div(labelSpan, valueSpan);
        box.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.3rem")
                .set("padding", "0.85rem 1rem")
                .set("border-radius", "18px")
                .set("background", "#f8fafc")
                .set("border", "1px solid #e2e8f0");

        return box;
    }

    private String getUtilizationColor(int utilizationPercent) {
        if (utilizationPercent >= 80) {
            return "linear-gradient(135deg, #ef4444 0%, #dc2626 100%)";
        }

        if (utilizationPercent >= 50) {
            return "linear-gradient(135deg, #f59e0b 0%, #d97706 100%)";
        }

        return "linear-gradient(135deg, #22c55e 0%, #16a34a 100%)";
    }

    private ViewFrame createAdminActionFrame() {
        H3 title = createSectionTitle("Schnellzugriffe");

        Paragraph text = new Paragraph("Springe direkt zu den wichtigsten Bereichen deiner DeskSharing-Verwaltung.");
        text.getStyle()
                .set("margin", "0")
                .set("color", "#64748b")
                .set("line-height", "1.55");

        Div actionGrid = new Div(
                createActionCard(
                        "Sitzplan öffnen",
                        "Benutzeransicht mit Buchungen und aktuellem Belegungsstatus.",
                        VaadinIcon.GRID_SMALL,
                        FloorplanView.class
                ),
                createActionCard(
                        "Plan-Editor",
                        "Büros, Räume und Tische visuell verwalten.",
                        VaadinIcon.EDIT,
                        AdminFloorplanEditorView.class
                ),
                createActionCard(
                        "Buchungen verwalten",
                        "Alle Sitzplatzbuchungen prüfen, filtern und bei Bedarf stornieren.",
                        VaadinIcon.CALENDAR,
                        AdminReservationView.class
                ),
                createActionCard(
                        "Benutzer verwalten",
                        "Benutzerrollen verwalten und Admin-Rechte vergeben.",
                        VaadinIcon.USERS,
                        UserManagementView.class
                )
        );

        styleActionGrid(actionGrid);

        return new ViewFrame(title, text, actionGrid);
    }

    private ViewFrame createUserActionFrame() {
        H3 title = createSectionTitle("Schnellzugriffe");

        Paragraph text = new Paragraph("Öffne den Sitzplan und buche direkt einen freien Arbeitsplatz.");
        text.getStyle()
                .set("margin", "0")
                .set("color", "#64748b")
                .set("line-height", "1.55");

        Div actionGrid = new Div(
                createActionCard(
                        "Sitzplatz buchen",
                        "Büro auswählen, freien Tisch anklicken und Zeitraum buchen.",
                        VaadinIcon.GRID_SMALL,
                        FloorplanView.class
                )
        );

        styleActionGrid(actionGrid);

        return new ViewFrame(title, text, actionGrid);
    }

    private H3 createSectionTitle(String text) {
        H3 title = new H3(text);
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.25rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.025em")
                .set("color", "#0f172a");
        return title;
    }

    private void styleActionGrid(Div grid) {
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(260px, 1fr))")
                .set("gap", "1rem")
                .set("width", "100%");
    }

    private Div createStatCard(String title,
                               String value,
                               String description,
                               VaadinIcon icon,
                               String accentColor) {
        Icon cardIcon = icon.create();
        cardIcon.getStyle()
                .set("width", "1.25rem")
                .set("height", "1.25rem")
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

        H3 titleLabel = new H3(title);
        titleLabel.getStyle()
                .set("margin", "0")
                .set("font-size", "0.96rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.01em")
                .set("color", "#334155");

        Div valueLabel = new Div();
        valueLabel.setText(value);
        valueLabel.getStyle()
                .set("font-size", "2.35rem")
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
                .set("gap", "0.75rem")
                .set("min-height", "180px")
                .set("background", "rgba(255, 255, 255, 0.96)")
                .set("border", "1px solid rgba(226, 232, 240, 0.92)")
                .set("border-radius", "24px")
                .set("padding", "1.35rem")
                .set("box-shadow", "0 18px 45px rgba(15, 23, 42, 0.08)");

        addHoverEffect(card);

        return card;
    }

    private Div createActionCard(String title,
                                 String description,
                                 VaadinIcon icon,
                                 Class<? extends Component> navigationTarget) {
        Icon actionIcon = icon.create();
        actionIcon.getStyle()
                .set("width", "1.35rem")
                .set("height", "1.35rem")
                .set("color", "#bfdbfe");

        Div iconBox = new Div(actionIcon);
        iconBox.getStyle()
                .set("width", "48px")
                .set("height", "48px")
                .set("border-radius", "16px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background", "rgba(37, 99, 235, 0.26)")
                .set("border", "1px solid rgba(147, 197, 253, 0.18)");

        H3 titleLabel = new H3(title);
        titleLabel.getStyle()
                .set("margin", "0")
                .set("font-size", "1.05rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.02em")
                .set("color", "#f8fafc");

        Paragraph descriptionLabel = new Paragraph(description);
        descriptionLabel.getStyle()
                .set("margin", "0")
                .set("font-size", "0.9rem")
                .set("line-height", "1.5")
                .set("color", "#cbd5e1");

        Button openButton = new Button("Öffnen", event -> UI.getCurrent().navigate(navigationTarget));
        openButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        openButton.getStyle()
                .set("width", "fit-content")
                .set("background", "#ffffff")
                .set("color", "#1d4ed8")
                .set("border", "none")
                .set("border-radius", "12px")
                .set("font-weight", "850")
                .set("padding", "0.55rem 0.95rem")
                .set("box-shadow", "0 14px 28px rgba(15, 23, 42, 0.22)");

        Div card = new Div(iconBox, titleLabel, descriptionLabel, openButton);
        card.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.85rem")
                .set("padding", "1.35rem")
                .set("border-radius", "24px")
                .set("background", "linear-gradient(135deg, #0f172a 0%, #1e3a8a 100%)")
                .set("border", "1px solid rgba(147, 197, 253, 0.20)")
                .set("box-shadow", "0 18px 45px rgba(15, 23, 42, 0.16)");

        addHoverEffect(card);

        return card;
    }

    private void addHoverEffect(Div card) {
        card.getStyle()
                .set("transition", "transform 150ms ease, box-shadow 150ms ease");

        card.getElement().executeJs("""
                const card = this;

                card.addEventListener('mouseenter', () => {
                    card.style.transform = 'translateY(-3px)';
                    card.style.boxShadow = '0 24px 52px rgba(15, 23, 42, 0.14)';
                });

                card.addEventListener('mouseleave', () => {
                    card.style.transform = 'translateY(0)';
                    card.style.boxShadow = card.style.boxShadow;
                });
                """);
    }
}