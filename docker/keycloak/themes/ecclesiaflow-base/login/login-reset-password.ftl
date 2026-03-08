<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>

    <#if section = "header">
        <h2>${msg("forgotTitle")}</h2>
        <p class="sub">${msg("forgotSubtitle")}</p>

    <#elseif section = "form">
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
            <div class="field">
                <label for="username">${msg("emailLabel")}</label>
                <input id="username" name="username" type="text"
                       value="${(auth.attemptedUsername!'')}"
                       placeholder="${msg("emailPlaceholder")}"
                       autofocus autocomplete="username"
                       <#if messagesPerField.existsError('username')>class="error"</#if> />
            </div>
            <button class="btn btn-primary" type="submit">${msg("sendResetLink")}</button>
            <a href="${url.loginUrl}" class="btn btn-outline" style="text-align:center;">${msg("backToSignIn")}</a>
        </form>

    <#elseif section = "info">
    </#if>

</@layout.registrationLayout>
