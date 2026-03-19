<#macro emailLayout>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>${msg("emailTitle","EcclesiaFlow")}</title>
</head>
<body style="margin: 0; padding: 0; background-color: #F8FAFC; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; -webkit-font-smoothing: antialiased;">
    <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color: #F8FAFC;">
        <tr>
            <td align="center" style="padding: 40px 16px;">
                <table width="560" cellpadding="0" cellspacing="0" border="0" style="max-width: 560px; width: 100%;">

                    <!-- GRADIENT HEADER -->
                    <tr>
                        <td style="height: 6px; background: linear-gradient(90deg, #6366F1, #818CF8, #14B8A6, #F59E0B); border-radius: 16px 16px 0 0; font-size: 0; line-height: 0;">&nbsp;</td>
                    </tr>
                    <tr>
                        <td style="background: #ffffff; padding: 28px 24px; border-left: 1px solid #E2E8F0; border-right: 1px solid #E2E8F0;">
                            <table cellpadding="0" cellspacing="0" border="0"><tr>
                                <!-- Logo icon -->
                                <td style="width: 32px; height: 32px; background: #6366F1; border-radius: 8px; text-align: center; vertical-align: middle;">
                                    <span style="color: #ffffff; font-size: 18px; font-weight: 700;">+</span>
                                </td>
                                <td style="padding-left: 10px;">
                                    <span style="font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif; font-size: 20px; font-weight: 700; color: #0F172A; letter-spacing: -0.02em;">Ecclesia<span style="color: #6366F1;">Flow</span></span>
                                </td>
                            </tr></table>
                        </td>
                    </tr>

                    <!-- CARD -->
                    <tr>
                        <td>
                            <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background: #ffffff; border-left: 1px solid #E2E8F0; border-right: 1px solid #E2E8F0;">
                                <#nested>
                            </table>
                        </td>
                    </tr>

                    <!-- FOOTER -->
                    <tr>
                        <td style="background: #ffffff; border-left: 1px solid #E2E8F0; border-right: 1px solid #E2E8F0; border-bottom: 1px solid #E2E8F0; border-radius: 0 0 16px 16px; padding: 0;">
                            <!-- Divider -->
                            <table width="100%" cellpadding="0" cellspacing="0" border="0"><tr>
                                <td style="padding: 0 24px;"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="height: 1px; background: #E2E8F0; font-size: 0; line-height: 0;">&nbsp;</td></tr></table></td>
                            </tr></table>
                            <!-- Footer content -->
                            <table width="100%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td align="center" style="padding: 20px 24px 8px;">
                                        <a href="#" style="font-family: 'Inter', -apple-system, sans-serif; font-size: 12px; color: #94A3B8; text-decoration: none; font-weight: 500;">${msg("footerHelp","Help Center")}</a>
                                        <span style="color: #E2E8F0; margin: 0 8px;">&#183;</span>
                                        <a href="#" style="font-family: 'Inter', -apple-system, sans-serif; font-size: 12px; color: #94A3B8; text-decoration: none; font-weight: 500;">${msg("footerPrivacy","Privacy")}</a>
                                        <span style="color: #E2E8F0; margin: 0 8px;">&#183;</span>
                                        <a href="#" style="font-family: 'Inter', -apple-system, sans-serif; font-size: 12px; color: #94A3B8; text-decoration: none; font-weight: 500;">${msg("footerTerms","Terms")}</a>
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="padding: 8px 24px 20px;">
                                        <span style="font-family: 'Inter', -apple-system, sans-serif; font-size: 11px; color: #CBD5E1;">&copy; ${.now?string('yyyy')} EcclesiaFlow. ${msg("allRightsReserved","Tous droits r&eacute;serv&eacute;s.")}</span>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                    <!-- Auto-message note -->
                    <tr>
                        <td align="center" style="padding: 16px 0 0;">
                            <span style="font-family: 'Inter', -apple-system, sans-serif; font-size: 11px; color: #CBD5E1;">${msg("emailAutoMessage","Ceci est un message automatique. Merci de ne pas r&eacute;pondre.")}</span>
                        </td>
                    </tr>

                </table>
            </td>
        </tr>
    </table>
</body>
</html>
</#macro>
