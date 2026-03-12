<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true>
<!DOCTYPE html>
<html<#if realm.internationalizationEnabled> lang="${locale.currentLanguageTag}"</#if>>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <script>
    (function(){var t=localStorage.getItem('ef-theme');if(!t){t=window.matchMedia&&window.matchMedia('(prefers-color-scheme:dark)').matches?'dark':'light';}document.documentElement.setAttribute('data-theme',t);})();
    </script>
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
</head>
<body class="${bodyClass}">
    <div class="user-centered">
        <!-- Animated grid background -->
        <div class="grid-bg">
            <div class="grid-lines"></div>
            <div class="grid-beam-h" style="top: 25%; animation-duration: 9s; animation-delay: 0s;"></div>
            <div class="grid-beam-h" style="top: 65%; animation-duration: 11s; animation-delay: 4s;"></div>
            <div class="grid-beam-h gold" style="top: 40%; animation-duration: 13s; animation-delay: 7s;"></div>
            <div class="grid-beam-v" style="left: 30%; animation-duration: 10s; animation-delay: 2s;"></div>
            <div class="grid-beam-v" style="left: 70%; animation-duration: 8s; animation-delay: 5s;"></div>
            <div class="grid-beam-v teal" style="left: 50%; animation-duration: 14s; animation-delay: 9s;"></div>
            <div class="grid-node" style="top: 25%; left: 30%; animation-delay: 0.5s;"></div>
            <div class="grid-node" style="top: 65%; left: 70%; animation-delay: 2s;"></div>
            <div class="grid-node" style="top: 40%; left: 50%; animation-delay: 3.5s;"></div>
            <!-- Subtle cross -->
            <div class="grid-cross" style="top: 20%; left: 80%;">
                <div class="cross-v"></div>
                <div class="cross-h"></div>
                <div class="cross-center"></div>
            </div>
        </div>

        <!-- Decorative orbits -->
        <div class="usr-orbit usr-orbit-1"></div>
        <div class="usr-orbit usr-orbit-2"></div>
        <div class="usr-orbit-dot d1"></div>
        <div class="usr-orbit-dot d2"></div>

        <!-- Side decorative elements -->
        <div class="usr-side-left">
            <div class="usr-side-line" style="animation-delay: 0s;"></div>
            <div class="usr-side-dot"></div>
            <div class="usr-side-line" style="animation-delay: 1s;"></div>
            <div class="usr-side-dot"></div>
            <div class="usr-side-line" style="animation-delay: 2s;"></div>
        </div>
        <div class="usr-side-right">
            <div class="usr-side-line" style="animation-delay: 0.5s;"></div>
            <div class="usr-side-dot"></div>
            <div class="usr-side-line" style="animation-delay: 1.5s;"></div>
            <div class="usr-side-dot"></div>
            <div class="usr-side-line" style="animation-delay: 2.5s;"></div>
        </div>

        <#if realm.internationalizationEnabled && locale.supported?size gt 1>
            <div class="user-locale">
                <select onchange="window.location=this.value">
                    <#list locale.supported as l>
                        <option value="${l.url}"<#if l.label == locale.current> selected</#if>>${l.label}</option>
                    </#list>
                </select>
            </div>
        </#if>

        <div class="user-content">
            <!-- Back to home link -->
            <a href="${msg("landingUrl")}" class="usr-back-home">
                <svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                ${msg("backToHome")}
            </a>

            <div class="user-logo">Ecclesia<span class="flow">Flow</span></div>

            <#nested "welcome">

            <div class="user-card">
                <div class="fcard">
                    <#nested "header">

                    <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                        <div class="alert alert-${message.type}">
                            <#if message.type = 'success'>
                                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"/></svg>
                            <#elseif message.type = 'error'>
                                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"/></svg>
                            <#elseif message.type = 'warning'>
                                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"/></svg>
                            <#else>
                                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"/></svg>
                            </#if>
                            <span>${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <#nested "form">

                    <#nested "socialProviders">

                    <#if displayInfo>
                        <#nested "info">
                    </#if>
                </div>
            </div>

            <!-- Trust indicators -->
            <div class="usr-trust">
                <div class="usr-trust-item">
                    <svg fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"/></svg>
                    ${msg("trustEncrypted")}
                </div>
                <div class="usr-trust-item">
                    <svg fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path d="M9 12.75 11.25 15 15 9.75m-3-7.036A11.959 11.959 0 0 1 3.598 6 11.99 11.99 0 0 0 3 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285Z"/></svg>
                    ${msg("trustSecure")}
                </div>
                <div class="usr-trust-item">
                    <svg fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/></svg>
                    ${msg("trustAvailable")}
                </div>
            </div>

            <!-- Bottom tagline -->
            <div class="usr-bottom-tag">${msg("brandSlogan")}</div>
        </div>
    </div>

    <!-- Theme toggle button -->
    <button class="theme-toggle" onclick="toggleTheme()" aria-label="Toggle dark/light mode">
        <svg class="icon-sun" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z" clip-rule="evenodd"/></svg>
        <svg class="icon-moon" viewBox="0 0 20 20" fill="currentColor"><path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"/></svg>
    </button>

    <script>
    function toggleTheme(){var h=document.documentElement;var t=h.getAttribute('data-theme')==='dark'?'light':'dark';h.setAttribute('data-theme',t);localStorage.setItem('ef-theme',t);}
    /* Password visibility toggle */
    document.querySelectorAll('.pw-toggle').forEach(function(btn){btn.addEventListener('click',function(){var input=document.getElementById(this.getAttribute('data-target'));if(input.type==='password'){input.type='text';this.classList.add('is-visible');this.setAttribute('aria-label','Hide password');}else{input.type='password';this.classList.remove('is-visible');this.setAttribute('aria-label','Show password');}});});
    </script>
</body>
</html>
</#macro>
