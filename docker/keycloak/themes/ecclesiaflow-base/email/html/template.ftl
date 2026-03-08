<#macro emailLayout>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${msg("emailTitle","EcclesiaFlow")}</title>
</head>
<body style="margin: 0; padding: 0; background-color: #f0f2f5; font-family: -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
    <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color: #f0f2f5; padding: 0; margin: 0;">
        <tr>
            <td align="center" style="padding: 0;">
                <table width="560" cellpadding="0" cellspacing="0" border="0" style="max-width: 560px; width: 100%; margin: 0 auto;">

                    <!-- BRANDED HEADER -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #0f1629 0%, #1a1f3d 50%, #0f1629 100%); padding: 0; border-radius: 0;">
                            <!-- Rainbow accent strip -->
                            <table width="100%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td style="height: 3px; background: linear-gradient(90deg, #3B52F6, #14B8A6, #F59E0B, #3B52F6); font-size: 0; line-height: 0;">&nbsp;</td>
                                </tr>
                            </table>
                            <table width="100%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td align="center" style="padding: 28px 20px 24px;">
                                        <span style="font-size: 24px; font-weight: 700; color: #ffffff; letter-spacing: -0.3px;">Ecclesia<span style="color: #6078FA;">Flow</span></span>
                                        <br/>
                                        <span style="font-size: 12px; color: #64748b; letter-spacing: 1.5px; text-transform: uppercase; font-weight: 500;">Church Management Platform</span>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                    <!-- CARD WRAPPER WITH PADDING -->
                    <tr>
                        <td style="padding: 28px 0 0;">
                            <!-- CARD -->
                            <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 24px rgba(0,0,0,0.06), 0 1px 3px rgba(0,0,0,0.04);">
                                <#nested>
                            </table>
                        </td>
                    </tr>

                    <!-- BRANDED FOOTER -->
                    <tr>
                        <td style="padding: 28px 0 0;">
                            <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background: linear-gradient(135deg, #0f1629 0%, #1a1f3d 100%); border-radius: 16px 16px 0 0;">
                                <tr>
                                    <td align="center" style="padding: 28px 32px 12px;">
                                        <span style="font-size: 16px; font-weight: 700; color: #e2e8f0; letter-spacing: -0.3px;">Ecclesia<span style="color: #6078FA;">Flow</span></span>
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="padding: 0 32px 16px;">
                                        <a href="#" style="font-size: 12px; color: #64748b; text-decoration: none; font-weight: 500; margin: 0 10px;">Help Center</a>
                                        <a href="#" style="font-size: 12px; color: #64748b; text-decoration: none; font-weight: 500; margin: 0 10px;">Privacy</a>
                                        <a href="#" style="font-size: 12px; color: #64748b; text-decoration: none; font-weight: 500; margin: 0 10px;">Terms</a>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 0 32px;"><table width="100%" cellpadding="0" cellspacing="0" border="0"><tr><td style="height: 1px; background: linear-gradient(90deg, transparent, #1e293b, transparent); font-size: 0; line-height: 0;">&nbsp;</td></tr></table></td>
                                </tr>
                                <tr>
                                    <td align="center" style="padding: 16px 32px 8px;">
                                        <span style="font-size: 11px; color: #475569;">&copy; ${.now?string('yyyy')} EcclesiaFlow. ${msg("allRightsReserved","All rights reserved.")}</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="padding: 0 32px 24px;">
                                        <span style="font-size: 11px; color: #475569;">${msg("emailAutoMessage","This is an automated message. Please do not reply.")}</span>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                </table>
            </td>
        </tr>
    </table>
</body>
</html>
</#macro>
