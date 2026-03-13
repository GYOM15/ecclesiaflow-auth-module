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

        <div id="pw-mismatch-msg" class="alert alert-error" style="display:none;">
            <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"/></svg>
            <span>${msg("passwordMismatch")}</span>
        </div>

        <script>
            /* Password strength meter */
            (function() {
                var pwInput = document.getElementById('password-new');
                var bars = document.querySelectorAll('.pw-bar');

                if (pwInput && bars.length) {
                    pwInput.addEventListener('input', function() {
                        var val = this.value;
                        var score = 0;
                        if (val.length >= 8) score++;
                        if (/[a-z]/.test(val) && /[A-Z]/.test(val)) score++;
                        if (/\d/.test(val)) score++;
                        if (/[^a-zA-Z0-9]/.test(val)) score++;

                        bars.forEach(function(bar, i) {
                            bar.className = 'pw-bar';
                            if (i < score) {
                                if (score <= 1) bar.classList.add('weak');
                                else if (score <= 2) bar.classList.add('medium');
                                else bar.classList.add('strong');
                            }
                        });
                    });
                }

                /* Client-side confirmation mismatch check */
                var confirmInput = document.getElementById('password-confirm');
                var mismatchMsg = document.getElementById('pw-mismatch-msg');
                var form = document.getElementById('kc-passwd-update-form');

                if (confirmInput && mismatchMsg && form) {
                    confirmInput.addEventListener('input', function() {
                        if (this.value && pwInput.value && this.value !== pwInput.value) {
                            mismatchMsg.style.display = 'flex';
                            confirmInput.classList.add('error');
                        } else {
                            mismatchMsg.style.display = 'none';
                            confirmInput.classList.remove('error');
                        }
                    });

                    form.addEventListener('submit', function(e) {
                        if (pwInput.value !== confirmInput.value) {
                            e.preventDefault();
                            mismatchMsg.style.display = 'flex';
                            confirmInput.classList.add('error');
                            confirmInput.focus();
                        }
                    });
                }
            })();
        </script>
    </#if>

</@layout.registrationLayout>
