${msg("executeActionsTitle","Action required")}

${msg("executeActionsSubtitle","Your EcclesiaFlow administrator has requested that you complete the following actions:")}

<#if requiredActions??>
<#list requiredActions as reqAction>
- ${msg("requiredAction.${reqAction}",reqAction)}
</#list>
</#if>

${msg("executeActionsInstruction","Click the link below to proceed. This link will expire in")} ${linkExpiration}.

${link}

${msg("executeActionsHint","If you didn't expect this email, please contact your church administrator.")}

---
© ${.now?string('yyyy')} EcclesiaFlow. ${msg("allRightsReserved","All rights reserved.")}
