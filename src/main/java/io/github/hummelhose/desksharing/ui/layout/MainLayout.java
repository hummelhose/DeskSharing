package io.github.hummelhose.desksharing.ui.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import io.github.hummelhose.desksharing.infrastructure.security.CurrentUserService;
import io.github.hummelhose.desksharing.ui.view.AdminFloorplanEditorView;
import io.github.hummelhose.desksharing.ui.view.DashboardView;
import io.github.hummelhose.desksharing.ui.view.FloorplanView;
import io.github.hummelhose.desksharing.ui.view.UserManagementView;
import io.github.hummelhose.desksharing.ui.view.AdminReservationView;
import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout {

    private final AuthenticationContext authenticationContext;
    private final CurrentUserService currentUserService;

    public MainLayout(AuthenticationContext authenticationContext,
                      CurrentUserService currentUserService) {
        this.authenticationContext = authenticationContext;
        this.currentUserService = currentUserService;

        setPrimarySection(Section.DRAWER);

        getElement().getStyle()
                .set("--vaadin-app-layout-drawer-width", "280px")
                .set("background", "#0f172a")
                .set("font-family", "Inter, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif");

        createHeader();
        createDrawer();
    }

    private void createHeader() {
        AppUser currentUser = currentUserService.getOrCreateCurrentUser();

        DrawerToggle drawerToggle = new DrawerToggle();
        drawerToggle.getStyle()
                .set("color", "#e2e8f0")
                .set("background", "rgba(15, 23, 42, 0.72)")
                .set("border", "1px solid rgba(148, 163, 184, 0.22)")
                .set("border-radius", "14px")
                .set("padding", "0.45rem")
                .set("box-shadow", "0 10px 28px rgba(15, 23, 42, 0.22)");

        H1 title = new H1("DeskSharing");
        title.getStyle()
                .set("font-size", "1.35rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.035em")
                .set("margin", "0")
                .set("color", "#f8fafc");

        Span subtitle = new Span("Sitzplatz- und Büroverwaltung");
        subtitle.getStyle()
                .set("font-size", "0.82rem")
                .set("color", "#94a3b8")
                .set("margin-top", "-0.1rem");

        Div titleWrapper = new Div(title, subtitle);
        titleWrapper.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.12rem");

        Div leftSide = new Div(drawerToggle, titleWrapper);
        leftSide.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "1rem");

        String username = currentUser.getDisplayName() != null && !currentUser.getDisplayName().isBlank()
                ? currentUser.getDisplayName()
                : "Benutzer";

        String email = currentUser.getEmail() != null && !currentUser.getEmail().isBlank()
                ? currentUser.getEmail()
                : "";

        Avatar avatar = new Avatar(getInitial(username));
        avatar.getStyle()
                .set("background", "linear-gradient(135deg, #dbeafe 0%, #93c5fd 100%)")
                .set("color", "#172554")
                .set("font-weight", "850")
                .set("box-shadow", "0 12px 28px rgba(59, 130, 246, 0.22)");

        Span userName = new Span(username);
        userName.getStyle()
                .set("font-size", "0.9rem")
                .set("font-weight", "850")
                .set("color", "#f8fafc")
                .set("line-height", "1.1");

        Span userMail = new Span(email);
        userMail.getStyle()
                .set("font-size", "0.76rem")
                .set("font-weight", "600")
                .set("color", "#94a3b8")
                .set("line-height", "1.1");

        Div userText = new Div(userName, userMail);
        userText.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.16rem");

        Div userBox = new Div(avatar, userText);
        userBox.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "0.72rem")
                .set("padding", "0.45rem 0.6rem")
                .set("border-radius", "16px")
                .set("background", "rgba(255, 255, 255, 0.06)")
                .set("border", "1px solid rgba(148, 163, 184, 0.16)");

        Span roleBadge = new Span(currentUser.getRole() == AppRole.ADMIN ? "Administrator" : "Mitarbeiter");
        roleBadge.getStyle()
                .set("padding", "0.42rem 0.78rem")
                .set("border-radius", "999px")
                .set("font-size", "0.74rem")
                .set("letter-spacing", "0.04em")
                .set("font-weight", "850")
                .set("background", currentUser.getRole() == AppRole.ADMIN
                        ? "rgba(37, 99, 235, 0.26)"
                        : "rgba(14, 165, 233, 0.22)")
                .set("color", currentUser.getRole() == AppRole.ADMIN ? "#bfdbfe" : "#bae6fd")
                .set("border", "1px solid rgba(147, 197, 253, 0.22)");

        Button logoutButton = new Button("Abmelden", new Icon(VaadinIcon.SIGN_OUT), event -> authenticationContext.logout());
        logoutButton.getStyle()
                .set("background", "rgba(255, 255, 255, 0.96)")
                .set("border", "1px solid rgba(248, 113, 113, 0.24)")
                .set("color", "#b91c1c")
                .set("border-radius", "14px")
                .set("font-weight", "850")
                .set("padding", "0.65rem 0.95rem")
                .set("box-shadow", "0 14px 30px rgba(15, 23, 42, 0.18)");

        Div rightSide = new Div(roleBadge, userBox, logoutButton);
        rightSide.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "0.85rem")
                .set("flex-wrap", "wrap")
                .set("justify-content", "flex-end");

        Div header = new Div(leftSide, rightSide);
        header.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("padding", "1rem 1.35rem")
                .set("gap", "1rem")
                .set("background", "linear-gradient(135deg, #0f172a 0%, #172554 100%)")
                .set("border-bottom", "1px solid rgba(148, 163, 184, 0.18)")
                .set("box-shadow", "0 16px 38px rgba(15, 23, 42, 0.24)");

        addToNavbar(header);
    }

    private void createDrawer() {
        AppUser currentUser = currentUserService.getOrCreateCurrentUser();

        Div brandBadge = new Div();
        brandBadge.setText("DS");
        brandBadge.getStyle()
                .set("width", "46px")
                .set("height", "46px")
                .set("border-radius", "16px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-weight", "900")
                .set("font-size", "1.05rem")
                .set("letter-spacing", "0.02em")
                .set("color", "#ffffff")
                .set("background", "linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)")
                .set("box-shadow", "0 16px 36px rgba(37, 99, 235, 0.34)");

        H1 navTitle = new H1("DeskSharing");
        navTitle.getStyle()
                .set("font-size", "1.25rem")
                .set("font-weight", "850")
                .set("letter-spacing", "-0.035em")
                .set("margin", "0")
                .set("color", "#f8fafc");

        Span navSubtitle = new Span(currentUser.getRole() == AppRole.ADMIN
                ? "Admin-Bereich"
                : "Mitarbeiter-Bereich");
        navSubtitle.getStyle()
                .set("font-size", "0.78rem")
                .set("font-weight", "650")
                .set("color", "#94a3b8");

        Div brandText = new Div(navTitle, navSubtitle);
        brandText.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.12rem");

        Div brandHeader = new Div(brandBadge, brandText);
        brandHeader.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "0.95rem")
                .set("padding", "1.35rem 1.1rem 1.1rem 1.1rem");

        Div nav = new Div();
        nav.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.35rem")
                .set("padding", "0.35rem 0.75rem 1rem 0.75rem");

        nav.add(createNavSectionLabel("Allgemein"));
        nav.add(createNavLink("Startseite", DashboardView.class, VaadinIcon.DASHBOARD));
        nav.add(createNavLink("Sitzplatz buchen", FloorplanView.class, VaadinIcon.GRID_SMALL));

        if (currentUser.getRole() == AppRole.ADMIN) {
            nav.add(createNavSectionLabel("Administration"));
            nav.add(createNavLink("Plan-Editor", AdminFloorplanEditorView.class, VaadinIcon.EDIT));
            nav.add(createNavLink("Buchungen verwalten", AdminReservationView.class, VaadinIcon.CALENDAR));
            nav.add(createNavLink("Benutzerverwaltung", UserManagementView.class, VaadinIcon.USERS));
        }

        Div navWrapper = new Div(nav);
        navWrapper.getStyle()
                .set("padding", "0 0.35rem");

        Div footerHint = new Div();
        footerHint.setText(currentUser.getRole() == AppRole.ADMIN
                ? "Verwalte Büros, Räume, Tische und Benutzer zentral an einer Stelle."
                : "Wähle ein Büro aus, klicke auf einen freien Tisch und buche deinen Arbeitsplatz.");
        footerHint.getStyle()
                .set("margin", "auto 1rem 1rem 1rem")
                .set("padding", "1rem")
                .set("border-radius", "18px")
                .set("font-size", "0.8rem")
                .set("line-height", "1.45")
                .set("color", "#cbd5e1")
                .set("background", "rgba(15, 23, 42, 0.62)")
                .set("border", "1px solid rgba(148, 163, 184, 0.16)")
                .set("box-shadow", "inset 0 1px 0 rgba(255,255,255,0.04)");

        Div drawerContent = new Div(brandHeader, navWrapper, footerHint);
        drawerContent.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("height", "100%")
                .set("background", "linear-gradient(180deg, #0f172a 0%, #111827 58%, #020617 100%)")
                .set("border-right", "1px solid rgba(148, 163, 184, 0.16)")
                .set("box-shadow", "18px 0 40px rgba(15, 23, 42, 0.25)");

        Scroller scroller = new Scroller(drawerContent);
        scroller.setSizeFull();

        addToDrawer(scroller);
    }

    private Span createNavSectionLabel(String text) {
        Span label = new Span(text);
        label.getStyle()
                .set("display", "inline-block")
                .set("padding", "0.75rem 0.9rem 0.25rem")
                .set("font-size", "0.68rem")
                .set("font-weight", "850")
                .set("letter-spacing", "0.08em")
                .set("text-transform", "uppercase")
                .set("color", "#64748b");

        return label;
    }

    private RouterLink createNavLink(String label,
                                     Class<? extends Component> navigationTarget,
                                     VaadinIcon icon) {
        Icon itemIcon = icon.create();
        itemIcon.getStyle()
                .set("color", "#93c5fd")
                .set("width", "1.15rem")
                .set("height", "1.15rem")
                .set("flex-shrink", "0");

        Span text = new Span(label);
        text.getStyle()
                .set("color", "#e2e8f0")
                .set("font-size", "0.95rem")
                .set("font-weight", "750");

        RouterLink link = new RouterLink();
        link.setRoute(navigationTarget);
        link.add(itemIcon, text);

        link.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "0.85rem")
                .set("padding", "0.82rem 0.9rem")
                .set("border-radius", "15px")
                .set("text-decoration", "none")
                .set("background", "transparent")
                .set("border", "1px solid transparent")
                .set("transition", "background 160ms ease, border 160ms ease, transform 160ms ease");

        link.getElement().executeJs("""
                const link = this;

                link.addEventListener('mouseenter', () => {
                    link.style.background = 'rgba(37, 99, 235, 0.18)';
                    link.style.border = '1px solid rgba(147, 197, 253, 0.20)';
                    link.style.transform = 'translateX(2px)';
                });

                link.addEventListener('mouseleave', () => {
                    link.style.background = 'transparent';
                    link.style.border = '1px solid transparent';
                    link.style.transform = 'translateX(0)';
                });
                """);

        return link;
    }

    private String getInitial(String username) {
        if (username == null || username.isBlank()) {
            return "U";
        }

        return username.substring(0, 1).toUpperCase();
    }
}