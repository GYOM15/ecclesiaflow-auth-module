<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm'); section>

    <#if section = "brand">
        <div class="brand-features">
            <div class="brand-feature">
                <div class="brand-feature-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                        <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                        <circle cx="9" cy="7" r="4"/>
                        <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                        <path d="M16 3.13a4 4 0 010 7.75"/>
                    </svg>
                </div>
                <div class="brand-feature-text">
                    <h4>${msg("featureMemberTitle")}</h4>
                    <p>${msg("featureMemberDesc")}</p>
                </div>
            </div>
            <div class="brand-feature">
                <div class="brand-feature-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                        <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                        <line x1="16" y1="2" x2="16" y2="6"/>
                        <line x1="8" y1="2" x2="8" y2="6"/>
                        <line x1="3" y1="10" x2="21" y2="10"/>
                    </svg>
                </div>
                <div class="brand-feature-text">
                    <h4>${msg("featureEventTitle")}</h4>
                    <p>${msg("featureEventDesc")}</p>
                </div>
            </div>
            <div class="brand-feature">
                <div class="brand-feature-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                    </svg>
                </div>
                <div class="brand-feature-text">
                    <h4>${msg("featureSecurityTitle")}</h4>
                    <p>${msg("featureSecurityDesc")}</p>
                </div>
            </div>
        </div>

    <#elseif section = "header">
        <h2>${msg("adminRegisterTitle")}</h2>
        <p class="subtitle">${msg("adminRegisterSubtitle")}</p>

    <#elseif section = "form">
        <form id="kc-register-form" action="${url.registrationAction}" method="post">
            <div class="field">
                <label for="user.attributes.churchName">${msg("churchNameLabel")}</label>
                <input id="user.attributes.churchName" name="user.attributes.churchName" type="text"
                       value="${(register.formData['user.attributes.churchName']!'')}"
                       placeholder="${msg("churchNameLabel")}"
                       <#if messagesPerField.existsError('user.attributes.churchName')>class="error"</#if> />
            </div>
            <div class="field-row">
                <div class="field">
                    <label for="firstName">${msg("firstNameLabel")}</label>
                    <input id="firstName" name="firstName" type="text"
                           value="${(register.formData.firstName!'')}"
                           placeholder="${msg("firstNameLabel")}"
                           <#if messagesPerField.existsError('firstName')>class="error"</#if> />
                </div>
                <div class="field">
                    <label for="lastName">${msg("lastNameLabel")}</label>
                    <input id="lastName" name="lastName" type="text"
                           value="${(register.formData.lastName!'')}"
                           placeholder="${msg("lastNameLabel")}"
                           <#if messagesPerField.existsError('lastName')>class="error"</#if> />
                </div>
            </div>
            <div class="field">
                <label for="email">${msg("emailLabel")}</label>
                <input id="email" name="email" type="email"
                       value="${(register.formData.email!'')}"
                       placeholder="${msg("emailPlaceholder")}"
                       autocomplete="email"
                       <#if messagesPerField.existsError('email')>class="error"</#if> />
            </div>
            <#if passwordRequired??>
                <div class="field">
                    <label for="password">${msg("passwordLabel")}</label>
                    <input id="password" name="password" type="password"
                           placeholder="${msg("newPasswordPlaceholder")}"
                           autocomplete="new-password"
                           <#if messagesPerField.existsError('password','password-confirm')>class="error"</#if> />
                    <div class="pw-strength">
                        <div class="pw-bar"></div><div class="pw-bar"></div>
                        <div class="pw-bar"></div><div class="pw-bar"></div>
                    </div>
                    <div class="field-hint">${msg("passwordHint")}</div>
                </div>
                <div class="field">
                    <label for="password-confirm">${msg("confirmPasswordLabel")}</label>
                    <input id="password-confirm" name="password-confirm" type="password"
                           placeholder="${msg("confirmPasswordPlaceholder")}"
                           autocomplete="new-password"
                           <#if messagesPerField.existsError('password-confirm')>class="error"</#if> />
                </div>
            </#if>
            <#if recaptchaRequired??>
                <div class="field">
                    <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
                </div>
            </#if>
            <button class="btn btn-primary" type="submit">${msg("doRegister")}</button>
        </form>
        <div class="fcard-footer">
            ${msg("hasAccount")} <a href="${url.loginUrl}">${msg("signIn")}</a>
        </div>
    </#if>

</@layout.registrationLayout>
