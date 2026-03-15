<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="page-voiles" displayMessage=!messagesPerField.existsError('username','password') displayInfo=true; section>

    <#if section = "welcome">
        <div class="user-welcome">${msg("userLoginWelcome")}</div>

    <#elseif section = "header">
        <h2>${msg("userLoginTitle")}</h2>
        <p class="sub">${msg("userLoginSubtitle")}</p>

    <#elseif section = "form">
        <#if social.providers??>
            <div class="social-row">
                <#list social.providers as p>
                    <a href="${p.loginUrl}" class="btn-social" id="social-${p.alias}">
                        <#if p.alias == "google">
                            <svg viewBox="0 0 18 18"><path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 01-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z" fill="#4285F4"/><path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 009 18z" fill="#34A853"/><path d="M3.964 10.71A5.41 5.41 0 013.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.997 8.997 0 000 9c0 1.452.348 2.827.957 4.042l3.007-2.332z" fill="#FBBC05"/><path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 00.957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z" fill="#EA4335"/></svg>
                        <#elseif p.alias == "facebook">
                            <svg viewBox="0 0 18 18" fill="#1877F2"><path d="M18 9a9 9 0 10-10.406 8.89v-6.29H5.309V9h2.285V7.017c0-2.255 1.343-3.501 3.4-3.501.984 0 2.014.176 2.014.176v2.215h-1.135c-1.118 0-1.467.694-1.467 1.406V9h2.496l-.399 2.6h-2.097v6.29A9.003 9.003 0 0018 9z"/></svg>
                        <#else>
                            <span class="social-icon">${p.displayName[0]}</span>
                        </#if>
                        ${p.displayName}
                    </a>
                </#list>
            </div>
            <div class="divider">${msg("orDivider")}</div>
        </#if>

        <#if realm.password>
            <form id="kc-form-login" action="${url.loginAction}" method="post">
                <div class="field">
                    <label for="username">${msg("emailLabel")}</label>
                    <input id="username" name="username" type="text"
                           value="${(login.username!'')}"
                           placeholder="${msg("emailPlaceholder")}"
                           autofocus autocomplete="username"
                           <#if messagesPerField.existsError('username','password')>class="error"</#if> />
                </div>
                <div class="field">
                    <label for="password">${msg("passwordLabel")}</label>
                    <div class="field-password">
                        <input id="password" name="password" type="password"
                               placeholder="${msg("passwordPlaceholder")}"
                               autocomplete="current-password"
                               <#if messagesPerField.existsError('username','password')>class="error"</#if> />
                        <button type="button" class="pw-toggle" aria-label="Afficher le mot de passe" data-target="password">
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
                <div class="form-row">
                    <#if realm.rememberMe && !usernameEditDisabled??>
                        <label>
                            <input type="checkbox" id="rememberMe" name="rememberMe"
                                   <#if login.rememberMe??>checked</#if>>
                            ${msg("rememberMe")}
                        </label>
                    <#else>
                        <span></span>
                    </#if>
                    <#if realm.resetPasswordAllowed>
                        <a href="${url.loginResetCredentialsUrl}">${msg("forgotPassword")}</a>
                    </#if>
                </div>
                <input type="hidden" id="id-hidden-input" name="credentialId"
                       <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if> />
                <button class="btn btn-primary" type="submit">${msg("signIn")}</button>
            </form>
        </#if>

    <#elseif section = "info">
        <div class="fcard-footer">
            ${msg("noAccount")} <a href="${msg("landingUrl")}/inscription">${msg("signUp")}</a>
        </div>
    </#if>

</@layout.registrationLayout>
