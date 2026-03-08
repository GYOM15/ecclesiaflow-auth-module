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
    <div class="split">
        <!-- Left branding panel -->
        <div class="panel-left">
            <div class="pl-grid"></div>
            <div class="pl-particles" id="pl-particles"></div>
            <div class="pl-pulse"></div>
            <div class="pl-pulse"></div>
            <div class="pl-pulse"></div>

            <#if realm.internationalizationEnabled && locale.supported?size gt 1>
                <div class="locale">
                    <select onchange="window.location=this.value">
                        <#list locale.supported as l>
                            <option value="${l.url}"<#if l.label == locale.current> selected</#if>>${l.label}</option>
                        </#list>
                    </select>
                </div>
            </#if>

            <div class="pl-content">
                <div class="pl-logo">Ecclesia<span class="flow">Flow</span></div>
                <div class="pl-tagline">${msg("brandTagline")}</div>
                <#nested "brand">
            </div>
        </div>

        <!-- Right form panel -->
        <div class="panel-right">
            <div class="form-container">
                <div class="mobile-logo">
                    <div class="logo-text">Ecclesia<span class="flow">Flow</span></div>
                </div>

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
    </div>

    <!-- Theme toggle button -->
    <button class="theme-toggle" onclick="toggleTheme()" aria-label="Toggle dark/light mode">
        <svg class="icon-sun" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z" clip-rule="evenodd"/></svg>
        <svg class="icon-moon" viewBox="0 0 20 20" fill="currentColor"><path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"/></svg>
    </button>

    <script>
    function toggleTheme(){var h=document.documentElement;var t=h.getAttribute('data-theme')==='dark'?'light':'dark';h.setAttribute('data-theme',t);localStorage.setItem('ef-theme',t);}
    /* Password visibility toggle */
    document.querySelectorAll('.pw-toggle').forEach(function(btn){btn.addEventListener('click',function(){var input=document.getElementById(this.getAttribute('data-target'));if(input.type==='password'){input.type='text';this.classList.add('is-visible');this.setAttribute('aria-label','Masquer le mot de passe');}else{input.type='password';this.classList.remove('is-visible');this.setAttribute('aria-label','Afficher le mot de passe');}});});
    (function() {
        var c = document.getElementById('pl-particles');
        if (!c) return;
        for (var i = 0; i < 15; i++) {
            var p = document.createElement('div');
            p.className = 'pl-particle';
            p.style.left = Math.random() * 100 + '%';
            p.style.animationDuration = (12 + Math.random() * 18) + 's';
            p.style.animationDelay = (Math.random() * 10) + 's';
            var s = (1 + Math.random() * 2) + 'px';
            p.style.width = s;
            p.style.height = s;
            c.appendChild(p);
        }
    })();
    </script>
</body>
</html>
</#macro>
