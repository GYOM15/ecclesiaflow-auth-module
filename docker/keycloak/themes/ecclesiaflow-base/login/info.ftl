<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true displayInfo=true; section>

    <#if section = "header">
        <#if messageHeader??>
            <h2>${kcSanitize(messageHeader)?no_esc}</h2>
        <#else>
            <h2>${message.summary}</h2>
        </#if>

    <#elseif section = "form">
        <#if requiredActions??>
            <div class="field-hint" style="margin-bottom:16px;">
                <#list requiredActions>
                    <#items as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#items>
                </#list>
            </div>
        </#if>

    <#elseif section = "info">
        <#if skipLink??>
        <#else>
            <#if pageRedirectUri?has_content>
                <a href="${pageRedirectUri}" class="btn btn-primary">${kcSanitize(msg("backToApplication"))?no_esc}</a>
            <#elseif actionUri?has_content>
                <a href="${actionUri}" class="btn btn-primary">${kcSanitize(msg("proceedWithAction"))?no_esc}</a>
            <#elseif (client.baseUrl)?has_content>
                <a href="${client.baseUrl}" class="btn btn-primary">${kcSanitize(msg("backToApplication"))?no_esc}</a>
            </#if>
        </#if>
    </#if>

</@layout.registrationLayout>
