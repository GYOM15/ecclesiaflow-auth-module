<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>

    <#if section = "header">
        <h2>${kcSanitize(msg("errorTitle"))?no_esc}</h2>

    <#elseif section = "form">
        <#if skipLink??>
        <#else>
            <#if client?? && client.baseUrl?has_content>
                <a href="${client.baseUrl}" class="btn btn-primary">${kcSanitize(msg("backToApplication"))?no_esc}</a>
            </#if>
        </#if>
    </#if>

</@layout.registrationLayout>
