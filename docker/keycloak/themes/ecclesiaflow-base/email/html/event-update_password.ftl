<#import "template.ftl" as layout>
<@layout.emailLayout>
<!-- Accent strip: teal -->
<tr><td style="height: 5px; background: linear-gradient(90deg, #0d9488, #14B8A6); font-size: 0; line-height: 0;">&nbsp;</td></tr>
<tr>
    <td style="padding: 28px 24px 24px;">
        <!-- Icon -->
        <table cellpadding="0" cellspacing="0" border="0"><tr>
            <td style="width: 56px; height: 56px; background: linear-gradient(135deg, rgba(20,184,166,0.12), rgba(20,184,166,0.06)); border: 1px solid rgba(20,184,166,0.15); border-radius: 16px; text-align: center; vertical-align: middle;">
                <span style="font-size: 24px;">&#128737;&#65039;</span>
            </td>
        </tr></table>

        <!-- Success badge -->
        <table cellpadding="0" cellspacing="0" border="0" style="margin: 20px 0;"><tr>
            <td style="background: linear-gradient(135deg, rgba(20,184,166,0.1), rgba(20,184,166,0.05)); border: 1px solid rgba(20,184,166,0.2); border-radius: 50px; padding: 8px 20px;">
                <span style="font-size: 14px; font-weight: 600; color: #0d9488;">&#10003; ${msg("eventUpdatePasswordBadge","Password updated successfully")}</span>
            </td>
        </tr></table>

        <h1 style="font-size: 22px; font-weight: 700; color: #0f172a; margin: 0 0 8px; line-height: 1.3; font-family: -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">${msg("eventUpdatePasswordTitle","Password updated")}</h1>
        <p style="font-size: 15px; color: #64748b; margin: 0 0 24px; line-height: 1.6;">${msg("eventUpdatePasswordSubtitle","Your EcclesiaFlow password was successfully changed.")}</p>

        <!-- Event details box: teal -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0" style="margin: 16px 0 24px;">
            <tr>
                <td style="padding: 18px 20px; background: linear-gradient(135deg, #f0fdfa, #f5fffe); border: 1px solid #99f6e4; border-left: 4px solid #14B8A6; border-radius: 12px;">
                    <table width="100%" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                            <td style="font-size: 14px; color: #64748b; padding: 4px 0;">${msg("eventDate","Date")}</td>
                            <td align="right" style="font-size: 14px; color: #0f172a; font-weight: 600; padding: 4px 0;">${event.date}</td>
                        </tr>
                        <tr>
                            <td style="font-size: 14px; color: #64748b; padding: 4px 0;">${msg("eventIpAddress","IP Address")}</td>
                            <td align="right" style="font-size: 14px; color: #0f172a; font-weight: 600; padding: 4px 0;">${event.ipAddress}</td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>

        <p style="font-size: 15px; color: #334155; line-height: 1.7; margin: 0 0 16px;">${msg("eventUpdatePasswordOk","If you made this change, no further action is needed.")}</p>

        <p style="font-size: 15px; color: #334155; line-height: 1.7; margin: 0 0 16px;">${msg("eventUpdatePasswordWarning","If you did <strong>not</strong> change your password, your account may be compromised. Please reset your password immediately and contact your church administrator.")}</p>

        <!-- Divider -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0" style="margin: 24px 0;"><tr><td style="height: 1px; background: linear-gradient(90deg, transparent, #e2e8f0, transparent); font-size: 0; line-height: 0;">&nbsp;</td></tr></table>

        <p style="font-size: 13px; color: #94a3b8; line-height: 1.6; margin: 0;">${msg("eventUpdatePasswordHint","This is a security notification. You will receive this email whenever your password is changed.")}</p>
    </td>
</tr>
</@layout.emailLayout>
