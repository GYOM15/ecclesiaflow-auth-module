<#import "template.ftl" as layout>
<@layout.emailLayout>
<!-- Accent strip: indigo -->
<tr><td style="height: 5px; background: linear-gradient(90deg, #6366F1, #818CF8); font-size: 0; line-height: 0;">&nbsp;</td></tr>
<tr>
    <td style="padding: 28px 24px 24px;">
        <!-- Icon -->
        <table cellpadding="0" cellspacing="0" border="0"><tr>
            <td style="width: 56px; height: 56px; background: linear-gradient(135deg, rgba(99,102,241,0.12), rgba(99,102,241,0.06)); border: 1px solid rgba(99,102,241,0.15); border-radius: 16px; text-align: center; vertical-align: middle;">
                <span style="font-size: 24px;">&#128274;</span>
            </td>
        </tr></table>

        <h1 style="font-size: 22px; font-weight: 700; color: #0f172a; margin: 20px 0 8px; line-height: 1.3; font-family: -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">${msg("emailVerificationCodeTitle","Your verification code")}</h1>
        <p style="font-size: 15px; color: #64748b; margin: 0 0 24px; line-height: 1.6;">${msg("emailVerificationCodeSubtitle","Enter the code below in EcclesiaFlow to verify your email address.")}</p>

        <!-- Code display -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="padding: 8px 0 24px;">
            <span style="display: inline-block; font-family: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace; font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #6366F1; background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(99,102,241,0.03)); padding: 18px 32px; border-radius: 14px; border: 2px dashed rgba(99,102,241,0.25);">${code}</span>
        </td></tr></table>

        <p style="font-size: 15px; color: #334155; line-height: 1.7; margin: 0 0 16px;">${msg("emailVerificationCodeExpiry","This code will expire in")} <strong>${linkExpiration}</strong>.</p>

        <!-- Divider -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0" style="margin: 24px 0;"><tr><td style="height: 1px; background: linear-gradient(90deg, transparent, #e2e8f0, transparent); font-size: 0; line-height: 0;">&nbsp;</td></tr></table>

        <p style="font-size: 13px; color: #94a3b8; line-height: 1.6; margin: 0;">${msg("emailVerificationCodeHint","If you didn't request this code, you can safely ignore this email. Someone may have entered your email by mistake.")}</p>
    </td>
</tr>
</@layout.emailLayout>
