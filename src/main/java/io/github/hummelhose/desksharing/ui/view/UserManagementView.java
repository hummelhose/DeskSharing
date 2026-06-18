package io.github.hummelhose.desksharing.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.hummelhose.desksharing.application.service.AppUserService;
import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import io.github.hummelhose.desksharing.infrastructure.security.AdminAccessService;
import io.github.hummelhose.desksharing.infrastructure.security.CurrentUserService;
import io.github.hummelhose.desksharing.ui.layout.MainLayout;
import io.github.hummelhose.desksharing.ui.layout.ViewFrame;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("Benutzerverwaltung")
@PermitAll
public class UserManagementView extends VerticalLayout implements BeforeEnterObserver {

    private final AppUserService appUserService;
    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;

    private final Grid<AppUser> userGrid = new Grid<>(AppUser.class, false);
    private final Div statsContainer = new Div();
    private final TextField searchField = new TextField();

    private List<AppUser> allUsers = List.of();

    public UserManagementView(AppUserService appUserService,
                              CurrentUserService currentUserService,
                              AdminAccessService adminAccessService) {
        this.appUserService = appUserService;
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;

        configureStatsContainer();
        configureSearchField();
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
        Span eyebrow = new Span("VERWALTUNG");
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

        H1 title = new H1("Benutzerverwaltung");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.65rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.045em")
                .set("line-height", "1.05")
                .set("color", "#0f172a");

        Paragraph subtitle = new Paragraph("Benutzer prüfen, Rollen verwalten und Admin-Zugriffe sauber steuern.");
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
        H3 gridTitle = createSectionTitle("Benutzer");
        Paragraph gridText = createSectionText("Alle Benutzerkonten und deren aktuelle Rollen im Überblick.");

        HorizontalLayout toolbar = new HorizontalLayout(searchField);
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(Alignment.END);
        toolbar.getStyle()
                .set("gap", "0.75rem")
                .set("flex-wrap", "wrap");

        ViewFrame frame = new ViewFrame(gridTitle, gridText, toolbar, userGrid);
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
                .set("grid-template-columns", "repeat(auto-fit, minmax(220px, 1fr))")
                .set("gap", "1rem");
    }

    private void configureSearchField() {
        searchField.setLabel("Suche");
        searchField.setPlaceholder("Name, E-Mail oder Rolle suchen");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidth("380px");
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        styleInputField(searchField);

        searchField.addValueChangeListener(event -> applyFilter());
    }

