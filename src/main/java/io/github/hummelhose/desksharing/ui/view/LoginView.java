package io.github.hummelhose.desksharing.ui.view;

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
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Anmeldung")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        configureLayout();
        configureLoginForm();

        Div logo = createLogo();
        Span eyebrow = createEyebrow();

        H1 title = new H1("DeskSharing");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2.2rem")
                .set("font-weight", "900")
                .set("letter-spacing", "-0.055em")
                .set("line-height", "1.05")
                .set("color", "#0f172a");

        Paragraph subtitle = new Paragraph("Melde dich mit deinem Microsoft-Firmenkonto an und buche deinen Arbeitsplatz direkt im Sitzplan.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("max-width", "440px")
                .set("text-align", "center")
                .set("font-size", "0.98rem")
                .set("line-height", "1.55")
                .set("color", "#64748b");

        Div header = new Div(logo, eyebrow, title, subtitle);
        header.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("gap", "0.65rem")
                .set("text-align", "center");

        Div microsoftLoginBox = createMicrosoftLoginBox();
        Div localLoginBox = createLocalLoginBox();

        Div card = new Div(header, microsoftLoginBox, localLoginBox);
        card.getStyle()
                .set("width", "480px")
                .set("max-width", "92vw")
                .set("box-sizing", "border-box")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "1.25rem")
                .set("padding", "2rem")
                .set("background", "rgba(255, 255, 255, 0.96)")
                .set("border", "1px solid rgba(226, 232, 240, 0.92)")
                .set("border-radius", "28px")
                .set("box-shadow", "0 24px 70px rgba(15, 23, 42, 0.16)");

        add(card);
    }

    private void configureLayout() {
        setSizeFull();
        setPadding(true);
        setSpacing(false);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        getStyle()
                .set("min-height", "100vh")
                .set("background",
                        "radial-gradient(circle at top left, rgba(37, 99, 235, 0.18) 0, transparent 34%), " +
                                "linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%)")
                .set("font-family", "Inter, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif");
    }

    private void configureLoginForm() {
        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        LoginI18n i18n = LoginI18n.createDefault();

        LoginI18n.Form form = i18n.getForm();
        form.setTitle("Lokale Testanmeldung");
        form.setUsername("Benutzername");
        form.setPassword("Passwort");
        form.setSubmit("Lokal anmelden");
        i18n.setForm(form);

        LoginI18n.ErrorMessage errorMessage = i18n.getErrorMessage();
        errorMessage.setTitle("Anmeldung fehlgeschlagen");
        errorMessage.setMessage("Bitte prüfe deinen Benutzernamen und dein Passwort.");
        i18n.setErrorMessage(errorMessage);

        i18n.setAdditionalInformation(null);
        loginForm.setI18n(i18n);

        loginForm.getStyle()
                .set("width", "100%")
                .set("--vaadin-input-field-background", "#f8fafc")
                .set("--vaadin-input-field-border-width", "1px")
                .set("--vaadin-input-field-border-color", "#dbe2ea")
                .set("--vaadin-input-field-border-radius", "14px")
                .set("--vaadin-input-field-hover-border-color", "#bfdbfe")
                .set("--vaadin-input-field-focus-border-color", "#2563eb")
                .set("--vaadin-input-field-value-color", "#0f172a")
                .set("--vaadin-input-field-label-color", "#475569")
                .set("--lumo-primary-color", "#2563eb")
                .set("--lumo-primary-text-color", "#1d4ed8")
                .set("--lumo-border-radius-m", "14px");
    }

    private Div createMicrosoftLoginBox() {
        Span recommendedBadge = new Span("EMPFOHLEN");
        recommendedBadge.getStyle()
                .set("display", "inline-flex")
                .set("width", "fit-content")
                .set("padding", "0.28rem 0.6rem")
                .set("border-radius", "999px")
                .set("font-size", "0.68rem")
                .set("font-weight", "900")
                .set("letter-spacing", "0.08em")
                .set("color", "#1d4ed8")
                .set("background", "#dbeafe")
                .set("border", "1px solid #bfdbfe");

        H3 title = new H3("Mit Microsoft anmelden");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.18rem")
                .set("font-weight", "900")
                .set("letter-spacing", "-0.03em")
                .set("color", "#0f172a");

        Paragraph text = new Paragraph("Bitte nutze dein Firmenkonto. Das ist der normale Login für alle Mitarbeiter.");
        text.getStyle()
                .set("margin", "0")
                .set("font-size", "0.9rem")
                .set("line-height", "1.45")
                .set("color", "#475569");

        Icon microsoftIcon = VaadinIcon.USER_CHECK.create();
        microsoftIcon.getStyle()
                .set("width", "1.15rem")
                .set("height", "1.15rem");

        Button microsoftButton = new Button("Mit Microsoft anmelden", microsoftIcon);
        microsoftButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        microsoftButton.getStyle()
                .set("width", "100%")
                .set("height", "52px")
                .set("background", "linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)")
                .set("border", "none")
                .set("border-radius", "16px")
                .set("font-size", "1rem")
                .set("font-weight", "900")
                .set("box-shadow", "0 16px 34px rgba(37, 99, 235, 0.28)")
                .set("cursor", "pointer");

        microsoftButton.addClickListener(event ->
                UI.getCurrent().getPage().setLocation("/oauth2/authorization/microsoft")
        );

        Div box = new Div(recommendedBadge, title, text, microsoftButton);
        box.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.75rem")
                .set("padding", "1.2rem")
                .set("border-radius", "22px")
                .set("background", "linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%)")
                .set("border", "1px solid #bfdbfe")
                .set("box-shadow", "0 16px 34px rgba(37, 99, 235, 0.12)");

        return box;
    }

    private Div createLocalLoginBox() {
        Span dividerText = new Span("Lokale Testanmeldung");
        dividerText.getStyle()
                .set("font-size", "0.78rem")
                .set("font-weight", "850")
                .set("letter-spacing", "0.05em")
                .set("text-transform", "uppercase")
                .set("color", "#64748b");

        Paragraph hint = new Paragraph("Nur für Entwicklung und Tests. Mitarbeiter sollen oben Microsoft verwenden.");
        hint.getStyle()
                .set("margin", "0")
                .set("font-size", "0.82rem")
                .set("line-height", "1.4")
                .set("color", "#64748b");

        Div box = new Div(dividerText, hint, loginForm);
        box.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.75rem")
                .set("padding", "1rem")
                .set("border-radius", "20px")
                .set("background", "#f8fafc")
                .set("border", "1px dashed #cbd5e1");

        return box;
    }

    private Div createLogo() {
        Div logo = new Div();
        logo.setText("DS");
        logo.getStyle()
                .set("width", "58px")
                .set("height", "58px")
                .set("border-radius", "20px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "1.25rem")
                .set("font-weight", "900")
                .set("letter-spacing", "0.02em")
                .set("color", "#ffffff")
                .set("background", "linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)")
                .set("box-shadow", "0 18px 38px rgba(37, 99, 235, 0.32)");

        return logo;
    }

    private Span createEyebrow() {
        Span eyebrow = new Span("SITZPLATZ- UND BÜROVERWALTUNG");
        eyebrow.getStyle()
                .set("display", "inline-flex")
                .set("width", "fit-content")
                .set("padding", "0.35rem 0.7rem")
                .set("border-radius", "999px")
                .set("font-size", "0.68rem")
                .set("font-weight", "850")
                .set("letter-spacing", "0.08em")
                .set("color", "#1d4ed8")
                .set("background", "#dbeafe")
                .set("border", "1px solid #bfdbfe");

        return eyebrow;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            loginForm.setError(true);
        }
    }
}