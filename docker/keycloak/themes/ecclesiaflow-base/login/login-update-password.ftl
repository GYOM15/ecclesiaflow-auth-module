<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>

    <#if section = "header">
        <#if isAppInitiatedAction??>
            <h2>${msg("resetTitle")}</h2>
            <p class="sub">${msg("resetSubtitle")}</p>
        <#else>
            <h2>${msg("setupTitle")}</h2>
            <p class="sub">${msg("setupSubtitle")}</p>
        </#if>

    <#elseif section = "form">
        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">
            <input type="text" id="username" name="username"
                   value="${username}" autocomplete="username"
                   readonly="readonly" style="display:none;" />

            <div class="field">
                <label for="password-new">${msg("newPasswordLabel")}</label>
                <div class="field-password">
                    <input id="password-new" name="password-new" type="password"
                           placeholder="${msg("newPasswordPlaceholder")}"
                           autofocus autocomplete="new-password"
                           <#if messagesPerField.existsError('password')>class="error"</#if> />
                    <button type="button" class="pw-toggle" aria-label="Afficher le mot de passe" data-target="password-new">
                        <svg class="icon-eye" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                        <svg class="icon-eye-off" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                            <line x1="1" y1="1" x2="23" y2="23"/>
                        </svg>
                    </button>
                </div>
                <div class="pw-strength">
                    <div class="pw-bar"></div><div class="pw-bar"></div>
                    <div class="pw-bar"></div><div class="pw-bar"></div>
                </div>
                <div class="field-hint">${msg("passwordHint")}</div>
            </div>
            <div class="field">
                <label for="password-confirm">${msg("confirmPasswordLabel")}</label>
                <div class="field-password">
                    <input id="password-confirm" name="password-confirm" type="password"
                           placeholder="${msg("confirmPasswordPlaceholder")}"
                           autocomplete="new-password"
                           <#if messagesPerField.existsError('password-confirm')>class="error"</#if> />
                    <button type="button" class="pw-toggle" aria-label="Afficher le mot de passe" data-target="password-confirm">
                        <svg class="icon-eye" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                        <svg class="icon-eye-off" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                            <line x1="1" y1="1" x2="23" y2="23"/>
                        </svg>
                    </button>
                </div>
            </div>

            <#if isAppInitiatedAction??>
                <button class="btn btn-primary" type="submit">${msg("updatePasswordBtn")}</button>
                <button class="btn btn-outline" type="submit" name="cancel-aia" value="true">${msg("doCancel")}</button>
            <#else>
                <button class="btn btn-primary" type="submit">${msg("activateAccountBtn")}</button>
            </#if>
        </form>

        <script>
            document.querySelectorAll('.pw-toggle').forEach(function(btn) {
                btn.addEventListener('click', function() {
                    var targetId = this.getAttribute('data-target');
                    var input = document.getElementById(targetId);
                    if (input.type === 'password') {
                        input.type = 'text';
                        this.classList.add('is-visible');
                        this.setAttribute('aria-label', 'Masquer le mot de passe');
                    } else {
                        input.type = 'password';
                        this.classList.remove('is-visible');
                        this.setAttribute('aria-label', 'Afficher le mot de passe');
                    }
                });
            });
        </script>
    </#if>

</@layout.registrationLayout>