    private void refreshStats(List<AppUser> users, List<AppUser> filteredUsers) {
        statsContainer.removeAll();

        long totalUsers = users.size();
        long admins = users.stream()
                .filter(user -> user.getRole() == AppRole.ADMIN)
                .count();
        long normalUsers = users.stream()
                .filter(user -> user.getRole() == AppRole.USER)
                .count();
        long visibleUsers = filteredUsers.size();

        statsContainer.add(
                createStatCard("Benutzer", String.valueOf(totalUsers), "Alle registrierten Konten", VaadinIcon.USERS, "#2563eb"),
                createStatCard("Administratoren", String.valueOf(admins), "Konten mit Admin-Rechten", VaadinIcon.SHIELD, "#7c3aed"),
                createStatCard("Mitarbeiter", String.valueOf(normalUsers), "Normale Mitarbeiterkonten", VaadinIcon.USER, "#16a34a"),
                createStatCard("Treffer", String.valueOf(visibleUsers), "Aktuell sichtbare Benutzer", VaadinIcon.SEARCH, "#f59e0b")
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

    private void configureGrid() {
        userGrid.addColumn(AppUser::getId)
                .setHeader("ID")
                .setAutoWidth(true)
                .setFlexGrow(0);

        userGrid.addComponentColumn(this::createUserCell)
                .setHeader("Benutzer")
                .setAutoWidth(true)
                .setFlexGrow(1);

        userGrid.addComponentColumn(user -> createRoleBadge(user.getRole()))
                .setHeader("Rolle")
                .setAutoWidth(true)
                .setFlexGrow(0);

        userGrid.addComponentColumn(this::createAccountBadge)
                .setHeader("Konto")
                .setAutoWidth(true)
                .setFlexGrow(0);

        userGrid.addComponentColumn(this::createRoleEditor)
                .setHeader("Aktion")
                .setAutoWidth(true)
                .setFlexGrow(0);

        userGrid.setWidthFull();
        userGrid.setAllRowsVisible(true);

        userGrid.getStyle()
                .set("border-radius", "18px")
                .set("overflow", "hidden")
                .set("border", "1px solid #e2e8f0")
                .set("box-shadow", "0 10px 24px rgba(15, 23, 42, 0.04)")
                .set("--vaadin-grid-cell-padding", "0.7rem 0.85rem")
                .set("--vaadin-grid-header-background", "#f8fafc")
                .set("--vaadin-grid-header-cell-color", "#334155");
    }

    private Div createUserCell(AppUser user) {
        Div avatar = new Div();
        avatar.setText(getInitials(user));
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

        Div textWrapper = new Div();
        textWrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.18rem");

        Span name = new Span(user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : "Unbekannter Benutzer");
        name.getStyle()
                .set("font-weight", "850")
                .set("color", "#0f172a")
                .set("font-size", "0.95rem");

        Span email = new Span(user.getEmail() != null ? user.getEmail() : "Keine E-Mail");
        email.getStyle()
                .set("font-size", "0.84rem")
                .set("font-weight", "600")
                .set("color", "#64748b");

        textWrapper.add(name, email);

        Div wrapper = new Div(avatar, textWrapper);
        wrapper.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "0.75rem")
                .set("padding", "0.2rem 0");

        return wrapper;
    }

    private String getInitials(AppUser user) {
        String displayName = user.getDisplayName();

        if (displayName == null || displayName.isBlank()) {
            return "?";
        }

        String[] parts = displayName.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private Span createRoleBadge(AppRole role) {
        Span badge = new Span(getRoleDisplayName(role));
        badge.getStyle()
                .set("padding", "0.36rem 0.76rem")
                .set("border-radius", "999px")
                .set("font-size", "0.78rem")
                .set("font-weight", "850")
                .set("letter-spacing", "0.04em")
                .set("display", "inline-block");

        if (role == AppRole.ADMIN) {
            badge.getStyle()
                    .set("background", "#dbeafe")
                    .set("color", "#1d4ed8")
                    .set("border", "1px solid #bfdbfe");
        } else {
            badge.getStyle()
                    .set("background", "#dcfce7")
                    .set("color", "#166534")
                    .set("border", "1px solid #bbf7d0");
        }

        return badge;
    }

    private Span createAccountBadge(AppUser user) {
        AppUser currentUser = currentUserService.getOrCreateCurrentUser();
        boolean ownAccount = currentUser.getId().equals(user.getId());

        Span badge = new Span(ownAccount ? "Eigenes Konto" : "Bearbeitbar");
        badge.getStyle()
                .set("padding", "0.34rem 0.7rem")
                .set("border-radius", "999px")
                .set("font-size", "0.76rem")
                .set("font-weight", "850")
                .set("letter-spacing", "0.02em")
                .set("display", "inline-block");

        if (ownAccount) {
            badge.getStyle()
                    .set("background", "#f1f5f9")
                    .set("color", "#475569")
                    .set("border", "1px solid #cbd5e1");
        } else {
            badge.getStyle()
                    .set("background", "#f8fafc")
                    .set("color", "#334155")
                    .set("border", "1px solid #e2e8f0");
        }

        return badge;
    }

    private HorizontalLayout createRoleEditor(AppUser user) {
        AppUser currentUser = currentUserService.getOrCreateCurrentUser();

        ComboBox<AppRole> roleField = new ComboBox<>();
        roleField.setItems(AppRole.values());
        roleField.setItemLabelGenerator(this::getRoleDisplayName);
        roleField.setValue(user.getRole());
        roleField.setWidth("145px");
        styleInputField(roleField);

        Button saveButton = new Button("Speichern");
        stylePrimaryButton(saveButton);

        boolean ownAccount = currentUser.getId().equals(user.getId());

        if (ownAccount) {
            roleField.setEnabled(false);
            saveButton.setEnabled(false);
            saveButton.setText("Gesperrt");
            styleDisabledButton(saveButton);
        } else {
            saveButton.addClickListener(event -> {
                AppRole selectedRole = roleField.getValue();

                if (selectedRole == null) {
                    Notification.show("Bitte eine Rolle auswählen.");
                    return;
                }

                appUserService.updateUserRole(user.getId(), selectedRole)
                        .ifPresentOrElse(updatedUser -> {
                                    refreshGrid();
                                    Notification.show("Rolle wurde aktualisiert.");
                                },
                                () -> Notification.show("Rolle konnte nicht aktualisiert werden."));
            });
        }

        HorizontalLayout layout = new HorizontalLayout(roleField, saveButton);
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setAlignItems(Alignment.CENTER);
        layout.getStyle()
                .set("gap", "0.65rem")
                .set("flex-wrap", "wrap");

        return layout;
    }

    private void refreshGrid() {
        allUsers = appUserService.getAllUsers();
        applyFilter();
    }

    private void applyFilter() {
        String query = searchField.getValue();

        List<AppUser> filteredUsers = allUsers.stream()
                .filter(user -> matchesSearch(user, query))
                .toList();

        userGrid.setItems(filteredUsers);
        refreshStats(allUsers, filteredUsers);
    }

    private boolean matchesSearch(AppUser user, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase();

        String displayName = user.getDisplayName() != null ? user.getDisplayName().toLowerCase() : "";
        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
        String role = user.getRole() != null
                ? (user.getRole().name() + " " + getRoleDisplayName(user.getRole())).toLowerCase()
                : "";

        return displayName.contains(normalizedQuery)
                || email.contains(normalizedQuery)
                || role.contains(normalizedQuery);
    }

    private String getRoleDisplayName(AppRole role) {
        if (role == AppRole.ADMIN) {
            return "Administrator";
        }

        return "Mitarbeiter";
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

    private void styleDisabledButton(Button button) {
        button.getStyle()
                .set("background", "#e2e8f0")
                .set("color", "#475569")
                .set("box-shadow", "none")
                .set("border", "1px solid #cbd5e1")
                .set("cursor", "not-allowed");
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
}