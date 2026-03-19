<#import "template.ftl" as layout>
<@layout.emailLayout>
<!-- Accent strip: indigo -->
<tr><td style="height: 5px; background: linear-gradient(90deg, #6366F1, #818CF8); font-size: 0; line-height: 0;">&nbsp;</td></tr>
<tr>
    <td style="padding: 28px 24px 24px;">
        <!-- Icon -->
        <table cellpadding="0" cellspacing="0" border="0"><tr>
            <td style="width: 56px; height: 56px; background: linear-gradient(135deg, rgba(99,102,241,0.12), rgba(99,102,241,0.06)); border: 1px solid rgba(99,102,241,0.15); border-radius: 16px; text-align: center; vertical-align: middle;">
                <span style="font-size: 20px; font-weight: 700; color: #6366F1;">&#10003;</span>
            </td>
        </tr></table>

        <!-- Title -->
        <h1 style="font-size: 22px; font-weight: 700; color: #0f172a; margin: 20px 0 8px; line-height: 1.3; font-family: -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">${msg("emailVerificationTitle","Verify your email")}</h1>
        <p style="font-size: 15px; color: #64748b; margin: 0 0 24px; line-height: 1.6;">${msg("emailVerificationGreeting","Welcome to EcclesiaFlow! Please confirm your email address to activate your account.")}</p>

        <!-- Body -->
        <p style="font-size: 15px; color: #334155; line-height: 1.7; margin: 0 0 16px;">${msg("emailVerificationInstruction","Click the button below to verify your email address. This link will expire in")} <strong>${linkExpiration}</strong>.</p>

        <!-- CTA Button -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td align="center" style="padding: 8px 0 24px;">
            <a href="${link}" style="display: inline-block; padding: 14px 36px; background: linear-gradient(135deg, #6366F1 0%, #818CF8 100%); color: #ffffff; text-decoration: none; border-radius: 12px; font-weight: 600; font-size: 15px; letter-spacing: 0.2px; box-shadow: 0 4px 14px rgba(99,102,241,0.3);">${msg("emailVerificationBtn","Verify my email")}</a>
        </td></tr></table>

        <!-- Link fallback -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0"><tr>
            <td style="font-size: 12px; color: #94a3b8; word-break: break-all; line-height: 1.5; padding: 14px 16px; background: #f8fafc; border-radius: 10px; border: 1px dashed #e2e8f0;">
                ${msg("emailLinkFallback","If the button doesn't work, copy and paste this link into your browser:")}<br/>
                <a href="${link}" style="color: #6366F1; text-decoration: none;">${link}</a>
            </td>
        </tr></table>

        <!-- Divider -->
        <table width="100%" cellpadding="0" cellspacing="0" border="0" style="margin: 24px 0;"><tr><td style="height: 1px; background: linear-gradient(90deg, transparent, #e2e8f0, transparent); font-size: 0; line-height: 0;">&nbsp;</td></tr></table>

        <!-- Hint -->
        <p style="font-size: 13px; color: #94a3b8; line-height: 1.6; margin: 0;">${msg("emailVerificationHint","If you didn't create an EcclesiaFlow account, you can safely ignore this email.")}</p>
    </td>
</tr>
</@layout.emailLayout>
