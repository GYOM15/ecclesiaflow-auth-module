<#import "template.ftl" as layout>
<@layout.emailLayout>
<!-- Accent strip: red -->
<tr><td style="height: 5px; background: linear-gradient(90deg, #dc2626, #ef4444); font-size: 0; line-height: 0;">&nbsp;</td></tr>
<tr>
    <td style="padding: 28px 24px 24px;">
        <!-- Icon -->
        <table cellpadding="0" cellspacing="0" border="0"><tr>
            <td style="width: 56px; height: 56px; background: linear-gradient(135deg, rgba(239,68,68,0.12), rgba(239,68,68,0.06)); border: 1px solid rgba(239,68,68,0.15); border-radius: 16px; text-align: center; vertical-align: middle;">
                <span style="font-size: 24px;">&#9888;&#65039;</span>
            </td>
        </tr></table>

        <h1 style="font-size: 22px; font-weight: 700; color: #0f172a; margin: 20px 0 8px; line-height: 1.3; font-family: -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">${msg("eventLoginErrorTitle","Failed login attempt")}</h1>
        <p style="font-size: 15px; color: #64748b; margin: 0 0 24px; line-height: 1.6;">${msg("eventLoginErrorSubtitle","We detected an unsuccessful login attempt on your EcclesiaFlow account.")}</p>

        <!-- Event details box -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0" style="margin: 16px 0 24px;">
            <tr>
                <td style="padding: 18px 20px; background: linear-gradient(135deg, #fef2f2, #fff5f5); border: 1px solid #fecaca; border-left: 4px solid #ef4444; border-radius: 12px;">
                    <table width="100%" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                            <td style="font-size: 14px; color: #64748b; padding: 4px 0;">${msg("eventDate","Date")}</td>
                            <td align="right" style="font-size: 14px; color: #0f172a; font-weight: 600; padding: 4px 0;">${event.date?datetime?string("dd/MM/yyyy HH:mm")}</td>
                        </tr>
                        <tr>
                            <td style="font-size: 14px; color: #64748b; padding: 4px 0;">${msg("eventIpAddress","IP Address")}</td>
                            <td align="right" style="font-size: 14px; color: #0f172a; font-weight: 600; padding: 4px 0;">${event.ipAddress}</td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>

        <p style="font-size: 15px; color: #334155; line-height: 1.7; margin: 0 0 16px;">${msg("eventLoginErrorBody","If this was you, no action is needed. If you don't recognize this activity, we strongly recommend changing your password immediately.")}</p>

        <!-- CTA Button: red -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="padding: 8px 0 24px;">
            <a href="${link!realmUrl!"#"}" style="display: inline-block; padding: 14px 36px; background: linear-gradient(135deg, #dc2626, #ef4444); color: #ffffff; text-decoration: none; border-radius: 12px; font-weight: 600; font-size: 15px; letter-spacing: 0.2px; box-shadow: 0 4px 14px rgba(239,68,68,0.3);">${msg("eventLoginErrorBtn","Secure my account")}</a>
        </td></tr></table>

        <!-- Divider -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0" style="margin: 24px 0;"><tr><td style="height: 1px; background: linear-gradient(90deg, transparent, #e2e8f0, transparent); font-size: 0; line-height: 0;">&nbsp;</td></tr></table>

        <p style="font-size: 13px; color: #94a3b8; line-height: 1.6; margin: 0;">${msg("eventLoginErrorHint","This notification was sent because brute force protection is enabled on your account.")}</p>
    </td>
</tr>
</@layout.emailLayout>
